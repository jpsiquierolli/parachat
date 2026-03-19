package com.example.parachat.domain.chat

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class Conversation(
    var id: String = "",
    var otherUserId: String = "",
    var title: String = "",
    var lastMessagePreview: String = "",
    var lastMessageTimestamp: Long = 0L,
    var unreadCount: Int = 0,
    var isGroup: Boolean = false,
    var participants: List<String> = emptyList(),
    var pinnedMessageId: String? = null
)

