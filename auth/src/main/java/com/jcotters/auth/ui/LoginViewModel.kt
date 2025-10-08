package com.jcotters.auth.ui

import androidx.lifecycle.ViewModel
import com.jcotters.auth.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val loginUseCase: LoginUseCase,
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
    loginUseCase.invoke(uiState.value.username, uiState.value.password)
      .onSuccess {
        viewModelUiState.value = viewModelUiState.value.copy(successfulLogin = true)
      }
      .onFailure { error ->
        setErrorMessage(error.message ?: "Failed to login.")
      }
  }

  private fun clearErrorMessage() = setErrorMessage("")

  private fun setErrorMessage(message: String) {
    viewModelUiState.value = viewModelUiState.value.copy(
      errorMessage = message
    )
  }
}