package parachat.domain.chat

import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(message: Message)
    fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>>
}

