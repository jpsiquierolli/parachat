package com.example.parachat.ui.feature.login

sealed interface LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data object Login : LoginEvent
    data object NavigateToSignup : LoginEvent
    data object ForgotPassword : LoginEvent
}