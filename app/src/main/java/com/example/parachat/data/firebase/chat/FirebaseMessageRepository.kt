package com.example.parachat.data.firebase.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.parachat.MainActivity
import com.example.parachat.security.MessageEncryption
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.tasks.await
import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.domain.chat.sortedForChat
import com.example.parachat.domain.displayNameFromParts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseMessageRepository(
    private val database: FirebaseDatabase,
    private val localDb: ParachatDatabase,
    private val context: Context
) : MessageRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val messagesRef = database.getReference("messages")
    private val pinnedRef = database.getReference("pinnedMessages")
    private val conversationsRef = database.getReference("conversations")
    private val usersRef = database.getReference("users")
    private val groupsRef = database.getReference("groups")
    private val messageDao = localDb.messageDao
    private val unreadBaselineByConversation = mutableMapOf<String, Int>()

    override suspend fun sendMessage(message: Message, isGroup: Boolean) {
        val conversationId = conversationId(message.senderId, message.receiverId, isGroup)
        val newMessageRef = messagesRef.child(conversationId).push()
        val messageId = newMessageRef.key ?: return
        
        val payload = message.copy(
            id = messageId,
            content = normalizeTextPayload(message.content, message.type),
            conversationId = conversationId
        )
        newMessageRef.setValue(payload).await()
        if (isGroup) {
            updateConversationForGroupMembers(payload)
        } else {
            updateConversationForSender(payload)
            updateConversationForReceiver(payload)
        }
        
        // Cache to Room (unencrypted for local view)
        messageDao.insert(com.example.parachat.data.room.chat.MessageEntity(
            id = messageId,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = message.content, 
            mediaUrl = message.mediaUrl,
            mediaThumbnailUrl = message.mediaThumbnailUrl,
            mediaDurationMillis = message.mediaDurationMillis,
            latitude = message.latitude,
            longitude = message.longitude,
            conversationId = conversationId,
            timestamp = message.timestamp,
            type = message.type.name,
            status = message.status.name
        ))
    }

    override fun getMessages(currentUserId: String, otherUserId: String, isGroup: Boolean): Flow<List<Message>> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId, isGroup)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { 
                    val msg = it.getValue(Message::class.java) ?: return@mapNotNull null
                    msg.copy(content = normalizeTextPayload(msg.content, msg.type))
                }.sortedForChat()
                
                // Cache to Room
                repositoryScope.launch {
                    messages.forEach { msg ->
                        messageDao.insert(com.example.parachat.data.room.chat.MessageEntity(
                            id = msg.id,
                            senderId = msg.senderId,
                            receiverId = msg.receiverId,
                            content = msg.content,
                            mediaUrl = msg.mediaUrl,
                            mediaThumbnailUrl = msg.mediaThumbnailUrl,
                            mediaDurationMillis = msg.mediaDurationMillis,
                            latitude = msg.latitude,
                            longitude = msg.longitude,
                            conversationId = msg.conversationId.ifBlank { conversationId },
                            timestamp = msg.timestamp,
                            type = msg.type.name,
                            status = msg.status.name
                        ))
                    }
                }
                
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                // On error/offline, load from Room
                repositoryScope.launch {
                    messageDao.getAllMessages().collect { entities ->
                        val messages = entities.filter { 
                            if (isGroup) {
                                it.conversationId == conversationId
                            } else {
                                (it.senderId == currentUserId && it.receiverId == otherUserId) ||
                                    (it.senderId == otherUserId && it.receiverId == currentUserId)
                            }
                        }.map { 
                            Message(
                                id = it.id,
                                senderId = it.senderId,
                                receiverId = it.receiverId,
                                content = it.content,
                                mediaUrl = it.mediaUrl,
                                mediaThumbnailUrl = it.mediaThumbnailUrl,
                                mediaDurationMillis = it.mediaDurationMillis,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                timestamp = it.timestamp,
                                type = MessageType.valueOf(it.type),
                                status = MessageStatus.valueOf(it.status),
                                conversationId = it.conversationId
                            )
                        }.sortedForChat()
                        trySend(messages)
                    }
                }
            }
        }
        messagesRef.child(conversationId).addValueEventListener(listener)
        awaitClose { messagesRef.child(conversationId).removeEventListener(listener) }
    }

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull { it.getValue(Conversation::class.java) }
                    .sortedByDescending { it.lastMessageTimestamp }
                maybeNotifyUnread(conversations)
                trySend(conversations)
            }

