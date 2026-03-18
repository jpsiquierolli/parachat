package com.example.parachat.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.domain.User

class HomeViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allUsersCache = emptyList<User>()

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
            userRepository.getAll().collect { allUsers ->
                val currentUserId = authRepository.getCurrentUser()?.uid
                val filtered = allUsers.filter { it.id != currentUserId }
                allUsersCache = filtered
                onSearchQueryChange(_searchQuery.value)

                _currentUser.value = allUsers.find { it.id == currentUserId }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
