package com.example.parachat.domain

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insert(user: User)
    fun getAll(): Flow<List<User>>
    fun observeUser(userId: String): Flow<User?>
    fun observeContactIds(ownerId: String): Flow<Set<String>>
    suspend fun addContact(ownerId: String, contactUserId: String)
    suspend fun removeContact(ownerId: String, contactUserId: String)
    suspend fun updateStatus(userId: String, status: UserStatus)
}
