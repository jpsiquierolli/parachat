package com.example.parachat.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.SupabaseProvider
import com.example.parachat.data.firebase.chat.FirebaseMessageRepository
import com.example.parachat.data.supabase.storage.MediaStorageRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageRepository
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.navigation.ChatRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ChatRoute>()
    val chatUserId = args.userId
    val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser = _otherUser.asStateFlow()

    private val _pinnedMessage = MutableStateFlow<Message?>(null)
    val pinnedMessage = _pinnedMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val storageRepository = MediaStorageRepository(SupabaseProvider.client)

    private var allMessagesCache = emptyList<Message>()

    init {
        loadData()
        markAsRead()
    }

    private fun loadData() {
        if (currentUserId.isBlank()) return
        
        viewModelScope.launch {
            // Observe other user
            launch {
                userRepository.observeUser(chatUserId).collect {
                    _otherUser.value = it
                }
            }

            // Observe messages
            launch {
                messageRepository.getMessages(currentUserId, chatUserId).collect {
                    allMessagesCache = it.sortedBy { message -> message.timestamp }
                    onSearchQueryChange(_searchQuery.value)
                }
            }

            // Observe pinned
            launch {
                messageRepository.observePinnedMessage(currentUserId, chatUserId).collect {
                    _pinnedMessage.value = it
                }
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(currentUserId, chatUserId)
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
            receiverId = chatUserId,
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
                    receiverId = chatUserId,
                    content = "",
                    mediaUrl = url,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
                messageRepository.sendMessage(message)
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
            messageRepository.pinMessage(currentUserId, chatUserId, message)
        }
    }

    fun unpinMessage() {
        viewModelScope.launch {
            messageRepository.unpinMessage(currentUserId, chatUserId)
        }
    }
}
