package com.example.parachat.data.room.user

import com.example.parachat.domain.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insert(user: User)
    fun getAll(): Flow<List<User>>
}
