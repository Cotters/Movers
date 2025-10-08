package com.jcotters.auth.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.LoginUseCase
import com.jcotters.auth.domain.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
  private val loginUseCase: LoginUseCase,
  private val signUpUseCase: SignUpUseCase,
) : ViewModel() {

  companion object {
    const val MISMATCHING_PASSWORDS_MESSAGE = "Passwords do not match."
  }

  private val viewModelUiState = MutableStateFlow(AuthViewState())
  val uiState: StateFlow<AuthViewState> = viewModelUiState

  private fun launchCredentialManagerFlow(context: Context) {
    val credentialManager = CredentialManager.create(context)
    // Retrieves the user's saved password for your app from their password provider.
    val getPasswordOption = GetPasswordOption()

    val credentialRequest = GetCredentialRequest(listOf(getPasswordOption))
    viewModelScope.launch {
      try {
        val result = credentialManager.getCredential(
          context = context,
          request = credentialRequest
        )
        handleSignIn(result)
      } catch (_: GetCredentialException) {
        // This is fine; no credentials available.
        clearErrorMessage()
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Failed to use Credential Manager.")
      }
    }
  }

  private fun handleSignIn(result: GetCredentialResponse) {
    val credential = result.credential
    when (credential) {
      is PasswordCredential -> {
        val username: String = credential.id
        val password: String = credential.password
        onUsernameUpdated(username)
        onPasswordUpdated(password)
        onLoginTapped()
      }
    }
  }

  fun onViewEvent(event: AuthViewEvent) {
    when (event) {
      is AuthViewEvent.OnLoad -> {
        launchCredentialManagerFlow(event.context)
      }

      is AuthViewEvent.UsernameUpdated -> onUsernameUpdated(event.username)
      is AuthViewEvent.PasswordUpdated -> onPasswordUpdated(event.password)
      is AuthViewEvent.ConfirmPasswordUpdated -> onConfirmPasswordUpdated(event.password)
      AuthViewEvent.LoginTapped -> onLoginTapped()
      AuthViewEvent.SignUpTapped -> onSignUpTapped()
      AuthViewEvent.CreateAccountTapped -> {
        viewModelUiState.update { it.copy(authMode = AuthMode.SignUp) }
        clearErrorMessage()
      }

      AuthViewEvent.HaveExistingAccountTapped -> {
        viewModelUiState.update { it.copy(authMode = AuthMode.Login) }
        clearErrorMessage()
      }
    }
  }

  private fun onUsernameUpdated(username: String) {
    clearErrorMessage()
    viewModelUiState.update { current ->
      current.copy(username = username)
    }
  }

  private fun onPasswordUpdated(password: String) {
    clearErrorMessage()
    viewModelUiState.update { current ->
      current.copy(password = password)
    }
  }

  private fun onConfirmPasswordUpdated(password: String) {
    clearErrorMessage()
    viewModelUiState.update { current ->
      current.copy(confirmPassword = password)
    }
  }

  private fun onLoginTapped() {
    clearErrorMessage()
    viewModelScope.launch {
      loginUseCase.invoke(uiState.value.username, uiState.value.password)
        .onSuccess {
          viewModelUiState.update { current -> current.copy(successfulLogin = true) }
        }
        .onFailure { error ->
          setErrorMessage(error.message ?: "Failed to login.")
        }
    }
  }

  private fun onSignUpTapped() {
    if (uiState.value.password.equals(uiState.value.confirmPassword, ignoreCase = false)) {
      clearErrorMessage()
      signUp()
    } else {
      setErrorMessage(MISMATCHING_PASSWORDS_MESSAGE)
    }
  }

  private fun signUp() {
    viewModelScope.launch {
      signUpUseCase.invoke(uiState.value.username, uiState.value.password)
        .onSuccess {
          viewModelUiState.update { current -> current.copy(successfulLogin = true) }
        }
        .onFailure { error ->
          setErrorMessage(error.message ?: "Failed to sign up.")
        }
    }
  }

  private fun clearErrorMessage() = setErrorMessage("")

  private fun setErrorMessage(message: String) {
    viewModelUiState.update { current ->
      current.copy(errorMessage = message)
    }
  }
}