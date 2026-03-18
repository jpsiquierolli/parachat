package com.example.parachat.domain

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insert(user: User)
    fun getAll(): Flow<List<User>>
    fun observeUser(userId: String): Flow<User?>
    suspend fun updateStatus(userId: String, status: UserStatus)
}
