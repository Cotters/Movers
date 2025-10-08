package com.jcotters.auth.ui

enum class AuthMode {
  Login, SignUp
}

data class AuthViewState(
  val authMode: AuthMode = AuthMode.Login,
  val username: String = "",
  val password: String = "",
  val confirmPassword: String = "",
  val successfulLogin: Boolean = false,
  val errorMessage: String = "",
) {
  val loginButtonEnabled: Boolean
    get() = username.isNotEmpty() && password.isNotEmpty()

  val signUpButtonEnabled: Boolean
    get() = username.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
}
