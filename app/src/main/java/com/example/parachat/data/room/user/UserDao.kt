package com.example.parachat.data.room.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getBy(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<UserEntity>>
}
