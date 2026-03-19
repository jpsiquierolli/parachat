package com.example.parachat.domain.chat

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class Group(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var photoUrl: String? = null,
    var ownerId: String = "",
    var members: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
)
