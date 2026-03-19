package com.example.parachat.data.firebase.user

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.tasks.await
import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import com.example.parachat.domain.displayNameFromParts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseUserRepository(
    private val database: FirebaseDatabase,
    private val localDb: ParachatDatabase
) : UserRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val usersRef = database.getReference("users")
    private val userDao = localDb.userDao

    override suspend fun insert(user: User) {
        val normalizedUsername = displayNameFromParts(user.username, user.email, user.id)
        val normalizedUser = user.copy(username = normalizedUsername)
        usersRef.child(user.id).setValue(normalizedUser).await()
        userDao.insert(com.example.parachat.data.room.user.UserEntity(
            id = normalizedUser.id,
            email = normalizedUser.email,
            username = normalizedUser.username.orEmpty(),
            photoUrl = normalizedUser.photoUrl,
            status = normalizedUser.status,
            about = normalizedUser.about,
            lastSeen = normalizedUser.lastSeen
        ))
    }

    override fun getAll(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    try {
                        // Manually map to ensure safety
                        val id = it.child("id").getValue(String::class.java) ?: it.key ?: return@mapNotNull null
                        val email = it.child("email").getValue(String::class.java) ?: ""
                        val username = displayNameFromParts(
                            username = it.child("username").getValue(String::class.java),
                            email = email,
                            id = id
                        )
                        val photoUrl = it.child("photoUrl").getValue(String::class.java)
                        val status = it.child("status").getValue(String::class.java) ?: UserStatus.OFFLINE.name
                        val about = it.child("about").getValue(String::class.java) ?: ""
                        val lastSeen = it.child("lastSeen").getValue(Long::class.java) ?: 0L
                        
                        User(id, email, username, photoUrl, status, about, lastSeen)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseUserRepo", "Error parsing user: ${it.key}", e)
                        null
                    }
                }

                android.util.Log.d("FirebaseUserRepo", "Loaded ${users.size} users from Firebase, total children: ${snapshot.childrenCount}")

                // Cache to Room
                repositoryScope.launch {
                    users.forEach { user ->
                        userDao.insert(com.example.parachat.data.room.user.UserEntity(
                            id = user.id,
                            email = user.email,
                            username = user.username.orEmpty(),
                            photoUrl = user.photoUrl,
                            status = user.status,
                            about = user.about,
                            lastSeen = user.lastSeen
                        ))
                    }
                }
                
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseUserRepo", "getAll cancelled: ${error.message}", error.toException())
                // On error/offline, try to load from Room
                repositoryScope.launch {
                    userDao.getAll().collect { entities ->
                        val users = entities.map {
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
                        trySend(users)
                    }
                }
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: userId
                val email = snapshot.child("email").getValue(String::class.java).orEmpty()
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                val resolvedUsername = displayNameFromParts(
                    username = snapshot.child("username").getValue(String::class.java),
                    email = email,
                    id = id
                )

                val user = User(
                    id = id,
                    email = email,
                    username = resolvedUsername,
                    photoUrl = snapshot.child("photoUrl").getValue(String::class.java),
                    status = snapshot.child("status").getValue(String::class.java) ?: UserStatus.OFFLINE.name,
                    about = snapshot.child("about").getValue(String::class.java) ?: "",
                    lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                )

                repositoryScope.launch {
                    userDao.insert(
                        com.example.parachat.data.room.user.UserEntity(
                            id = user.id,
                            email = user.email,
                            username = user.username.orEmpty(),
                            photoUrl = user.photoUrl,
                            status = user.status,
                            about = user.about,
                            lastSeen = user.lastSeen
                        )
                    )
                }

                // Backfill empty usernames so all clients can show the same display name.
                if (snapshot.child("username").getValue(String::class.java).isNullOrBlank()) {
                    repositoryScope.launch {
                        usersRef.child(id).child("username").setValue(resolvedUsername)
                    }
                }

                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.child(userId).addValueEventListener(listener)
        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        val updates = mapOf(
            "status" to status.name,
            "lastSeen" to System.currentTimeMillis()
        )
        usersRef.child(userId).updateChildren(updates).await()
    }
}
