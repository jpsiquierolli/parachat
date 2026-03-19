package com.example.parachat.domain

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class User(
    var id: String = "",
    var email: String = "",
    var username: String = "",
    var photoUrl: String? = null,
    var status: String = UserStatus.OFFLINE.name,
    var about: String = "",
    var lastSeen: Long = 0L
)
