package parachat.ui.feature.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import parachat.data.SupabaseProvider
import parachat.data.supabase.user.UserRepositoryImpl
import parachat.domain.User

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepositoryImpl(SupabaseProvider.client)

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            userRepository.getAll().collect { allUsers ->
                // Filter out current user from the list
                _users.value = allUsers.filter { it.id != currentUserId }
                _currentUser.value = allUsers.find { it.id == currentUserId }
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

