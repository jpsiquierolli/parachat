package com.example.parachat.data.supabase.user

import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.data.room.user.UserEntity
import com.example.parachat.data.supabase.SupabaseSchemaGuard
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import com.example.parachat.domain.displayNameFromParts
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

class SupabaseUserRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDb: ParachatDatabase
) : UserRepository {

    private val userDao = localDb.userDao
    private val usersTable = "users"
    private val contactsTable = "contacts"

    private fun logTableMissingOnce(table: String) {
        android.util.Log.e(
            "SupabaseUserRepository",
            "Supabase table '$table' is missing. Run the SQL migration scripts in parachat/supabase/migrations."
        )
    }

    override suspend fun insert(user: User) {
        userDao.insert(
            UserEntity(
                id = user.id,
                email = user.email,
                username = displayNameFromParts(user.username, user.email, user.id),
                photoUrl = user.photoUrl,
                status = user.status,
                about = user.about,
                lastSeen = user.lastSeen
            )
        )

        if (!SupabaseSchemaGuard.isTableAvailable(usersTable)) {
            return
        }

        try {
            supabase.postgrest[usersTable].upsert(user)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error inserting user", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(usersTable, e)) {
                logTableMissingOnce(usersTable)
            }
        }
    }

    override fun getAll(): Flow<List<User>> = flow {
        // Emit from cache immediately (partial data)
        try {
            val cachedEntities = userDao.getAll().first()
            if (cachedEntities.isNotEmpty()) {
                val cachedUsers = cachedEntities.map { 
                    User(
                        id = it.id,
                        email = it.email,
                        username = it.username,
                        photoUrl = it.photoUrl,
                        status = it.status,
                        about = it.about,
                        lastSeen = it.lastSeen
                    )
                }
                emit(cachedUsers)
            }
        } catch (e: Exception) {
            // Ignore cache read errors
        }

        if (!SupabaseSchemaGuard.isTableAvailable(usersTable)) {
            return@flow
        }

        try {
            val users = supabase.postgrest[usersTable].select().decodeList<User>()
            android.util.Log.d("SupabaseUserRepository", "Fetched ${users.size} users from Supabase")
            
            emit(users)
            
            // Update cache
            if (users.isNotEmpty()) {
                users.forEach { 
                    userDao.insert(
                        UserEntity(
                            id = it.id,
                            email = it.email,
                            username = displayNameFromParts(it.username, it.email, it.id),
                            photoUrl = it.photoUrl,
                            status = it.status,
                            about = it.about,
                            lastSeen = it.lastSeen
                        )
                    )
                }
            } else {
                 android.util.Log.d("SupabaseUserRepository", "Supabase returned empty user list")
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error fetching users from Supabase", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(usersTable, e)) {
                logTableMissingOnce(usersTable)
            }
        }
    }

    override fun observeUser(userId: String): Flow<User?> = flow {
        if (!SupabaseSchemaGuard.isTableAvailable(usersTable)) {
            val cached = userDao.getBy(userId)
            emit(
                cached?.let {
                    User(
                        id = it.id,
                        email = it.email,
                        username = it.username,
                        photoUrl = it.photoUrl,
                        status = it.status,
                        about = it.about,
                        lastSeen = it.lastSeen
                    )
                }
            )
            return@flow
        }

        try {
            val user = supabase.postgrest[usersTable].select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<User>()
            emit(user)
            
            // Should also persist partial update to Room
            if (user != null) {
                userDao.insert(
                    UserEntity(
                        id = user.id,
                        email = user.email,
                        username = displayNameFromParts(user.username, user.email, user.id),
                        photoUrl = user.photoUrl,
                        status = user.status,
                        about = user.about,
                        lastSeen = user.lastSeen
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error observing user", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(usersTable, e)) {
                logTableMissingOnce(usersTable)
            }
            // Try to load from cache as fallback
            val cached = userDao.getBy(userId)
            if (cached != null) {
                 emit(
                     User(
                         id = cached.id,
                         email = cached.email,
                         username = cached.username,
                         photoUrl = cached.photoUrl,
                         status = cached.status,
                         about = cached.about,
                         lastSeen = cached.lastSeen
                     )
                 )
            } else {
                 emit(null)
            }
        }
    }

    override fun observeContactIds(ownerId: String): Flow<Set<String>> = flow {
        if (!SupabaseSchemaGuard.isTableAvailable(contactsTable)) {
            emit(emptySet())
            return@flow
        }

        while (coroutineContext.isActive) {
            try {
                val rows = supabase.postgrest[contactsTable].select {
                    filter { eq("owner_id", ownerId) }
                }.decodeList<ContactRow>()
                emit(rows.map { it.contactUserId }.toSet())
            } catch (e: Exception) {
                android.util.Log.e("SupabaseUserRepository", "Error observing contacts", e)
                if (SupabaseSchemaGuard.markMissingTableIfNeeded(contactsTable, e)) {
                    logTableMissingOnce(contactsTable)
                }
                emit(emptySet())
            }

            delay(CONTACTS_REFRESH_INTERVAL_MS)
        }
    }

    override suspend fun addContact(ownerId: String, contactUserId: String) {
        if (ownerId.isBlank() || contactUserId.isBlank() || ownerId == contactUserId) return
        if (!SupabaseSchemaGuard.isTableAvailable(contactsTable)) return

        try {
            supabase.postgrest[contactsTable].upsert(
                mapOf(
                    "owner_id" to ownerId,
                    "contact_user_id" to contactUserId
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error adding contact", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(contactsTable, e)) {
                logTableMissingOnce(contactsTable)
            }
        }
    }

    override suspend fun removeContact(ownerId: String, contactUserId: String) {
        if (ownerId.isBlank() || contactUserId.isBlank()) return
        if (!SupabaseSchemaGuard.isTableAvailable(contactsTable)) return

        try {
            supabase.postgrest[contactsTable].delete {
                filter {
                    eq("owner_id", ownerId)
                    eq("contact_user_id", contactUserId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseUserRepository", "Error removing contact", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(contactsTable, e)) {
                logTableMissingOnce(contactsTable)
            }
        }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        if (!SupabaseSchemaGuard.isTableAvailable(usersTable)) {
            return
        }

        try {
            supabase.postgrest[usersTable].update(
                {
                   set("status", status.name)
                   set("last_seen", System.currentTimeMillis()) 
                }
            ) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
             android.util.Log.e("SupabaseUserRepository", "Error updating status", e)
             if (SupabaseSchemaGuard.markMissingTableIfNeeded(usersTable, e)) {
                 logTableMissingOnce(usersTable)
             }
        }
    }

    @Serializable
    private data class ContactRow(
        @SerialName("owner_id") val ownerId: String,
        @SerialName("contact_user_id") val contactUserId: String
    )

    companion object {
        private const val CONTACTS_REFRESH_INTERVAL_MS = 2_000L
    }
}
