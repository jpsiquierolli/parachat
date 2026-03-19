package com.example.parachat.data.room.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaDurationMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val conversationId: String = "",
    val timestamp: Long,
    val type: String,
    val status: String
)
