package com.example.parachat.domain

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val username: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val status: String = UserStatus.OFFLINE.name,
    val about: String = "",
    @SerialName("last_seen")
    val lastSeen: Long = 0L
)
