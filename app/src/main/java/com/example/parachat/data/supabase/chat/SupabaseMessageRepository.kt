package com.example.parachat.data.supabase.chat

import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.data.supabase.SupabaseSchemaGuard
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.domain.chat.sortedForChat
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class SupabaseMessageRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDb: ParachatDatabase
) : MessageRepository {

    private val messageDao = localDb.messageDao
    private val messagesTable = "messages"
    private val conversationsTable = "conversations"

    private fun logTableMissingOnce(table: String) {
        android.util.Log.e(
            "SupabaseMessageRepo",
            "Supabase table '$table' is missing. Run the SQL migration scripts in parachat/supabase/migrations."
        )
    }

    override suspend fun sendMessage(message: Message) {
        val conversationId = conversationId(message.senderId, message.receiverId)
        // Generate an ID if empty, as Supabase insert won't auto-gen if we send an empty string.
        val messageId = if (message.id.isBlank()) java.util.UUID.randomUUID().toString() else message.id
        val messageWithConvId = message.copy(
            id = messageId,
            conversationId = conversationId,
            status = MessageStatus.SENT
        )

        try {
            if (SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
                supabase.postgrest[messagesTable].upsert(messageWithConvId)
            }

            // Update conversation for sender
            updateConversation(ownerId = message.senderId, otherId = message.receiverId, message = messageWithConvId, increaseUnread = false)
            // Update conversation for receiver
            updateConversation(ownerId = message.receiverId, otherId = message.senderId, message = messageWithConvId, increaseUnread = true)

        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error sending message", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)) {
                logTableMissingOnce(messagesTable)
            }
        }
        
        // Cache to Room
        try {
            messageDao.insert(messageWithConvId.toEntity())
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error saving message locally", e)
        }
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = channelFlow {
        if (SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            launch {
                // Prime cache immediately, then keep polling while chat is open.
                syncMessagesFromRemote(currentUserId, otherUserId)
                while (isActive) {
                    delay(MESSAGE_SYNC_INTERVAL_MS)
                    syncMessagesFromRemote(currentUserId, otherUserId)
                }
            }
        }

        messageDao.getMessagesForChat(currentUserId, otherUserId)
            .map { entities -> entities.map { it.toDomain() }.sortedForChat() }
            .collect { send(it) }
    }

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = flow {
         if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) {
             emit(emptyList())
             return@flow
         }

         try {
             val conversations = supabase.postgrest[conversationsTable].select {
                 filter {
                     eq("owner_id", currentUserId)
                 }
                 order("last_message_timestamp", Order.DESCENDING)
             }.decodeList<Conversation>()
             emit(conversations)
         } catch (e: Exception) {
             android.util.Log.e("SupabaseMessageRepo", "Error fetching conversations", e)
             if (SupabaseSchemaGuard.markMissingTableIfNeeded(conversationsTable, e)) {
                 logTableMissingOnce(conversationsTable)
             }
             emit(emptyList())
         }
    }

    override suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, otherUserId)
        supabase.postgrest[conversationsTable].update({
            set("pinned_message_id", message.id)
        }) {
            filter { eq("id", rowId) }
        }
    }

    override suspend fun unpinMessage(currentUserId: String, otherUserId: String) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, otherUserId)
        supabase.postgrest[conversationsTable].update({
            set<String?>("pinned_message_id", null)
        }) {
            filter { eq("id", rowId) }
        }
    }

    override fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?> = flow {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable) || !SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            emit(null)
            return@flow
        }

        try {
            val rowId = conversationRowId(currentUserId, otherUserId)
            val conversation = supabase.postgrest[conversationsTable].select {
                filter { eq("id", rowId) }
            }.decodeSingleOrNull<Conversation>()

            val pinnedId = conversation?.pinnedMessageId
            if (pinnedId.isNullOrBlank()) {
                emit(null)
                return@flow
            }

            val pinnedMessage = supabase.postgrest[messagesTable].select {
                filter { eq("id", pinnedId) }
            }.decodeSingleOrNull<Message>()
            emit(pinnedMessage)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error observing pinned message", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(conversationsTable, e) ||
                SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)
            ) {
                logTableMissingOnce(conversationsTable)
                logTableMissingOnce(messagesTable)
            }
            emit(null)
        }
    }

    override suspend fun markConversationAsRead(currentUserId: String, otherUserId: String) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, otherUserId)

        supabase.postgrest[conversationsTable].update({
            set("unread_count", 0)
        }) {
            filter { eq("id", rowId) }
        }

        val chatMessages = try {
            if (!SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
                emptyList()
            } else {
                supabase.postgrest[messagesTable].select {
                filter { eq("conversation_id", conversationId(currentUserId, otherUserId)) }
                }.decodeList<Message>()
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error loading messages to mark as read", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)) {
                logTableMissingOnce(messagesTable)
            }
            emptyList()
        }

        chatMessages
            .filter { it.receiverId == currentUserId && it.status != MessageStatus.READ }
            .forEach { msg ->
                try {
                    supabase.postgrest[messagesTable].update({
                        set("status", MessageStatus.READ.name)
                    }) {
                        filter { eq("id", msg.id) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseMessageRepo", "Error marking message as read", e)
                }
            }
    }

    private fun conversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }

    private fun conversationRowId(ownerId: String, otherId: String): String {
        return "${ownerId}__${otherId}"
    }

    private suspend fun updateConversation(ownerId: String, otherId: String, message: Message, increaseUnread: Boolean) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) {
            return
        }

        try {
             val rowId = conversationRowId(ownerId, otherId)
             val currentConversation = try {
                 supabase.postgrest[conversationsTable].select {
                     filter { eq("id", rowId) }
                 }.decodeSingleOrNull<Conversation>()
             } catch (_: Exception) {
                 null
             }

             val currentUnread = if (increaseUnread) currentConversation?.unreadCount ?: 0 else 0

             val preview = when (message.type) {
                 MessageType.TEXT -> message.content.take(50)
                 MessageType.IMAGE -> "[Image]"
                 MessageType.VIDEO -> "[Video]"
                 MessageType.AUDIO -> "[Audio]"
                 MessageType.LOCATION -> "[Location]"
                 MessageType.FILE -> message.content.ifBlank { "[File]" }
             }

             val conversation = Conversation(
                 id = rowId,
                 otherUserId = otherId,
                 title = currentConversation?.title.orEmpty(),
                 lastMessagePreview = preview,
                 lastMessageTimestamp = message.timestamp,
                 unreadCount = if (increaseUnread) currentUnread + 1 else 0,
                 isGroup = false,
                 participants = listOf(ownerId, otherId),
                 pinnedMessageId = currentConversation?.pinnedMessageId
             )
             supabase.postgrest[conversationsTable].upsert(
                 mapOf(
                     "id" to conversation.id,
                     "owner_id" to ownerId,
                     "other_user_id" to conversation.otherUserId,
                     "title" to conversation.title,
                     "last_message_preview" to conversation.lastMessagePreview,
                     "last_message_timestamp" to conversation.lastMessageTimestamp,
                     "unread_count" to conversation.unreadCount,
                     "is_group" to conversation.isGroup,
                     "participants" to conversation.participants,
                     "pinned_message_id" to conversation.pinnedMessageId
                 )
             )
        } catch (e: Exception) {
             android.util.Log.e("SupabaseMessageRepo", "Error updating conversation", e)
             if (SupabaseSchemaGuard.markMissingTableIfNeeded(conversationsTable, e)) {
                 logTableMissingOnce(conversationsTable)
             }
        }
    }

    private suspend fun syncMessagesFromRemote(currentUserId: String, otherUserId: String) {
        if (!SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            return
        }

        try {
            val conversationId = conversationId(currentUserId, otherUserId)
            val remoteMessages = supabase.postgrest[messagesTable].select {
                filter {
                    eq("conversation_id", conversationId)
                }
                order("timestamp", Order.ASCENDING)
            }.decodeList<Message>()

            if (remoteMessages.isNotEmpty()) {
                messageDao.insertAll(remoteMessages.map { it.toEntity() })
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error syncing messages", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)) {
                logTableMissingOnce(messagesTable)
            }
        }
    }

    private fun Message.toEntity() = com.example.parachat.data.room.chat.MessageEntity(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        mediaUrl = mediaUrl,
        mediaThumbnailUrl = mediaThumbnailUrl,
        mediaDurationMillis = mediaDurationMillis,
        latitude = latitude,
        longitude = longitude,
        conversationId = conversationId,
        timestamp = timestamp,
        type = type.name,
        status = status.name
    )

    private fun com.example.parachat.data.room.chat.MessageEntity.toDomain() = Message(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        mediaUrl = mediaUrl,
        mediaThumbnailUrl = mediaThumbnailUrl,
        mediaDurationMillis = mediaDurationMillis,
        latitude = latitude,
        longitude = longitude,
        type = MessageType.valueOf(type),
        status = MessageStatus.valueOf(status),
        timestamp = timestamp,
        conversationId = conversationId
    )

    companion object {
        private const val MESSAGE_SYNC_INTERVAL_MS = 2_000L
    }
}
