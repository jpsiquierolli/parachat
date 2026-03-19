package com.example.parachat.data.room.user

import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {
    override suspend fun insert(user: User) {
        dao.insert(user.toEntity())
    }

    override fun getAll(): Flow<List<User>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeUser(userId: String): Flow<User?> {
        return dao.observeUser(userId).map { it?.toDomain() }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        // Status updates are primarily for remote
    }
}
