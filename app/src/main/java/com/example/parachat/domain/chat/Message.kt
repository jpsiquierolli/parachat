package com.example.parachat.domain.chat

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "", // Can be userId or groupId
    val groupId: String? = null,
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaDurationMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val timestamp: Long = System.currentTimeMillis()
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
