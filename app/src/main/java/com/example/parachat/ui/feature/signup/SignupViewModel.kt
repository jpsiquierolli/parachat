package com.example.parachat.ui.feature.signup

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
// import kotlinx.coroutines.tasks.await
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.domain.User
import com.example.parachat.domain.UserStatus
import com.example.parachat.navigation.HomeRoute
import com.example.parachat.navigation.LoginRoute
import com.example.parachat.ui.UIEvent

class SignupViewModel : ViewModel() {

    private val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
    private val userRepository = FirebaseUserRepository(FirebaseDatabase.getInstance())

    var username by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loading by mutableStateOf(false)
        private set

    private val _uiEvent = Channel<UIEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: SignupEvent) {
        when (event) {
            is SignupEvent.UsernameChanged -> {
                username = event.username
            }
            is SignupEvent.EmailChanged -> {
                email = event.email
            }
            is SignupEvent.PasswordChanged -> {
                password = event.password
            }

            SignupEvent.Signup -> {
                signup()
            }
            SignupEvent.NavigateToLogin -> {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        viewModelScope.launch {
            _uiEvent.send(UIEvent.Navigate(LoginRoute))
        }
    }

    private fun signup () {
        loading = true
        viewModelScope.launch {
            try {
                if (username.isBlank()) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "O nome de usuário não pode estar vazio"
                    ))
                    return@launch
                }

                if (email.isBlank()) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "O email não pode estar vazio"
                    ))
                    return@launch
                }

                if (password.isBlank()) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "A senha não pode estar vazia"
                    ))
                    return@launch
                }

                val result = authRepository.signUp(email, password)
                val userId = result.user?.uid

                if (userId == null) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "Erro: Usuário criado, mas ID não encontrado."
                    ))
                    return@launch
                }

                // Save profile asynchronously (with timeout) so navigation isn't blocked
                launch {
                    try {
                        withTimeout(15_000L) {
                            saveUserProfile(userId, email, username)
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.w("SignupViewModel", "Profile save timed out", e)
                        _uiEvent.send(UIEvent.ShowSnackBar(
                            message = "Aviso: O perfil está lento para salvar. Verifique sua conexão."
                        ))
                    } catch (e: Exception) {
                        Log.e("SignupViewModel", "Profile save failed", e)
                        _uiEvent.send(UIEvent.ShowSnackBar(
                            message = "Aviso: Não foi possível salvar o perfil. O cadastro foi concluído."
                        ))
                    }
                }

                _uiEvent.send(UIEvent.Navigate(HomeRoute))
            } catch (e: Exception) {
                Log.e("SignupViewModel", "Signup failed", e)
                _uiEvent.send(UIEvent.ShowSnackBar(
                    message = e.message ?: "Algo deu errado, tente novamente."
                ))
            } finally {
                loading = false
            }
        }
    }

    private suspend fun saveUserProfile(userId: String, email: String, username: String) {
        try {
            val user = User(
                id = userId,
                email = email,
                username = username,
                status = UserStatus.ONLINE.name,
                lastSeen = System.currentTimeMillis()
            )
            userRepository.insert(user)
            Log.d("SignupViewModel", "User saved to Firebase: $username")
        } catch (e: Exception) {
            Log.e("SignupViewModel", "Error saving user profile", e)
            throw e // Re-throw to be caught by the caller or timeout
        }
    }
}
