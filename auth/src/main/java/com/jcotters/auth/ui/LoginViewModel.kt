package com.jcotters.auth.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed interface LoginViewEvent {
  class UsernameUpdated(val username: String) : LoginViewEvent
  class PasswordUpdated(val username: String) : LoginViewEvent
  object LoginTapped : LoginViewEvent
}

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

@HiltViewModel
class LoginViewModel @Inject constructor(

) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(LoginViewState())
  val uiState: StateFlow<LoginViewState> = viewModelUiState

  fun onViewEvent(event: LoginViewEvent) {
    when (event) {
      is LoginViewEvent.UsernameUpdated -> onUsernameUpdated(event)
      is LoginViewEvent.PasswordUpdated -> onPasswordUpdated(event)
      LoginViewEvent.LoginTapped -> onLoginTapped()
    }
  }

  private fun onUsernameUpdated(event: LoginViewEvent.UsernameUpdated) {
    clearErrorMessage()
    viewModelUiState.value = viewModelUiState.value.copy(
      username = event.username
    )
  }

  private fun onPasswordUpdated(event: LoginViewEvent.PasswordUpdated) {
    clearErrorMessage()
    viewModelUiState.value = viewModelUiState.value.copy(
      password = event.username
    )
  }

  private fun onLoginTapped() {
    clearErrorMessage()
    val username = uiState.value.username
    val password = uiState.value.password
    if (username.equals("username", ignoreCase = false) && password.equals("password", ignoreCase = false)) {
      viewModelUiState.value = viewModelUiState.value.copy(successfulLogin = true)
    } else {
      setErrorMessage("Incorrect login details.")
    }
  }

  private fun clearErrorMessage() {
    setErrorMessage("")
  }

  private fun setErrorMessage(message: String) {
    viewModelUiState.value = viewModelUiState.value.copy(
      errorMessage = message
    )
  }
}