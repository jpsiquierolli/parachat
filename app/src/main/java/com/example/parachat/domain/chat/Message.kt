package com.example.parachat.domain.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    @SerialName("receiver_id")
    val receiverId: String = "",
    val content: String = "",
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_thumbnail_url")
    val mediaThumbnailUrl: String? = null,
    @SerialName("media_duration_millis")
    val mediaDurationMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("conversation_id")
    val conversationId: String = ""
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    LOCATION,
    FILE
}

@Serializable
enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}

// Stable ordering: oldest first, then deterministic tie-breakers.
val CHAT_MESSAGE_ORDER: Comparator<Message> = compareBy<Message> { it.timestamp }
    .thenBy { it.id }
    .thenBy { it.senderId }

fun List<Message>.sortedForChat(): List<Message> = sortedWith(CHAT_MESSAGE_ORDER)

