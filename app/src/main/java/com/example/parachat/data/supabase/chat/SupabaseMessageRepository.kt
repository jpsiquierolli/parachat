package com.example.parachat.data.supabase.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.data.supabase.SupabaseSchemaGuard
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.domain.chat.sortedForChat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class SupabaseMessageRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDb: ParachatDatabase,
    @ApplicationContext private val context: Context
) : MessageRepository {

    private val messageDao = localDb.messageDao
    private val messagesTable = "messages"
    private val conversationsTable = "conversations"
    private val groupsTable = "groups"

    private val unreadBaselineByConversation = mutableMapOf<String, Int>()

    private fun logTableMissingOnce(table: String) {
        android.util.Log.e(
            "SupabaseMessageRepo",
            "Supabase table '$table' is missing. Run the SQL migration scripts in parachat/supabase/migrations."
        )
    }

    override suspend fun sendMessage(message: Message, isGroup: Boolean) {
        if (isGroup) {
            sendGroupMessage(message)
        } else {
            sendDirectMessage(message)
        }
    }

    override fun getMessages(currentUserId: String, chatId: String, isGroup: Boolean): Flow<List<Message>> = channelFlow {
        val conversationId = conversationId(currentUserId, chatId, isGroup)

        if (SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            launch {
                syncMessagesFromRemote(currentUserId, chatId, isGroup)
                while (isActive) {
                    delay(MESSAGE_SYNC_INTERVAL_MS)
                    syncMessagesFromRemote(currentUserId, chatId, isGroup)
                }
            }
        }

        val localSource = if (isGroup) {
            messageDao.getMessagesByConversationId(conversationId)
        } else {
            messageDao.getMessagesForChat(currentUserId, chatId)
        }

        localSource
            .map { entities -> entities.map { it.toDomain() }.sortedForChat() }
            .collect { send(it) }
    }

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = channelFlow {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) {
            send(emptyList())
            return@channelFlow
        }

        launch {
            while (isActive) {
                try {
                    val conversations = supabase.postgrest[conversationsTable].select {
                        filter { eq("owner_id", currentUserId) }
                        order("last_message_timestamp", Order.DESCENDING)
                    }.decodeList<Conversation>()

                    val mergedConversations = mergeMissingGroupConversations(currentUserId, conversations)

                    maybeNotifyUnread(mergedConversations)
                    send(mergedConversations)
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseMessageRepo", "Error fetching conversations", e)
                    if (SupabaseSchemaGuard.markMissingTableIfNeeded(conversationsTable, e)) {
                        logTableMissingOnce(conversationsTable)
                    }
                    send(emptyList())
                }

                delay(CONVERSATIONS_SYNC_INTERVAL_MS)
            }
        }

        awaitClose { }
    }

    override suspend fun pinMessage(currentUserId: String, chatId: String, message: Message, isGroup: Boolean) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, chatId, isGroup)
        supabase.postgrest[conversationsTable].update({
            set("pinned_message_id", message.id)
        }) {
            filter { eq("id", rowId) }
        }
    }

    override suspend fun unpinMessage(currentUserId: String, chatId: String, isGroup: Boolean) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, chatId, isGroup)
        supabase.postgrest[conversationsTable].update({
            set<String?>("pinned_message_id", null)
        }) {
            filter { eq("id", rowId) }
        }
    }

    override fun observePinnedMessage(currentUserId: String, chatId: String, isGroup: Boolean): Flow<Message?> = flow {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable) || !SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            emit(null)
            return@flow
        }

        try {
            val rowId = conversationRowId(currentUserId, chatId, isGroup)
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

    override suspend fun markConversationAsRead(currentUserId: String, chatId: String, isGroup: Boolean) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val rowId = conversationRowId(currentUserId, chatId, isGroup)

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
                    filter {
                        eq("conversation_id", conversationId(currentUserId, chatId, isGroup))
                        if (isGroup) {
                            eq("receiver_id", currentUserId)
                        }
                    }
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

    private suspend fun sendDirectMessage(message: Message) {
        val conversationId = directConversationId(message.senderId, message.receiverId)
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

            updateConversation(
                ownerId = message.senderId,
                chatId = message.receiverId,
                message = messageWithConvId,
                increaseUnread = false,
                isGroup = false,
                title = "",
                participants = listOf(message.senderId, message.receiverId)
            )
            updateConversation(
                ownerId = message.receiverId,
                chatId = message.senderId,
                message = messageWithConvId,
                increaseUnread = true,
                isGroup = false,
                title = "",
                participants = listOf(message.senderId, message.receiverId)
            )
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error sending message", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)) {
                logTableMissingOnce(messagesTable)
            }
        }

        try {
            messageDao.insert(messageWithConvId.toEntity())
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error saving message locally", e)
        }
    }

    private suspend fun sendGroupMessage(message: Message) {
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) return

        val group = try {
            supabase.postgrest[groupsTable].select {
                filter { eq("id", message.receiverId) }
            }.decodeSingleOrNull<Group>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error loading group for message send", e)
            null
        } ?: return

        val recipients = (group.members + group.creatorId).filter { it.isNotBlank() }.distinct()
        if (recipients.isEmpty()) return

        val groupConversationId = groupConversationId(group.id)
        val now = message.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis()

        try {
            recipients.forEach { recipientId ->
                val row = message.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    receiverId = recipientId,
                    conversationId = groupConversationId,
                    timestamp = now,
                    status = if (recipientId == message.senderId) MessageStatus.SENT else MessageStatus.DELIVERED
                )

                if (SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
                    supabase.postgrest[messagesTable].upsert(row)
                }

                updateConversation(
                    ownerId = recipientId,
                    chatId = group.id,
                    message = row,
                    increaseUnread = recipientId != message.senderId,
                    isGroup = true,
                    title = group.name,
                    participants = recipients
                )

                if (recipientId == message.senderId) {
                    messageDao.insert(row.toEntity())
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error sending group message", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(messagesTable, e)) {
                logTableMissingOnce(messagesTable)
            }
        }
    }

    private fun directConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }

    private fun groupConversationId(groupId: String): String = "group_$groupId"

    private fun conversationId(currentUserId: String, chatId: String, isGroup: Boolean): String {
        return if (isGroup) groupConversationId(chatId) else directConversationId(currentUserId, chatId)
    }

    private fun conversationRowId(ownerId: String, chatId: String, isGroup: Boolean): String {
        return if (isGroup) "${ownerId}__group__$chatId" else "${ownerId}__$chatId"
    }

    private suspend fun updateConversation(
        ownerId: String,
        chatId: String,
        message: Message,
        increaseUnread: Boolean,
        isGroup: Boolean,
        title: String,
        participants: List<String>
    ) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) {
            return
        }

        try {
            val rowId = conversationRowId(ownerId, chatId, isGroup)
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
                otherUserId = chatId,
                title = title.ifBlank { currentConversation?.title.orEmpty() },
                lastMessagePreview = preview,
                lastMessageTimestamp = message.timestamp,
                unreadCount = if (increaseUnread) currentUnread + 1 else 0,
                isGroup = isGroup,
                participants = participants,
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

    private suspend fun syncMessagesFromRemote(currentUserId: String, chatId: String, isGroup: Boolean) {
        if (!SupabaseSchemaGuard.isTableAvailable(messagesTable)) {
            return
        }

        try {
            val conversationId = conversationId(currentUserId, chatId, isGroup)
            val remoteMessages = supabase.postgrest[messagesTable].select {
                filter {
                    eq("conversation_id", conversationId)
                    if (isGroup) {
                        eq("receiver_id", currentUserId)
                    }
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

    private fun maybeNotifyUnread(conversations: List<Conversation>) {
        conversations.forEach { conversation ->
            val previous = unreadBaselineByConversation[conversation.id]
            if (previous == null) {
                if (conversation.unreadCount > 0) {
                    showUnreadNotification(conversation)
                }
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (conversation.isGroup) "Nova mensagem no grupo" else "Nova mensagem")
            .setContentText(
                conversation.title.ifBlank {
                    conversation.lastMessagePreview.ifBlank { "Voce recebeu uma nova mensagem" }
                }
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(conversation.id.hashCode(), notification)
    }

    private suspend fun mergeMissingGroupConversations(
        currentUserId: String,
        baseConversations: List<Conversation>
    ): List<Conversation> {
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) {
            return baseConversations
        }

        return try {
            val groups = supabase.postgrest[groupsTable].select {
                order("created_at", Order.DESCENDING)
            }.decodeList<Group>()

            val visibleGroups = groups.filter { group ->
                group.creatorId == currentUserId || group.members.contains(currentUserId)
            }

            val existingGroupIds = baseConversations
                .asSequence()
                .filter { it.isGroup }
                .map { it.otherUserId }
                .toSet()

            val synthetic = visibleGroups
                .filter { it.id !in existingGroupIds }
                .map { group ->
                    Conversation(
                        id = conversationRowId(currentUserId, group.id, isGroup = true),
                        otherUserId = group.id,
                        title = group.name,
                        lastMessagePreview = "",
                        lastMessageTimestamp = group.createdAt,
                        unreadCount = 0,
                        isGroup = true,
                        participants = (group.members + group.creatorId).distinct(),
                        pinnedMessageId = null
                    )
                }

            (baseConversations + synthetic).sortedByDescending { it.lastMessageTimestamp }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessageRepo", "Error merging group conversations", e)
            baseConversations
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
        private const val CONVERSATIONS_SYNC_INTERVAL_MS = 2_500L
    }
}
