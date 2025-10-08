package com.jcotters.auth.ui

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext

sealed interface AuthViewEvent {
  class OnLoad(@param:ActivityContext val context: Context): AuthViewEvent
  class UsernameUpdated(val username: String) : AuthViewEvent
  class PasswordUpdated(val password: String) : AuthViewEvent
  class ConfirmPasswordUpdated(val password: String) : AuthViewEvent
  object CreateAccountTapped: AuthViewEvent
  object HaveExistingAccountTapped: AuthViewEvent
  object SignUpTapped: AuthViewEvent
  object LoginTapped : AuthViewEvent
}