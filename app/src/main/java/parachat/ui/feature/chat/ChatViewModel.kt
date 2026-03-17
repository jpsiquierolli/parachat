package parachat.ui.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import parachat.data.SupabaseProvider
import parachat.data.supabase.chat.MessageRepositoryImpl
import parachat.domain.chat.Message
import parachat.navigation.ChatRoute

class ChatViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val messageRepository = MessageRepositoryImpl(SupabaseProvider.client)

    private val args = savedStateHandle.toRoute<ChatRoute>()
    val chatUserId = args.userId
    val currentUserId = auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        if (currentUserId.isBlank()) return
        
        viewModelScope.launch {
            messageRepository.getMessages(currentUserId, chatUserId).collect {
                _messages.value = it.sortedBy { message -> message.timestamp }
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
            receiverId = chatUserId,
            content = text,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            messageRepository.sendMessage(message)
            _messageText.value = ""
        }
    }
}

