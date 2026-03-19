package com.example.parachat.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.GroupRepository
import com.example.parachat.navigation.GroupManagementRoute
import com.example.parachat.ui.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val args = savedStateHandle.toRoute<GroupManagementRoute>()
    private val groupId = args.groupId

    private val _group = MutableStateFlow<Group?>(null)
    val group = _group.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName = _groupName.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _selectedMembers = MutableStateFlow<Set<String>>(emptySet())
    val selectedMembers = _selectedMembers.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _uiEvent = Channel<UIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val currentUserId: String = authRepository.getCurrentUser()?.uid.orEmpty()

    init {
        observeGroup()
        loadUsers()
    }

    private fun observeGroup() {
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                _group.value = group
                if (group != null) {
                    _groupName.value = group.name
                    _selectedMembers.value = group.members.toSet()
                }
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAll().collect { users ->
                _allUsers.value = users
            }
        }
    }

    fun onGroupNameChange(name: String) {
        _groupName.value = name
    }

    fun onMemberToggled(userId: String, selected: Boolean) {
        val members = _selectedMembers.value.toMutableSet()
        if (selected) {
            members.add(userId)
        } else {
            val current = _group.value ?: return
            if (userId == current.creatorId) {
                viewModelScope.launch {
                    _uiEvent.send(UIEvent.ShowSnackBar("O criador não pode ser removido"))
                }
                return
            }
            members.remove(userId)
        }
        _selectedMembers.value = members
    }

    fun saveChanges() {
        val currentGroup = _group.value ?: return
        val updatedName = _groupName.value.trim()
        val updatedMembers = _selectedMembers.value.toList()

        if (updatedName.isBlank()) {
            viewModelScope.launch { _uiEvent.send(UIEvent.ShowSnackBar("Nome do grupo não pode estar vazio")) }
            return
        }

        if (updatedMembers.size < 2) {
            viewModelScope.launch { _uiEvent.send(UIEvent.ShowSnackBar("Um grupo deve ter pelo menos 2 membros")) }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val updatedGroup = currentGroup.copy(name = updatedName, members = updatedMembers)
                groupRepository.updateGroup(updatedGroup)
                _uiEvent.send(UIEvent.ShowSnackBar("Grupo atualizado"))
            } catch (e: Exception) {
                _uiEvent.send(UIEvent.ShowSnackBar("Erro ao atualizar grupo: ${e.message}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun leaveGroup() {
        val userId = currentUserId
        val currentGroup = _group.value ?: return
        if (userId.isBlank()) return
        if (userId == currentGroup.creatorId) {
            viewModelScope.launch {
                _uiEvent.send(UIEvent.ShowSnackBar("O criador não pode sair do grupo"))
            }
            return
        }

        viewModelScope.launch {
            try {
                groupRepository.removeMember(currentGroup.id, userId)
                _uiEvent.send(UIEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvent.send(UIEvent.ShowSnackBar("Erro ao sair do grupo: ${e.message}"))
            }
        }
    }
}
