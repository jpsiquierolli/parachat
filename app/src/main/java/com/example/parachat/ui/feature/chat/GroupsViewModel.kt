package com.example.parachat.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        val userId = authRepository.getCurrentUser()?.uid ?: run {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            groupRepository.observeGroups(userId).collect { groups ->
                _groups.value = groups.sortedBy { it.name.lowercase() }
                _isLoading.value = false
            }
        }
    }
}
