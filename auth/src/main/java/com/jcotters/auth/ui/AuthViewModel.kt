package com.jcotters.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.LoginUseCase
import com.jcotters.auth.domain.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val userRepository: IUserRepository,
) : ViewModel() {

    companion object {
        const val MISMATCHING_PASSWORDS_MESSAGE = "Passwords do not match."
        const val SIGN_IN_FAILED_MESSAGE = "Failed to sign in. Try again later."
        const val SIGN_UP_FAILED_MESSAGE = "Failed to sign up. Try again later."
        const val CREDENTIAL_MANAGER_FAILED_MESSAGE = "Failed to use Credential Manager."
    }

    private val viewModelUiState = MutableStateFlow(AuthViewState())
    val uiState: StateFlow<AuthViewState> = viewModelUiState

    fun onViewEvent(event: AuthViewEvent) {
        when (event) {
            is AuthViewEvent.CredentialsReceived -> {
                onUsernameUpdated(event.username)
                onPasswordUpdated(event.password)
                onLoginTapped()
            }
            AuthViewEvent.CredentialRetrievalFailed -> setErrorMessage(CREDENTIAL_MANAGER_FAILED_MESSAGE)
            is AuthViewEvent.UsernameUpdated -> onUsernameUpdated(event.username)
            is AuthViewEvent.PasswordUpdated -> onPasswordUpdated(event.password)
            is AuthViewEvent.ConfirmPasswordUpdated -> onConfirmPasswordUpdated(event.password)
            AuthViewEvent.LoginTapped -> onLoginTapped()
            AuthViewEvent.SignUpTapped -> onSignUpTapped()
            AuthViewEvent.CreateAccountTapped -> {
                viewModelUiState.update { it.copy(authMode = AuthMode.SignUp) }
                clearErrorMessage()
                fetchUserCount()
            }

            AuthViewEvent.HaveExistingAccountTapped -> {
                viewModelUiState.update { it.copy(authMode = AuthMode.Login) }
                clearErrorMessage()
            }
        }
    }

    private var credentialsRequested: Boolean = false

    fun claimCredentialRequest(): Boolean {
        if (credentialsRequested) return false
        credentialsRequested = true
        return true
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
                    viewModelUiState.update { current -> current.copy(successfulLogin = true, password = "", confirmPassword = "") }
                }
                .onFailure { error ->
                    setErrorMessage(error.message ?: SIGN_IN_FAILED_MESSAGE)
                }
        }
    }

    private fun onSignUpTapped() {
        if (enteredPasswordsMatch) {
            clearErrorMessage()
            signUp()
        } else {
            setErrorMessage(MISMATCHING_PASSWORDS_MESSAGE)
        }
    }

    private val enteredPasswordsMatch: Boolean
        get() = uiState.value.password.equals(uiState.value.confirmPassword, ignoreCase = false)

    private fun signUp() {
        viewModelScope.launch {
            signUpUseCase.invoke(uiState.value.username, uiState.value.password)
                .onSuccess {
                    viewModelUiState.update { current -> current.copy(successfulLogin = true, password = "", confirmPassword = "") }
                }
                .onFailure { error ->
                    setErrorMessage(error.message ?: SIGN_UP_FAILED_MESSAGE)
                }
        }
    }

    private fun clearErrorMessage() = setErrorMessage("")

    private fun setErrorMessage(message: String) {
        viewModelUiState.update { current ->
            current.copy(errorMessage = message)
        }
    }

    private fun fetchUserCount() {
        viewModelScope.launch {
            try {
                val count = userRepository.getUserCount()
                viewModelUiState.update { current ->
                    current.copy(userCount = count)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                viewModelUiState.update { current ->
                    current.copy(userCount = 0)
                }
            }
        }
    }
}
