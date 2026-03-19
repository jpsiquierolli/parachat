package com.example.parachat.data.room.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val groupId: String?,
    val content: String,
    val mediaUrl: String?,
    val type: String,
    val status: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?
)

fun Message.toEntity() = MessageEntity(
    id = id,
    senderId = senderId,
    receiverId = receiverId,
    groupId = groupId,
    content = content,
    mediaUrl = mediaUrl,
    type = type.name,
    status = status.name,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude
)

fun MessageEntity.toDomain() = Message(
    id = id,
    senderId = senderId,
    receiverId = receiverId,
    groupId = groupId,
    content = content,
    mediaUrl = mediaUrl,
    type = MessageType.valueOf(type),
    status = MessageStatus.valueOf(status),
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude
)
