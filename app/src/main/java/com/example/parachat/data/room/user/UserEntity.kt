package com.example.parachat.data.room.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val photoUrl: String? = null,
    val status: String = "OFFLINE",
    val about: String = "",
    val lastSeen: Long = 0L
)
