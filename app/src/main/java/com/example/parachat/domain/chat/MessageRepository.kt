package com.example.parachat.domain.chat

import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(message: Message)
    fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>>
    fun getGroupMessages(groupId: String): Flow<List<Message>>
    fun observeConversations(currentUserId: String): Flow<List<Conversation>>
    suspend fun pinMessage(currentUserId: String, otherUserId: String, message: Message)
    suspend fun unpinMessage(currentUserId: String, otherUserId: String)
    fun observePinnedMessage(currentUserId: String, otherUserId: String): Flow<Message?>
    
    suspend fun pinGroupMessage(groupId: String, message: Message)
    suspend fun unpinGroupMessage(groupId: String)
    fun observePinnedGroupMessage(groupId: String): Flow<Message?>

    suspend fun markConversationAsRead(currentUserId: String, otherUserId: String)
}
