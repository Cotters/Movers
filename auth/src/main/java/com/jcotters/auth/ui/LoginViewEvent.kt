package com.jcotters.auth.ui

sealed interface LoginViewEvent {
  class UsernameUpdated(val username: String) : LoginViewEvent
  class PasswordUpdated(val username: String) : LoginViewEvent
  object LoginTapped : LoginViewEvent
}