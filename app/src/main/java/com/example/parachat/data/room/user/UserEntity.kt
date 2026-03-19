package com.example.parachat.data.room.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.parachat.domain.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val photoUrl: String?,
    val status: String,
    val about: String,
    val lastSeen: Long
)

fun User.toEntity() = UserEntity(
    id = id,
    email = email,
    username = username,
    photoUrl = photoUrl,
    status = status,
    about = about,
    lastSeen = lastSeen
)

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    username = username,
    photoUrl = photoUrl,
    status = status,
    about = about,
    lastSeen = lastSeen
)
