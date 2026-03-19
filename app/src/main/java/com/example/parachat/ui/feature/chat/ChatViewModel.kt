package com.example.parachat.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.SupabaseProvider
import com.example.parachat.data.firebase.chat.FirebaseMessageRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.data.supabase.storage.MediaStorageRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.navigation.ChatRoute
import com.example.parachat.data.room.ParachatDatabase
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ChatViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val database = ParachatDatabase.getInstance(application)
    private val messageRepository = FirebaseMessageRepository(FirebaseDatabase.getInstance(), database.messageDao)
    private val storageRepository = MediaStorageRepository(SupabaseProvider.client)
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())

    private val args = savedStateHandle.toRoute<ChatRoute>()
    val chatUserId = args.userId
    val chatGroupId = args.groupId
    val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _pinnedMessage = MutableStateFlow<Message?>(null)
    val pinnedMessage = _pinnedMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Map of userId -> User for resolving sender names/avatars in groups
    private val _senderProfiles = MutableStateFlow<Map<String, User>>(emptyMap())
    val senderProfiles = _senderProfiles.asStateFlow()

    private var allMessagesCache = emptyList<Message>()

    init {
        loadMessages()
        observePinnedMessage()
        markAsRead()
    }

    private fun markAsRead() {
        viewModelScope.launch {
            if (chatUserId != null) {
                messageRepository.markConversationAsRead(currentUserId, chatUserId)
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            if (chatGroupId != null) {
                messageRepository.getGroupMessages(chatGroupId).collect { msgs ->
                    allMessagesCache = msgs
                    updateFilteredMessages()
                    // Fetch profiles for any new senders we haven't seen yet
                    fetchMissingSenderProfiles(msgs.map { it.senderId }.toSet())
                }
            } else if (chatUserId != null) {
                messageRepository.getMessages(currentUserId, chatUserId).collect { msgs ->
                    allMessagesCache = msgs
                    updateFilteredMessages()
                }
            }
        }
    }

    private fun fetchMissingSenderProfiles(senderIds: Set<String>) {
        viewModelScope.launch {
            val current = _senderProfiles.value
            val missing = senderIds.filter { it !in current }
            if (missing.isEmpty()) return@launch
            val newProfiles = current.toMutableMap()
            for (id in missing) {
                try {
                    userRepository.observeUser(id).collect { user ->
                        if (user != null) {
                            newProfiles[id] = user
                            _senderProfiles.value = newProfiles.toMap()
                        }
                        return@collect
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun observePinnedMessage() {
        viewModelScope.launch {
            if (chatGroupId != null) {
                messageRepository.observePinnedGroupMessage(chatGroupId).collect {
                    _pinnedMessage.value = it
                }
            } else if (chatUserId != null) {
                messageRepository.observePinnedMessage(currentUserId, chatUserId).collect {
                    _pinnedMessage.value = it
                }
            }
        }
    }

    fun onMessageChange(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value
        if (text.isBlank()) return

        val message = Message(
            senderId = currentUserId,
            receiverId = chatUserId ?: "",
            groupId = chatGroupId,
            content = text,
            type = MessageType.TEXT,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            messageRepository.sendMessage(message)
            _messageText.value = ""
        }
    }

    fun sendLocationMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.sendMessage(message)
        }
    }

    fun sendMedia(bytes: ByteArray, extension: String, mimeType: String, type: MessageType) {
        viewModelScope.launch {
            try {
                val url = storageRepository.uploadBytes(
                    ownerId = currentUserId,
                    bytes = bytes,
                    extension = extension,
                    mimeType = mimeType
                )

                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUserId ?: "",
                    groupId = chatGroupId,
                    content = "",
                    mediaUrl = url,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
                messageRepository.sendMessage(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateFilteredMessages()
    }

    private fun updateFilteredMessages() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _messages.value = allMessagesCache
        } else {
            _messages.value = allMessagesCache.filter {
                it.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun pinMessage(message: Message) {
        viewModelScope.launch {
            if (chatGroupId != null) {
                messageRepository.pinGroupMessage(chatGroupId, message)
            } else if (chatUserId != null) {
                messageRepository.pinMessage(currentUserId, chatUserId, message)
            }
        }
    }

    fun unpinMessage() {
        viewModelScope.launch {
            if (chatGroupId != null) {
                messageRepository.unpinGroupMessage(chatGroupId)
            } else if (chatUserId != null) {
                messageRepository.unpinMessage(currentUserId, chatUserId)
            }
        }
    }
}
