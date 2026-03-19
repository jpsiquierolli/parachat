package com.example.parachat.domain.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("creator_id")
    val creatorId: String = "",
    val members: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)