//            override fun onCancelled(error: DatabaseError) {
//                close(error.toException())
//            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e(
                    "FirebaseMessageRepo",
                    "observeConversations cancelled: ${error.code} ${error.message}",
                    error.toException()
                )
                trySend(emptyList())
            }
        }
        conversationsRef.child(currentUserId).addValueEventListener(listener)
        awaitClose { conversationsRef.child(currentUserId).removeEventListener(listener) }
    }

    override suspend fun deleteConversation(currentUserId: String, chatId: String, isGroup: Boolean) {
        if (currentUserId.isBlank() || chatId.isBlank()) return

        conversationsRef.child(currentUserId).child(chatId).removeValue().await()

        // Also clear local unread baseline to avoid stale notification deltas.
        unreadBaselineByConversation.keys
            .filter { it.contains(chatId) }
            .toList()
            .forEach { unreadBaselineByConversation.remove(it) }
    }

    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message, isGroup: Boolean) {
        val conversationId = conversationId(currentUserId, otherUserId, isGroup)
        pinnedRef.child(conversationId).setValue(message).await()
    }

    override suspend fun unpinMessage(currentUserId: String, otherUserId: String, isGroup: Boolean) {
        val conversationId = conversationId(currentUserId, otherUserId, isGroup)
        pinnedRef.child(conversationId).removeValue().await()
    }

    override fun observePinnedMessage(currentUserId: String, otherUserId: String, isGroup: Boolean): Flow<Message?> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId, isGroup)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val message = snapshot.getValue(Message::class.java)?.copy(
                    content = normalizeTextPayload(
                        snapshot.child("content").getValue(String::class.java).orEmpty(),
                        snapshot.child("type").getValue(MessageType::class.java) ?: MessageType.TEXT
                    )
                )
                trySend(message)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w(
                    "FirebaseMessageRepo",
                    "observePinnedMessage cancelled for $conversationId: ${error.code} ${error.message}"
                )
                trySend(null)
            }
        }
        pinnedRef.child(conversationId).addValueEventListener(listener)
        awaitClose { pinnedRef.child(conversationId).removeEventListener(listener) }
    }

    override suspend fun markConversationAsRead(currentUserId: String, otherUserId: String, isGroup: Boolean) {
        val conversationId = conversationId(currentUserId, otherUserId, isGroup)
        conversationsRef.child(currentUserId).child(otherUserId).child("unreadCount").setValue(0).await()
        if (isGroup) return

        val snapshot = messagesRef.child(conversationId).get().await()
        snapshot.children.mapNotNull { it.getValue(Message::class.java) }
            .filter { it.receiverId == currentUserId && it.status != MessageStatus.READ }
            .forEach { message ->
                messagesRef.child(conversationId).child(message.id).child("status").setValue(MessageStatus.READ).await()
            }
    }

    private suspend fun updateConversationForGroupMembers(message: Message) {
        val groupId = message.receiverId
        val groupSnapshot = groupsRef.child(groupId).get().await()
        val groupName = groupSnapshot.child("name").getValue(String::class.java).orEmpty().ifBlank { "Group" }
        val creatorId = groupSnapshot.child("creatorId").getValue(String::class.java).orEmpty()
        val members = groupSnapshot.child("members").children
            .mapNotNull { it.getValue(String::class.java) }
            .toMutableSet()

        if (creatorId.isNotBlank()) members.add(creatorId)

        members.forEach { memberId ->
            val ref = conversationsRef.child(memberId).child(groupId)
            val snapshot = ref.get().await()
            val currentUnread = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
            val unreadCount = if (memberId == message.senderId) 0 else currentUnread + 1

            val conversation = Conversation(
                id = conversationId(message.senderId, groupId, true),
                otherUserId = groupId,
                title = groupName,
                lastMessagePreview = previewFor(message),
                lastMessageTimestamp = message.timestamp,
                unreadCount = unreadCount,
                isGroup = true,
                participants = members.toList()
            )
            ref.setValue(conversation).await()
        }
    }

    private suspend fun updateConversationForSender(message: Message) {
        val title = resolveDisplayName(message.receiverId)
        val conversation = buildConversation(message, otherUserId = message.receiverId, title = title, unread = 0)
        conversationsRef.child(message.senderId).child(message.receiverId).setValue(conversation).await()
    }

    private suspend fun updateConversationForReceiver(message: Message) {
        val ref = conversationsRef.child(message.receiverId).child(message.senderId)
        val snapshot = ref.get().await()
        val currentUnread = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
        val title = resolveDisplayName(message.senderId)
        val conversation = buildConversation(message, otherUserId = message.senderId, title = title, unread = currentUnread + 1)
        ref.setValue(conversation).await()
    }

    private suspend fun resolveDisplayName(userId: String): String {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            val email = snapshot.child("email").getValue(String::class.java).orEmpty()
            val username = snapshot.child("username").getValue(String::class.java)
            displayNameFromParts(username = username, email = email, id = userId)
        } catch (_: Exception) {
            userId
        }
    }

    private fun buildConversation(message: Message, otherUserId: String, title: String, unread: Int): Conversation {
        return Conversation(
            id = conversationId(message.senderId, message.receiverId),
            otherUserId = otherUserId,
            title = title,
            lastMessagePreview = previewFor(message),
            lastMessageTimestamp = message.timestamp,
            unreadCount = unread,
            participants = listOf(message.senderId, message.receiverId)
        )
    }

    private fun previewFor(message: Message): String = when (message.type) {
        MessageType.TEXT -> decryptForPreview(message)
        MessageType.IMAGE -> "[Image]"
        MessageType.VIDEO -> "[Video]"
        MessageType.AUDIO -> "[Audio]"
        MessageType.LOCATION -> "[Location]"
        MessageType.FILE -> message.content.ifBlank { "[File]" }
    }

    private fun decryptForPreview(message: Message): String {
        val normalized = normalizeTextPayload(message.content, message.type)
        if (message.type != MessageType.TEXT || normalized.isBlank()) return normalized

        return try {
            val key = MessageEncryption.deriveConversationKey(message.senderId, message.receiverId)
            MessageEncryption.decrypt(normalized, key)
        } catch (_: Exception) {
            normalized
        }
    }

    private fun normalizeTextPayload(content: String, type: MessageType): String {
        if (type != MessageType.TEXT) return content
        val trimmed = content.trim()
        if (!looksLikeBase64(trimmed)) return content

        return try {
            val decoded = String(android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT), Charsets.UTF_8)
            if (decoded.isNotBlank() && decoded.isMostlyReadable()) decoded else content
        } catch (_: Exception) {
            content
        }
    }

    private fun looksLikeBase64(value: String): Boolean {
        if (value.isBlank()) return false
        if (value.length % 4 != 0) return false
        return value.matches(Regex("^[A-Za-z0-9+/=\\n\\r]+$"))
    }

    private fun String.isMostlyReadable(): Boolean {
        if (isBlank()) return false
        val printable = count { it == '\n' || it == '\r' || it == '\t' || !it.isISOControl() }
        return printable.toDouble() / length.toDouble() >= 0.85
    }

    private fun maybeNotifyUnread(conversations: List<Conversation>) {
        conversations.forEach { conversation ->
            val previous = unreadBaselineByConversation[conversation.id]
            if (previous == null) {
                unreadBaselineByConversation[conversation.id] = conversation.unreadCount
                return@forEach
            }

            if (conversation.unreadCount > previous) {
                showUnreadNotification(conversation)
            }

            unreadBaselineByConversation[conversation.id] = conversation.unreadCount
        }
    }

    private fun showUnreadNotification(conversation: Conversation) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "parachat_messages"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Parachat mensagens",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversation.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (conversation.isGroup) "Nova mensagem no grupo" else "Nova mensagem"
        val body = conversation.lastMessagePreview.ifBlank { "Voce recebeu uma nova mensagem" }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(conversation.id.hashCode(), notification)
    }

    private fun conversationId(userA: String, userB: String, isGroup: Boolean = false): String {
        return if (isGroup) {
            "group_$userB"
        } else {
            listOf(userA, userB).sorted().joinToString(separator = "_")
        }
    }
}
