package com.example.parachat.ui.feature.home

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.data.firebase.chat.FirebaseGroupRepository
import com.example.parachat.data.firebase.user.FirebaseContactRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserStatus
import com.example.parachat.domain.chat.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())
    private val groupRepository = FirebaseGroupRepository(FirebaseDatabase.getInstance())
    private val contactRepository = FirebaseContactRepository(FirebaseDatabase.getInstance())

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var allContactsCache = emptyList<User>()
    private var allGroupsCache = emptyList<Group>()
    
    val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

    init {
        fetchData()
        updateUserStatus(UserStatus.ONLINE)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterData()
    }

    private fun filterData() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _contacts.value = allContactsCache
            _groups.value = allGroupsCache
        } else {
            _contacts.value = allContactsCache.filter {
                it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
            _groups.value = allGroupsCache.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    private fun fetchData() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            userRepository.observeUser(currentUserId).collect { user ->
                _currentUser.value = user
            }
        }

        viewModelScope.launch {
            combine(
                contactRepository.observeContacts(currentUserId),
                userRepository.getAll()
            ) { contactIds, allUsers ->
                allUsers.filter { contactIds.contains(it.id) }
            }.collect { contactUsers ->
                allContactsCache = contactUsers
                filterData()
            }
        }

        viewModelScope.launch {
            groupRepository.observeGroups(currentUserId).collect { groups ->
                allGroupsCache = groups
                filterData()
            }
        }
    }

    fun addContactByEmail(email: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByEmail(email)
                if (user != null) {
                    if (user.id == currentUserId) {
                        _error.value = "Você não pode adicionar a si mesmo."
                    } else {
                        contactRepository.addContact(currentUserId, user.id)
                    }
                } else {
                    _error.value = "Usuário não encontrado."
                }
            } catch (e: Exception) {
                _error.value = "Erro ao adicionar contato: ${e.message}"
            }
        }
    }

    fun importDeviceContacts(context: Context) {
        viewModelScope.launch {
            val deviceEmails = mutableListOf<String>()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null, null, null, null
            )
            cursor?.use {
                val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (it.moveToNext()) {
                    deviceEmails.add(it.getString(emailIndex))
                }
            }

            val allUsers = userRepository.getAll().first()
            val matchedUsers = allUsers.filter { deviceEmails.contains(it.email) && it.id != currentUserId }
            
            matchedUsers.forEach { user ->
                contactRepository.addContact(currentUserId, user.id)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun updateUserStatus(status: UserStatus) {
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                userRepository.updateStatus(currentUserId, status)
            }
        }
    }

    fun removeContact(contactId: String) {
        viewModelScope.launch {
            contactRepository.removeContact(currentUserId, contactId)
        }
    }

    fun signOut() {
        updateUserStatus(UserStatus.OFFLINE)
        authRepository.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        updateUserStatus(UserStatus.OFFLINE)
    }
}
