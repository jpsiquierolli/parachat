package com.example.parachat.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.SupabaseProvider
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.data.supabase.storage.MediaStorageRepository
import com.example.parachat.domain.User

class ProfileViewModel : ViewModel() {

    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())
    private val storageRepository = MediaStorageRepository(SupabaseProvider.client, "user-media")

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collect {
                _currentUser.value = it
            }
        }
    }

    fun updateProfile(username: String, about: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Since updateStatus updates "status" and "lastSeen", we need a full update method or similar
                // But FirebaseUserRepository only has insert (overwrite) or updateStatus.
                // We'll use insert to overwrite fields, but preserve others.
                val updatedUser = user.copy(username = username, about = about)
                userRepository.insert(updatedUser)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfilePicture(bytes: ByteArray, extension: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val url = storageRepository.uploadBytes(
                    ownerId = user.id,
                    bytes = bytes,
                    extension = extension,
                    mimeType = "image/$extension"
                )
                val updatedUser = user.copy(photoUrl = url)
                userRepository.insert(updatedUser)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

