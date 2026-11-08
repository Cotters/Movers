package com.jcotters.auth.ui

sealed interface AuthViewEvent {
    data class CredentialsReceived(val username: String, val password: String) : AuthViewEvent
    data object CredentialRetrievalFailed : AuthViewEvent
    data class UsernameUpdated(val username: String) : AuthViewEvent
    data class PasswordUpdated(val password: String) : AuthViewEvent
    data class ConfirmPasswordUpdated(val password: String) : AuthViewEvent
    data object CreateAccountTapped : AuthViewEvent
    data object HaveExistingAccountTapped : AuthViewEvent
    data object SignUpTapped : AuthViewEvent
    data object LoginTapped : AuthViewEvent
}
