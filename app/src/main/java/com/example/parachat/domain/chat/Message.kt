package com.example.parachat.domain.chat

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class Message(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var groupId: String? = null,
    var content: String = "",
    var mediaUrl: String? = null,
    var mediaThumbnailUrl: String? = null,
    var mediaDurationMillis: Long? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var type: MessageType = MessageType.TEXT,
    var status: MessageStatus = MessageStatus.SENT,
    var timestamp: Long = System.currentTimeMillis()
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
