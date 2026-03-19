package com.example.parachat.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.UserStatus
import com.example.parachat.domain.displayName
import com.example.parachat.domain.displayNameFromParts
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.MessageRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val localDb: com.example.parachat.data.room.ParachatDatabase
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _contactIds = MutableStateFlow<Set<String>>(emptySet())
    val contactIds = _contactIds.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var allUsersCache = emptyList<User>()
    private var rawConversationsCache = emptyList<Conversation>()

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
        
        // Initial loading state
        _isLoading.value = true

        viewModelScope.launch {
            // Observe conversations
            launch {
                messageRepository.observeConversations(currentUserId)
                    .catch { e ->
                        _conversations.value = emptyList()
                        rawConversationsCache = emptyList()
                        _isLoading.value = false
                        android.util.Log.e("HomeViewModel", "Error loading conversations", e)
                    }
                    .collect { list ->
                        rawConversationsCache = list
                        updateConversationTitles()
                        _isLoading.value = false
                    }
            }

            // Observe current user
            launch {
                userRepository.observeUser(currentUserId).collect { user ->
                    if (user == null) {
                        try {
                            val firebaseUser = authRepository.getCurrentUser()
                            if (firebaseUser != null) {
                                val restoredUser = User(
                                    id = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    username = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                                    status = UserStatus.ONLINE.name,
                                    lastSeen = System.currentTimeMillis()
                                )
                                userRepository.insert(restoredUser)
                                _currentUser.value = restoredUser
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Failed to restore user", e)
                        }
                    } else {
                        _currentUser.value = user
                    }
                }
            }

            // Fetch users for search
            launch {
                userRepository.getAll().collect { allUsers ->
                    allUsersCache = allUsers.filter { it.id != currentUserId }
                    android.util.Log.d("HomeViewModel", "Fetched all users: ${allUsers.size}, filtered: ${allUsersCache.size}")
                    _isLoading.value = false
                    updateConversationTitles()
                    updateContactsList()
                }
            }

            // Observe contacts
            launch {
                userRepository.observeContactIds(currentUserId).collect { ids ->
                    _contactIds.value = ids
                    updateContactsList()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateContactsList()
    }

    private fun updateContactsList() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            _users.value = allUsersCache.filter { user -> user.id in _contactIds.value }
        } else {
            _users.value = allUsersCache.filter {
                it.displayName().contains(query, ignoreCase = true) ||
                    it.username.orEmpty().contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
            }
        }
    }

    fun addContact(userId: String) {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.addContact(currentUserId, userId)
        }
    }

    fun removeContact(userId: String) {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.removeContact(currentUserId, userId)
        }
    }

    fun importDeviceContacts(emails: List<String>, names: List<String>) {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
        val normalizedEmails = emails.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val normalizedNames = names.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        if (normalizedEmails.isEmpty() && normalizedNames.isEmpty()) return

        viewModelScope.launch {
            allUsersCache
                .filter { user ->
                    val userEmail = user.email.trim().lowercase()
                    val userName = user.username.orEmpty().trim().lowercase()
                    userEmail in normalizedEmails || (userName.isNotBlank() && userName in normalizedNames)
                }
                .forEach { match ->
                    userRepository.addContact(currentUserId, match.id)
                }
        }
    }

    private fun updateConversationTitles() {
        val usersById = allUsersCache.associateBy { it.id }
        val usersByUsername = allUsersCache
            .mapNotNull { user -> user.username?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { it to user } }
            .toMap()

        _conversations.value = rawConversationsCache.map { conversation ->
            if (conversation.isGroup) {
                return@map conversation.copy(
                    title = conversation.title.ifBlank { "Grupo" }
                )
            }

            val normalizedOtherKey = conversation.otherUserId.trim().lowercase()
            val resolvedOtherUser = usersById[conversation.otherUserId] ?: usersByUsername[normalizedOtherKey]
            val resolvedOtherId = resolvedOtherUser?.id ?: conversation.otherUserId

            val displayTitle = resolvedOtherUser?.displayName()
                ?: conversation.title
                    .takeIf { it.isNotBlank() && it != conversation.otherUserId }
                ?: displayNameFromParts(username = null, email = "", id = resolvedOtherId)

            conversation.copy(
                otherUserId = resolvedOtherId,
                title = displayTitle
            )
        }
    }

    fun signOut(onComplete: () -> Unit) {
        val currentUserId = authRepository.getCurrentUser()?.uid
        if (currentUserId != null) {
            viewModelScope.launch {
                try {
                    // Timeout after 2 seconds to not block logout
                    withTimeout(2000) {
                        try {
                            userRepository.updateStatus(currentUserId, UserStatus.OFFLINE)
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Failed to update status on logout", e)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore timeout or other errors
                } finally {
                    try {
                        localDb.clearAllData()
                        android.util.Log.d("HomeViewModel", "Cleared local database on sign-out")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error clearing local database", e)
                    }
                    authRepository.signOut()
                    onComplete()
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    localDb.clearAllData()
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error clearing local database on logout", e)
                }
                authRepository.signOut()
                onComplete()
            }
        }
    }
}
