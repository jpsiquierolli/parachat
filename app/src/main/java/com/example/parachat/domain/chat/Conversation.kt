package com.example.parachat.domain.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = "",
    @SerialName("other_user_id")
    val otherUserId: String = "",
    val title: String = "",
    @SerialName("last_message_preview")
    val lastMessagePreview: String = "",
    @SerialName("last_message_timestamp")
    val lastMessageTimestamp: Long = 0L,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("is_group")
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    @SerialName("pinned_message_id")
    val pinnedMessageId: String? = null
)
