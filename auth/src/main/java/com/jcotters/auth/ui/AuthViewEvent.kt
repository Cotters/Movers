package com.jcotters.auth.ui

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext

sealed interface AuthViewEvent {
    data class OnLoad(@param:ActivityContext val context: Context) : AuthViewEvent
    data class UsernameUpdated(val username: String) : AuthViewEvent
    data class PasswordUpdated(val password: String) : AuthViewEvent
    data class ConfirmPasswordUpdated(val password: String) : AuthViewEvent
    data object CreateAccountTapped : AuthViewEvent
    data object HaveExistingAccountTapped : AuthViewEvent
    data object SignUpTapped : AuthViewEvent
    data object LoginTapped : AuthViewEvent
}