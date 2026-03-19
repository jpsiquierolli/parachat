package com.example.parachat.domain.chat

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String? = null,
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
