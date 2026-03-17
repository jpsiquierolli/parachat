package parachat.data.room.user

import parachat.domain.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insert(user: User)
    fun getAll(): Flow<List<User>>
}
