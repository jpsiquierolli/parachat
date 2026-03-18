package com.example.parachat.domain

import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    ONLINE,
    OFFLINE,
    BUSY
}

