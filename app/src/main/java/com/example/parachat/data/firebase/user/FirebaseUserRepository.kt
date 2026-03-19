package com.example.parachat.data.firebase.user

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus

class FirebaseUserRepository(
    private val database: FirebaseDatabase
) : UserRepository {

    private val usersRef = database.getReference("users")

    override suspend fun insert(user: User) {
        usersRef.child(user.id).setValue(user).await()
    }

    override fun getAll(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (child in snapshot.children) {
                    try {
                        val user = child.getValue(User::class.java)
                        if (user != null) {
                            users.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseUserRepository", "Error converting child to User: ${child.key}", e)
                    }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    trySend(snapshot.getValue(User::class.java))
                } catch (e: Exception) {
                    Log.e("FirebaseUserRepository", "Error converting snapshot to User", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.child(userId).addValueEventListener(listener)
        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    suspend fun getUserByEmail(email: String): User? {
        val snapshot = usersRef.orderByChild("email").equalTo(email).get().await()
        return snapshot.children.firstOrNull()?.let { 
            try {
                it.getValue(User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
        return snapshot.children.firstOrNull()?.let {
            try {
                it.getValue(User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        val updates = mapOf(
            "status" to status.name,
            "lastSeen" to System.currentTimeMillis()
        )
        usersRef.child(userId).updateChildren(updates).await()
        
        if (status == UserStatus.ONLINE) {
            usersRef.child(userId).child("status").onDisconnect().setValue(UserStatus.OFFLINE.name)
            usersRef.child(userId).child("lastSeen").onDisconnect().setValue(System.currentTimeMillis())
        }
    }
}
