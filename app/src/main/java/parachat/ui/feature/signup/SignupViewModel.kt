package parachat.ui.feature.signup

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import parachat.data.supabase.user.UserRepositoryImpl
import parachat.data.SupabaseProvider
import parachat.domain.User
import parachat.navigation.HomeRoute
import parachat.navigation.LoginRoute
import parachat.ui.UIEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupViewModel : ViewModel() {

    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

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

                auth.createUserWithEmailAndPassword(email, password).await()

                val userId = auth.currentUser?.uid ?: return@launch
                saveUserToSupabase(userId, email, username)

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

    private suspend fun saveUserToSupabase(userId: String, email: String, username: String) {
        try {
            val userRepo = UserRepositoryImpl(SupabaseProvider.client)

            val user = User(
                id = userId,
                email = email,
                username = username
            )

            userRepo.insert(user)
            Log.d("SignupViewModel", "User saved to Supabase: $username")
        } catch (e: Exception) {
            Log.e("SignupViewModel", "Error saving user to Supabase", e)
        }
    }
}

