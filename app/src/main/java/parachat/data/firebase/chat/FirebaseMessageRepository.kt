package parachat.data.firebase.chat

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import parachat.domain.chat.Conversation
import parachat.domain.chat.Message
import parachat.domain.chat.MessageRepository
import parachat.domain.chat.MessageStatus
import parachat.domain.chat.MessageType

class FirebaseMessageRepository(
    private val database: FirebaseDatabase
) : MessageRepository {

    private val messagesRef = database.getReference("messages")
    private val pinnedRef = database.getReference("pinnedMessages")
    private val conversationsRef = database.getReference("conversations")

    override suspend fun sendMessage(message: Message) {
        val conversationId = conversationId(message.senderId, message.receiverId)
        val newMessageRef = messagesRef.child(conversationId).push()
        val messageId = newMessageRef.key ?: return
        val payload = message.copy(id = messageId)
        newMessageRef.setValue(payload).await()
        updateConversationForSender(payload)
        updateConversationForReceiver(payload)
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = callbackFlow {
        val conversationId = conversationId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
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

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
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
        val conversation = buildConversation(message, otherUserId = message.receiverId, unread = 0)
        conversationsRef.child(message.senderId).child(message.receiverId).setValue(conversation).await()
    }

    private suspend fun updateConversationForReceiver(message: Message) {
        val ref = conversationsRef.child(message.receiverId).child(message.senderId)
        val snapshot = ref.get().await()
        val currentUnread = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
        val conversation = buildConversation(message, otherUserId = message.senderId, unread = currentUnread + 1)
        ref.setValue(conversation).await()
    }

    private fun buildConversation(message: Message, otherUserId: String, unread: Int): Conversation {
        return Conversation(
            id = conversationId(message.senderId, message.receiverId),
            otherUserId = otherUserId,
            title = otherUserId,
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
