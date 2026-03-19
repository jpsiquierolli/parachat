package com.example.parachat.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.MessageRepository
import kotlinx.coroutines.delay

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var allUsersCache = emptyList<User>()

    init {
        fetchData()
        updatePresence()
    }

    private fun updatePresence() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.updateStatus(currentUserId, UserStatus.ONLINE)
        }
    }

    private fun fetchData() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            
            // Observe conversations
            launch {
                messageRepository.observeConversations(currentUserId).collect {
                    _conversations.value = it
                    _isLoading.value = false
                }
            }

            // Observe current user
            launch {
                userRepository.observeUser(currentUserId).collect {
                    _currentUser.value = it
                }
            }

            // Fetch users for search
            launch {
                userRepository.getAll().collect { allUsers ->
                    allUsersCache = allUsers.filter { it.id != currentUserId }
                    if (_searchQuery.value.isNotBlank()) {
                         onSearchQueryChange(_searchQuery.value)
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _users.value = emptyList() // Don't show users unless searching
        } else {
            _users.value = allUsersCache.filter {
                it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
    }

    fun signOut() {
        val currentUserId = authRepository.getCurrentUser()?.uid
        if (currentUserId != null) {
            viewModelScope.launch {
                userRepository.updateStatus(currentUserId, UserStatus.OFFLINE)
                authRepository.signOut()
            }
        } else {
            authRepository.signOut()
        }
    }
}
