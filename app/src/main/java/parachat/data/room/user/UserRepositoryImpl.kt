package parachat.data.room.user

import parachat.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {
    override suspend fun insert(user: User) {
        val entity = UserEntity(
            id = user.id,
            email = user.email,
            username = user.username
        )
        dao.insert(entity)
    }

    override fun getAll(): Flow<List<User>> {
        return dao.getAll().map { entities ->
            entities.map { entity ->
                User(
                    id = entity.id,
                    email = entity.email,
                    username = entity.username
                )
            }
        }
    }
}