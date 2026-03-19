package com.example.parachat.ui.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.navigation.HomeRoute
import com.example.parachat.navigation.SignupRoute
import com.example.parachat.ui.UIEvent
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loading by mutableStateOf(false)
        private set


    private val _uiEvent = Channel<UIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus () {
        if (authRepository.getCurrentUser() != null) {
            viewModelScope.launch {
                _uiEvent.send(UIEvent.Navigate(HomeRoute))
            }
        }
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                email = event.email
            }
            is LoginEvent.PasswordChanged -> {
                password = event.password
            }

            LoginEvent.Login -> {
                login()
            }
            LoginEvent.NavigateToSignup -> {
                navigateToSignup()
            }
            LoginEvent.ForgotPassword -> {
                resetPassword()
            }
        }
    }

    private fun resetPassword() {
        if (email.isBlank()) {
            viewModelScope.launch {
                _uiEvent.send(UIEvent.ShowSnackBar("Digite seu email para redefinir a senha"))
            }
            return
        }
        viewModelScope.launch {
            try {
                authRepository.resetPassword(email)
                _uiEvent.send(UIEvent.ShowSnackBar("Email de redefinição enviado!"))
            } catch (e: Exception) {
                _uiEvent.send(UIEvent.ShowSnackBar(e.message ?: "Erro ao enviar email"))
            }
        }
    }

    private fun navigateToSignup() {
        viewModelScope.launch {
            _uiEvent.send(UIEvent.Navigate(SignupRoute))
        }
    }

    private fun login () {
        loading = true
        viewModelScope.launch {
            try {
                if (email.isBlank()) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "The email can't be empty"
                    ))
                    return@launch
                }
                if (password.isBlank()) {
                    _uiEvent.send(UIEvent.ShowSnackBar(
                        message = "The password can't be empty"
                    ))
                    return@launch
                }
                authRepository.signIn(email, password)
                _uiEvent.send(UIEvent.Navigate(HomeRoute))
            } catch (e: Exception) {
                _uiEvent.send(UIEvent.ShowSnackBar(
                    message = e.message ?: "Something went wrong, please try again."
                ))
            } finally {
                loading = false
            }
        }
    }
}
