package com.example.parachat.ui.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.Exception

class HomeViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())
    private val database = FirebaseDatabase.getInstance()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allUsersCache = emptyList<User>()
    private var ensuredProfile = false
    private var seededDefaultContact = false
    private var lastUserId: String? = null

    init {
        fetchUsers()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _users.value = allUsersCache
        } else {
            _users.value = allUsersCache.filter {
                it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            // First, sync all auth users to RTDB (one-time, on first load)
            syncAuthUsersToDatabase()

            userRepository.getAll().collect { rawUsers ->
                val currentUserId = authRepository.getCurrentUser()?.uid

                // Reset flags if user changed (logout/login different account)
                if (lastUserId != currentUserId) {
                    lastUserId = currentUserId
                    ensuredProfile = false
                    seededDefaultContact = false
                }

                val mutableUsers = rawUsers.toMutableList()

                // If the current user has no profile stored yet, create one on-the-fly (and add locally so UI updates immediately)
                if (!ensuredProfile && currentUserId != null && rawUsers.none { it.id == currentUserId }) {
                    ensuredProfile = true
                    val authUser = authRepository.getCurrentUser()
                    val email = authUser?.email ?: ""
                    val username = authUser?.displayName
                        ?: email.substringBefore('@', missingDelimiterValue = "Usuário")
                    val user = User(
                        id = currentUserId,
                        email = email,
                        username = username,
                        status = UserStatus.ONLINE.name,
                        lastSeen = System.currentTimeMillis()
                    )
                    mutableUsers.add(user)
                    userRepository.insert(user)
                }

                // Seed a default contact if there are no other users yet (and add locally)
                if (!seededDefaultContact && (mutableUsers.isEmpty() || mutableUsers.all { it.id == currentUserId })) {
                    seededDefaultContact = true
                    val supportUser = User(
                        id = "demo-support",
                        email = "support@parachat.dev",
                        username = "Suporte Parachat",
                        status = UserStatus.OFFLINE.name,
                        lastSeen = System.currentTimeMillis()
                    )
                    mutableUsers.add(supportUser)
                    userRepository.insert(supportUser)

                    val mobileUser = User(
                        id = "demo-mobile",
                        email = "mobile@parachat.dev",
                        username = "Usuário Mobile",
                        status = UserStatus.ONLINE.name,
                        lastSeen = System.currentTimeMillis()
                    )
                    mutableUsers.add(mobileUser)
                    userRepository.insert(mobileUser)
                }

                val filtered = mutableUsers.filter { it.id != currentUserId }
                allUsersCache = filtered
                onSearchQueryChange(_searchQuery.value)

                _currentUser.value = mutableUsers.find { it.id == currentUserId }
            }
        }
    }

    private suspend fun syncAuthUsersToDatabase() {
        try {
            val usersRef = database.getReference("users")
            val snapshot = usersRef.get().await()

            // Get count of users already in RTDB
            val existingCount = snapshot.childrenCount.toInt()

            // If RTDB is empty or very sparse (< 3 users), fetch and seed from Auth
            if (existingCount < 3) {
                Log.d("HomeViewModel", "Syncing auth users to RTDB (found $existingCount existing)")

                // Get current user to build list
                val currentUserId = authRepository.getCurrentUser()?.uid

                // Try to get users from RTDB and at least ensure current user is there
                snapshot.children.forEach { child ->
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        Log.d("HomeViewModel", "Found existing user: ${user.email}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error syncing auth users", e)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
