package com.example.parachat.data.firebase.chat

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.util.CryptoUtils
import com.example.parachat.data.room.chat.MessageDao
import com.example.parachat.data.room.chat.toEntity
import com.example.parachat.data.room.chat.toDomain

class FirebaseMessageRepository(
    private val database: FirebaseDatabase,
    private val messageDao: MessageDao
) : MessageRepository {

    private val messagesRef = database.getReference("messages")
    private val groupMessagesRef = database.getReference("groupMessages")
    private val pinnedRef = database.getReference("pinnedMessages")
    private val pinnedGroupsRef = database.getReference("pinnedGroupMessages")
    private val conversationsRef = database.getReference("conversations")
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun sendMessage(message: Message) {
        val encryptedContent = if (message.type == MessageType.TEXT) {
            CryptoUtils.encrypt(message.content)
        } else {
            message.content
        }
        val secureMessage = message.copy(content = encryptedContent)

        val groupId = secureMessage.groupId
        if (groupId != null) {
            val newMessageRef = groupMessagesRef.child(groupId).push()
            val messageId = newMessageRef.key ?: return
            val payload = secureMessage.copy(id = messageId)
            newMessageRef.setValue(payload).await()
            messageDao.insert(payload.toEntity())
        } else {
            val conversationId = conversationId(secureMessage.senderId, secureMessage.receiverId)
            val newMessageRef = messagesRef.child(conversationId).push()
            val messageId = newMessageRef.key ?: return
            val payload = secureMessage.copy(id = messageId)
            newMessageRef.setValue(payload).await()
            messageDao.insert(payload.toEntity())
            updateConversationForSender(secureMessage.senderId, secureMessage.receiverId, payload)
            updateConversationForReceiver(secureMessage.senderId, secureMessage.receiverId, payload)
        }
    }

    private fun decryptMessage(message: Message): Message {
        return if (message.type == MessageType.TEXT) {
            message.copy(content = CryptoUtils.decrypt(message.content))
        } else {
            message
        }
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                repositoryScope.launch {
                    messageDao.insertAll(messages.map { it.toEntity() })
                }
                trySend(messages.map { decryptMessage(it) }.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        messagesRef.child(conversationId).addValueEventListener(listener)
        awaitClose { messagesRef.child(conversationId).removeEventListener(listener) }
    }

    override fun getGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                repositoryScope.launch {
                    messageDao.insertAll(messages.map { it.toEntity() })
                }
                trySend(messages.map { decryptMessage(it) }.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        groupMessagesRef.child(groupId).addValueEventListener(listener)
        awaitClose { groupMessagesRef.child(groupId).removeEventListener(listener) }
    }

    fun getLocalMessages(userId: String, otherUserId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(userId, otherUserId).map { entities ->
            entities.map { decryptMessage(it.toDomain()) }
        }
    }

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull { it.getValue(Conversation::class.java) }
                    .map { it.copy(lastMessagePreview = CryptoUtils.decrypt(it.lastMessagePreview)) }
                    .sortedByDescending { it.lastMessageTimestamp }
                trySend(conversations)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        conversationsRef.child(currentUserId).addValueEventListener(listener)
        awaitClose { conversationsRef.child(currentUserId).removeEventListener(listener) }
    }

    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message) {
        val conversationId = conversationId(currentUserId, otherUserId)
        val toPin = if (message.type == MessageType.TEXT) message.copy(content = CryptoUtils.encrypt(message.content)) else message
        pinnedRef.child(conversationId).setValue(toPin).await()
    }

    override suspend fun unpinMessage(currentUserId: String, otherUserId: String) {
        val conversationId = conversationId(currentUserId, otherUserId)
        pinnedRef.child(conversationId).removeValue().await()
    }

    override fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(Message::class.java)
                trySend(msg?.let { decryptMessage(it) })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        pinnedRef.child(conversationId).addValueEventListener(listener)
        awaitClose { pinnedRef.child(conversationId).removeEventListener(listener) }
    }

    override suspend fun pinGroupMessage(groupId: String, message: Message) {
        val toPin = if (message.type == MessageType.TEXT) message.copy(content = CryptoUtils.encrypt(message.content)) else message
        pinnedGroupsRef.child(groupId).setValue(toPin).await()
    }

    override suspend fun unpinGroupMessage(groupId: String) {
        pinnedGroupsRef.child(groupId).removeValue().await()
    }

    override fun observePinnedGroupMessage(groupId: String): Flow<Message?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(Message::class.java)
                trySend(msg?.let { decryptMessage(it) })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        pinnedGroupsRef.child(groupId).addValueEventListener(listener)
        awaitClose { pinnedGroupsRef.child(groupId).removeEventListener(listener) }
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

    private suspend fun updateConversationForSender(senderId: String, receiverId: String, message: Message) {
        val conversation = buildConversation(message, otherUserId = receiverId, unread = 0)
        conversationsRef.child(senderId).child(receiverId).setValue(conversation).await()
    }

    private suspend fun updateConversationForReceiver(senderId: String, receiverId: String, message: Message) {
        val ref = conversationsRef.child(receiverId).child(senderId)
        val snapshot = ref.get().await()
        val currentUnread = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
        val conversation = buildConversation(message, otherUserId = senderId, unread = currentUnread + 1)
        ref.setValue(conversation).await()
    }

    private fun buildConversation(message: Message, otherUserId: String, unread: Int): Conversation {
        return Conversation(
            id = conversationId(message.senderId, message.receiverId),
            otherUserId = otherUserId,
            title = otherUserId,
            lastMessagePreview = if (message.type == MessageType.TEXT) message.content else previewFor(message),
            lastMessageTimestamp = message.timestamp,
            unreadCount = unread,
            participants = listOf(message.senderId, message.receiverId)
        )
    }

    private fun previewFor(message: Message): String = when (message.type) {
        MessageType.TEXT -> message.content
        MessageType.IMAGE -> "[Imagem]"
        MessageType.VIDEO -> "[Vídeo]"
        MessageType.AUDIO -> "[Áudio]"
        MessageType.LOCATION -> "[Localização]"
        MessageType.FILE -> message.content.ifBlank { "[Arquivo]" }
    }

    private fun conversationId(userA: String, userB: String): String = listOf(userA, userB).sorted().joinToString(separator = "_")
}
