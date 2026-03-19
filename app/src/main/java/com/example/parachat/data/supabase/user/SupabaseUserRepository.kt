package com.example.parachat.data.supabase.user

import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.data.room.user.UserEntity
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SupabaseUserRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDb: ParachatDatabase
) : UserRepository {

    private val userDao = localDb.userDao

    override suspend fun insert(user: User) {
        try {
            // Note: If using custom 'users' table, and JSON mapping is snake_case (via @SerialName),
            // this insert should work assuming table columns are id, email, username, photo_url, last_seen.
            // If table uses 'lastSeen', our @SerialName would conflict unless columns match.
            // Usually Supabase auto-generated schema is snake_case.
            supabase.postgrest["users"].upsert(user) // Use upsert to handle duplicates/updates
            userDao.insert(UserEntity(user.id, user.email, user.username ?: "Unknown"))
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error inserting user", e)
            throw e
        }
    }

    override fun getAll(): Flow<List<User>> = flow {
        // Emit from cache immediately (partial data)
        try {
            val cachedEntities = userDao.getAll().first()
            if (cachedEntities.isNotEmpty()) {
                val cachedUsers = cachedEntities.map { 
                    User(id = it.id, email = it.email, username = it.username) 
                }
                emit(cachedUsers)
            }
        } catch (e: Exception) {
            // Ignore cache read errors
        }

        // Fetch from Supabase (full data)
        try {
            val users = supabase.postgrest["users"].select().decodeList<User>()
            // Log success
            android.util.Log.d("SupabaseUserRepository", "Fetched ${users.size} users from Supabase")
            
            emit(users)
            
            // Update cache
            if (users.isNotEmpty()) {
                users.forEach { 
                    userDao.insert(UserEntity(it.id, it.email, it.username ?: "Unknown"))
                }
            } else {
                 android.util.Log.d("SupabaseUserRepository", "Supabase returned empty user list")
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error fetching users from Supabase", e)
            // If we have emitted cached data, the UI is showing something.
            // If cache was empty and this fails, UI shows empty.
            // We should probably re-emit cache if it was already emitted? No, flow stays open?
            // Flow usually completes if we don't use callbackFlow. 'flow' builder completes after block.
        }
    }

    override fun observeUser(userId: String): Flow<User?> = flow {
        try {
            val user = supabase.postgrest["users"].select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<User>()
            emit(user)
            
            // Should also persist partial update to Room
            if (user != null) {
                userDao.insert(UserEntity(user.id, user.email, user.username ?: "Unknown"))
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error observing user", e)
            // Try to load from cache as fallback
            val cached = userDao.getBy(userId)
            if (cached != null) {
                 emit(User(id = cached.id, email = cached.email, username = cached.username))
            } else {
                 emit(null)
            }
        }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        try {
            supabase.postgrest["users"].update(
                {
                   set("status", status.name)
                   set("lastSeen", System.currentTimeMillis()) 
                }
            ) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
             android.util.Log.e("SupabaseUserRepository", "Error updating status", e)
        }
    }
}
