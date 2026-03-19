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
    private fun Message.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "groupId" to groupId,
        "content" to content,
        "mediaUrl" to mediaUrl,
        "mediaThumbnailUrl" to mediaThumbnailUrl,
        "mediaDurationMillis" to mediaDurationMillis,
        "latitude" to latitude,
        "longitude" to longitude,
        "type" to type.name,
        "status" to status.name,
        "timestamp" to timestamp
    )
    private fun DataSnapshot.toMessage(): Message? {
        return try {
            Message(
                id = child("id").getValue(String::class.java) ?: return null,
                senderId = child("senderId").getValue(String::class.java) ?: return null,
                receiverId = child("receiverId").getValue(String::class.java) ?: "",
                groupId = child("groupId").getValue(String::class.java),
                content = child("content").getValue(String::class.java) ?: "",
                mediaUrl = child("mediaUrl").getValue(String::class.java),
                mediaThumbnailUrl = child("mediaThumbnailUrl").getValue(String::class.java),
                mediaDurationMillis = child("mediaDurationMillis").getValue(Long::class.java),
                latitude = child("latitude").getValue(Double::class.java),
                longitude = child("longitude").getValue(Double::class.java),
                type = MessageType.valueOf(child("type").getValue(String::class.java) ?: MessageType.TEXT.name),
                status = MessageStatus.valueOf(child("status").getValue(String::class.java) ?: MessageStatus.SENT.name),
                timestamp = child("timestamp").getValue(Long::class.java) ?: 0L
            )
        } catch (e: Exception) { null }
    }
    private fun Conversation.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "otherUserId" to otherUserId,
        "title" to title,
        "lastMessagePreview" to lastMessagePreview,
        "lastMessageTimestamp" to lastMessageTimestamp,
        "unreadCount" to unreadCount,
        "isGroup" to isGroup,
        "participants" to participants,
        "pinnedMessageId" to pinnedMessageId
    )
    @Suppress("UNCHECKED_CAST")
    private fun DataSnapshot.toConversation(): Conversation? {
        return try {
            Conversation(
                id = child("id").getValue(String::class.java) ?: "",
                otherUserId = child("otherUserId").getValue(String::class.java) ?: return null,
                title = child("title").getValue(String::class.java) ?: "",
                lastMessagePreview = child("lastMessagePreview").getValue(String::class.java) ?: "",
                lastMessageTimestamp = child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L,
                unreadCount = child("unreadCount").getValue(Long::class.java)?.toInt() ?: 0,
                isGroup = child("isGroup").getValue(Boolean::class.java) ?: false,
                participants = (child("participants").value as? List<String>) ?: emptyList(),
                pinnedMessageId = child("pinnedMessageId").getValue(String::class.java)
            )
        } catch (e: Exception) { null }
    }
    override suspend fun sendMessage(message: Message) {
        val encryptedContent = if (message.type == MessageType.TEXT) CryptoUtils.encrypt(message.content) else message.content
        val secureMessage = message.copy(content = encryptedContent)
        val groupId = secureMessage.groupId
        if (groupId != null) {
            val ref = groupMessagesRef.child(groupId).push()
            val id = ref.key ?: return
            val payload = secureMessage.copy(id = id)
            ref.setValue(payload.toMap()).await()
            messageDao.insert(payload.toEntity())
        } else {
            val convId = conversationId(secureMessage.senderId, secureMessage.receiverId)
            val ref = messagesRef.child(convId).push()
            val id = ref.key ?: return
            val payload = secureMessage.copy(id = id)
            ref.setValue(payload.toMap()).await()
            messageDao.insert(payload.toEntity())
            updateConversationForSender(secureMessage.senderId, secureMessage.receiverId, payload)
            updateConversationForReceiver(secureMessage.senderId, secureMessage.receiverId, payload)
        }
    }
    private fun decryptMessage(message: Message): Message =
        if (message.type == MessageType.TEXT) message.copy(content = CryptoUtils.decrypt(message.content)) else message
    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = callbackFlow {
        val convId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.toMessage() }
                repositoryScope.launch { messageDao.insertAll(messages.map { it.toEntity() }) }
                trySend(messages.map { decryptMessage(it) }.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.child(convId).addValueEventListener(listener)
        awaitClose { messagesRef.child(convId).removeEventListener(listener) }
    }
    override fun getGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.toMessage() }
                repositoryScope.launch { messageDao.insertAll(messages.map { it.toEntity() }) }
                trySend(messages.map { decryptMessage(it) }.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        groupMessagesRef.child(groupId).addValueEventListener(listener)
        awaitClose { groupMessagesRef.child(groupId).removeEventListener(listener) }
    }
    fun getLocalMessages(userId: String, otherUserId: String): Flow<List<Message>> =
        messageDao.getMessagesForChat(userId, otherUserId).map { it.map { e -> decryptMessage(e.toDomain()) } }
    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull { it.toConversation() }
                    .map { it.copy(lastMessagePreview = CryptoUtils.decrypt(it.lastMessagePreview)) }
                    .sortedByDescending { it.lastMessageTimestamp }
                trySend(conversations)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        conversationsRef.child(currentUserId).addValueEventListener(listener)
        awaitClose { conversationsRef.child(currentUserId).removeEventListener(listener) }
    }
    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message) {
        val convId = conversationId(currentUserId, otherUserId)
        val toPin = if (message.type == MessageType.TEXT) message.copy(content = CryptoUtils.encrypt(message.content)) else message
        pinnedRef.child(convId).setValue(toPin.toMap()).await()
    }
    override suspend fun unpinMessage(currentUserId: String, otherUserId: String) {
        pinnedRef.child(conversationId(currentUserId, otherUserId)).removeValue().await()
    }
    override fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?> = callbackFlow {
        val convId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.toMessage()?.let { decryptMessage(it) }) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        pinnedRef.child(convId).addValueEventListener(listener)
        awaitClose { pinnedRef.child(convId).removeEventListener(listener) }
    }
    override suspend fun pinGroupMessage(groupId: String, message: Message) {
        val toPin = if (message.type == MessageType.TEXT) message.copy(content = CryptoUtils.encrypt(message.content)) else message
        pinnedGroupsRef.child(groupId).setValue(toPin.toMap()).await()
    }
    override suspend fun unpinGroupMessage(groupId: String) {
        pinnedGroupsRef.child(groupId).removeValue().await()
    }
    override fun observePinnedGroupMessage(groupId: String): Flow<Message?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.toMessage()?.let { decryptMessage(it) }) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        pinnedGroupsRef.child(groupId).addValueEventListener(listener)
        awaitClose { pinnedGroupsRef.child(groupId).removeEventListener(listener) }
    }
    override suspend fun markConversationAsRead(currentUserId: String, otherUserId: String) {
        val convId = conversationId(currentUserId, otherUserId)
        conversationsRef.child(currentUserId).child(otherUserId).child("unreadCount").setValue(0).await()
        messagesRef.child(convId).get().await().children.mapNotNull { it.toMessage() }
            .filter { it.receiverId == currentUserId && it.status != MessageStatus.READ }
            .forEach { msg ->
                messagesRef.child(convId).child(msg.id).child("status").setValue(MessageStatus.READ.name).await()
            }
    }
    private suspend fun updateConversationForSender(senderId: String, receiverId: String, message: Message) {
        conversationsRef.child(senderId).child(receiverId).setValue(buildConversation(message, receiverId, 0).toMap()).await()
    }
    private suspend fun updateConversationForReceiver(senderId: String, receiverId: String, message: Message) {
        val ref = conversationsRef.child(receiverId).child(senderId)
        val currentUnread = ref.get().await().child("unreadCount").getValue(Long::class.java)?.toInt() ?: 0
        ref.setValue(buildConversation(message, senderId, currentUnread + 1).toMap()).await()
    }
    private fun buildConversation(message: Message, otherUserId: String, unread: Int) = Conversation(
        id = conversationId(message.senderId, message.receiverId),
        otherUserId = otherUserId,
        title = otherUserId,
        lastMessagePreview = if (message.type == MessageType.TEXT) message.content else previewFor(message),
        lastMessageTimestamp = message.timestamp,
        unreadCount = unread,
        participants = listOf(message.senderId, message.receiverId)
    )
    private fun previewFor(message: Message): String = when (message.type) {
        MessageType.TEXT -> message.content
        MessageType.IMAGE -> "[Imagem]"
        MessageType.VIDEO -> "[Video]"
        MessageType.AUDIO -> "[Audio]"
        MessageType.LOCATION -> "[Localizacao]"
        MessageType.FILE -> message.content.ifBlank { "[Arquivo]" }
    }
    private fun conversationId(userA: String, userB: String): String =
        listOf(userA, userB).sorted().joinToString(separator = "_")
}
