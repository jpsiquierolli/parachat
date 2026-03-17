package parachat.ui.feature.signup

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
// import kotlinx.coroutines.tasks.await
import parachat.auth.FirebaseAuthRepository
import parachat.data.firebase.user.FirebaseUserRepository
import parachat.domain.User
import parachat.domain.UserStatus
import parachat.navigation.HomeRoute
import parachat.navigation.LoginRoute
import parachat.ui.UIEvent

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

    private val _uiEvent = Channel<UIEvent>()
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

                authRepository.signUp(email, password)

                val userId = authRepository.getCurrentUser()?.uid ?: return@launch
                saveUserProfile(userId, email, username)

                _uiEvent.send(UIEvent.Navigate(HomeRoute))
            } catch (e: Exception) {
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
            _uiEvent.send(UIEvent.ShowSnackBar(
                message = "No foi possvel salvar o perfil: ${e.message ?: "erro desconhecido"}"
            ))
        }
    }
}
