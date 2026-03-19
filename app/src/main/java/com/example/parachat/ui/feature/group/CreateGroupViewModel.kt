package com.example.parachat.ui.feature.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.chat.FirebaseGroupRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.chat.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateGroupViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())
    private val groupRepository = FirebaseGroupRepository(FirebaseDatabase.getInstance())

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _selectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIds = _selectedUserIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAll().collect { allUsers ->
                _users.value = allUsers.filter { it.id != currentUserId }
            }
        }
    }

    fun toggleUserSelection(userId: String) {
        val current = _selectedUserIds.value
        if (current.contains(userId)) {
            _selectedUserIds.value = current - userId
        } else {
            _selectedUserIds.value = current + userId
        }
    }

    fun createGroup(name: String, description: String, onCreated: (String) -> Unit) {
        if (name.isBlank() || _selectedUserIds.value.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val group = Group(
                    name = name,
                    description = description,
                    ownerId = currentUserId,
                    members = _selectedUserIds.value.toList() + currentUserId
                )
                val groupId = groupRepository.createGroup(group)
                onCreated(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
