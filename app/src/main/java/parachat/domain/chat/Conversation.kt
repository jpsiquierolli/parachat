package parachat.domain.chat

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = "",
    val otherUserId: String = "",
    val title: String = "",
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val pinnedMessageId: String? = null
)

