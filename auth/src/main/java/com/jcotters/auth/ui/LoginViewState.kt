package com.jcotters.auth.ui

data class LoginViewState(
  val username: String = "",
  val password: String = "",
  val isLoggingIn: Boolean = false,
  val errorMessage: String = "",
  val successfulLogin: Boolean = false,
) {
  val loginButtonEnabled: Boolean
    get() = username.isNotEmpty() && password.isNotEmpty()
}