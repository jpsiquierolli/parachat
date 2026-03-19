package com.example.parachat.data.firebase.chat

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
    private val localDb: ParachatDatabase
) : MessageRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val messagesRef = database.getReference("messages")
    private val pinnedRef = database.getReference("pinnedMessages")
    private val conversationsRef = database.getReference("conversations")
    private val usersRef = database.getReference("users")
    private val messageDao = localDb.messageDao

    override suspend fun sendMessage(message: Message) {
        val conversationId = conversationId(message.senderId, message.receiverId)
        val newMessageRef = messagesRef.child(conversationId).push()
        val messageId = newMessageRef.key ?: return
        
        // Basic Encryption for Requirement 11
        val encryptedContent = if (message.type == MessageType.TEXT) {
            android.util.Base64.encodeToString(message.content.toByteArray(), android.util.Base64.DEFAULT)
        } else message.content
        
        val payload = message.copy(id = messageId, content = encryptedContent)
        newMessageRef.setValue(payload).await()
        updateConversationForSender(payload)
        updateConversationForReceiver(payload)
        
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

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { 
                    val msg = it.getValue(Message::class.java) ?: return@mapNotNull null
                    if (msg.type == MessageType.TEXT) {
                        try {
                            val decoded = String(android.util.Base64.decode(msg.content, android.util.Base64.DEFAULT))
                            msg.copy(content = decoded)
                        } catch (e: Exception) {
                            msg
                        }
                    } else msg
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
                            (it.senderId == currentUserId && it.receiverId == otherUserId) ||
                            (it.senderId == otherUserId && it.receiverId == currentUserId)
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

    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message) {
        val conversationId = conversationId(currentUserId, otherUserId)
        pinnedRef.child(conversationId).setValue(message).await()
    }

    override suspend fun unpinMessage(currentUserId: String, otherUserId: String) {
        val conversationId = conversationId(currentUserId, otherUserId)
        pinnedRef.child(conversationId).removeValue().await()
    }

    override fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Message::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        pinnedRef.child(conversationId).addValueEventListener(listener)
        awaitClose { pinnedRef.child(conversationId).removeEventListener(listener) }
    }

    override suspend fun markConversationAsRead(currentUserId: String, otherUserId: String) {
        val conversationId = conversationId(currentUserId, otherUserId)
        conversationsRef.child(currentUserId).child(otherUserId).child("unreadCount").setValue(0).await()
        val snapshot = messagesRef.child(conversationId).get().await()
        snapshot.children.mapNotNull { it.getValue(Message::class.java) }
            .filter { it.receiverId == currentUserId && it.status != MessageStatus.READ }
            .forEach { message ->
                messagesRef.child(conversationId).child(message.id).child("status").setValue(MessageStatus.READ).await()
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
        MessageType.TEXT -> message.content
        MessageType.IMAGE -> "[Image]"
        MessageType.VIDEO -> "[Video]"
        MessageType.AUDIO -> "[Audio]"
        MessageType.LOCATION -> "[Location]"
        MessageType.FILE -> message.content.ifBlank { "[File]" }
    }

    private fun conversationId(userA: String, userB: String): String = listOf(userA, userB).sorted().joinToString(separator = "_")
}
