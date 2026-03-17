package parachat.data.room.chat

import parachat.domain.chat.Message
import parachat.domain.chat.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val dao: MessageDao
) : MessageRepository {
    override suspend fun sendMessage(message: Message) {
        val entity = MessageEntity(
            id = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = message.content,
            timestamp = message.timestamp
        )
        dao.insert(entity)
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> {
        return dao.getMessagesForChat(currentUserId, otherUserId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    senderId = entity.senderId,
                    receiverId = entity.receiverId,
                    content = entity.content,
                    timestamp = entity.timestamp
                )
            }
        }
    }
}

