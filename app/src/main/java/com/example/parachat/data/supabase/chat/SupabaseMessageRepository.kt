package com.example.parachat.data.supabase.chat

import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SupabaseMessageRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDb: ParachatDatabase
) : MessageRepository {

    private val messageDao = localDb.messageDao

    override suspend fun sendMessage(message: Message) {
        val conversationId = conversationId(message.senderId, message.receiverId)
        // Generate an ID if empty, as Supabase insert won't auto-gen if we send an empty string.
        val messageId = if (message.id.isBlank()) java.util.UUID.randomUUID().toString() else message.id
        val messageWithConvId = message.copy(id = messageId, conversationId = conversationId)

        try {
            supabase.postgrest["messages"].upsert(messageWithConvId)

            // Update conversation for sender
            updateConversation(message.senderId, message.receiverId, messageWithConvId)
            // Update conversation for receiver (increment unread logic complex here, simplify for now)
            updateConversation(message.receiverId, message.senderId, messageWithConvId)

        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error sending message", e)
        }
        
        // Cache to Room
        try {
            messageDao.insert(com.example.parachat.data.room.chat.MessageEntity(
                id = messageWithConvId.id,
                senderId = messageWithConvId.senderId,
                receiverId = messageWithConvId.receiverId,
                content = messageWithConvId.content,
                timestamp = messageWithConvId.timestamp,
                type = messageWithConvId.type.name,
                status = messageWithConvId.status.name
            ))
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error saving message locally", e)
        }
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = flow {
         try {
             // Emit empty list first
             emit(emptyList())

             val conversationId = conversationId(currentUserId, otherUserId)
             // Use conversation_id filter now
             val messages = supabase.postgrest["messages"].select {
                 filter {
                     eq("conversation_id", conversationId)
                 }
                 order("timestamp", Order.ASCENDING)
             }.decodeList<Message>()
             emit(messages)
         } catch (e: Exception) {
             android.util.Log.e("SupabaseMessageRepo", "Error fetching messages", e)
             // Don't emit error here to avoid crash, just log. UI has empty list from above.
         }
    }

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = flow {
         try {
             // We query conversations table.
             // We assume 'participants' column contains currentUserId?
             // Or 'owner_id' column exists? 
             // Firebase schema was nested under userId.
             // Supabase schema likely has 'participants' array or join table.
             // Given Conversation data class has 'participants: List<String>', we can use check "participants @> {currentUserId}".
             val conversations = supabase.postgrest["conversations"].select {
                 filter {
                     contains("participants", listOf(currentUserId))
                 }
                 order("last_message_timestamp", Order.DESCENDING)
             }.decodeList<Conversation>()
             emit(conversations)
         } catch (e: Exception) {
             android.util.Log.e("SupabaseMessageRepo", "Error fetching conversations", e)
             emit(emptyList())
         }
    }

    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message) {}
    override suspend fun unpinMessage(currentUserId: String, otherUserId: String) {}
    override fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?> = flow { emit(null) }
    override suspend fun markConversationAsRead(currentUserId: String, otherUserId: String) {}

    private fun conversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }

    private suspend fun updateConversation(ownerId: String, otherId: String, message: Message) {
        try {
             val conversationId = conversationId(ownerId, otherId)
             val conversation = Conversation(
                 id = conversationId,
                 otherUserId = otherId,
                 title = otherId, // Ideally fetch username
                 lastMessagePreview = message.content.take(50),
                 lastMessageTimestamp = message.timestamp,
                 unreadCount = 0, // Reset for sender. Logic for receiver needs read.
                 isGroup = false,
                 participants = listOf(ownerId, otherId)
             )
             // Upsert conversation. Table needs primary key (id).
             supabase.postgrest["conversations"].upsert(conversation)
        } catch (e: Exception) {
             android.util.Log.e("SupabaseMessageRepo", "Error updating conversation", e)
        }
    }
}
