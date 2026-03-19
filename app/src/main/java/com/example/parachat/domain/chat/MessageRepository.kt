package com.example.parachat.domain.chat

import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(message: Message, isGroup: Boolean = false)
    fun getMessages(currentUserId: String, chatId: String, isGroup: Boolean = false): Flow<List<Message>>
    fun observeConversations(currentUserId: String): Flow<List<Conversation>>
    suspend fun pinMessage(currentUserId: String, chatId: String, message: Message, isGroup: Boolean = false)
    suspend fun unpinMessage(currentUserId: String, chatId: String, isGroup: Boolean = false)
    fun observePinnedMessage(currentUserId: String, chatId: String, isGroup: Boolean = false): Flow<Message?>
    suspend fun markConversationAsRead(currentUserId: String, chatId: String, isGroup: Boolean = false)
}
