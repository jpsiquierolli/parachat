package com.example.parachat.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.GroupRepository
import com.example.parachat.ui.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _selectedUsers = MutableStateFlow<Set<String>>(emptySet())
    val selectedUsers = _selectedUsers.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName = _groupName.asStateFlow()

    private val _uiEvent = Channel<UIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getAll().collect { allUsers ->
                _users.value = allUsers.filter { it.id != currentUserId }
            }
        }
    }

    fun onGroupNameChange(name: String) {
        _groupName.value = name
    }

    fun toggleUserSelection(userId: String) {
        val current = _selectedUsers.value.toMutableSet()
        if (current.contains(userId)) {
            current.remove(userId)
        } else {
            current.add(userId)
        }
        _selectedUsers.value = current
    }

    fun createGroup() {
        val name = _groupName.value
        val members = _selectedUsers.value.toList() + (authRepository.getCurrentUser()?.uid ?: "")
        
        if (name.isBlank()) {
            viewModelScope.launch { _uiEvent.send(UIEvent.ShowSnackBar("Nome do grupo não pode estar vazio")) }
            return
        }
        
        if (members.size < 2) {
             viewModelScope.launch { _uiEvent.send(UIEvent.ShowSnackBar("Selecione pelo menos um membro")) }
             return
        }

        viewModelScope.launch {
            try {
                val group = Group(
                    name = name,
                    creatorId = authRepository.getCurrentUser()?.uid ?: "",
                    members = members
                )
                groupRepository.createGroup(group)
                _uiEvent.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvent.send(UIEvent.ShowSnackBar("Erro ao criar grupo: ${e.message}"))
            }
        }
    }
}
