package com.jcotters.auth.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException

@Composable
internal fun AuthRoute() {
    val viewModel: AuthViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel, lifecycle, context) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.claimCredentialRequest()) {
                try {
                    val response = CredentialManager.create(context).getCredential(
                        context,
                        GetCredentialRequest(listOf(GetPasswordOption())),
                    )
                    val credential = response.credential as? PasswordCredential
                    if (credential != null) {
                        viewModel.onViewEvent(
                            AuthViewEvent.CredentialsReceived(credential.id, credential.password),
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: GetCredentialException) {
                    // Dismissal and missing saved credentials leave manual sign-in available.
                } catch (_: Exception) {
                    viewModel.onViewEvent(AuthViewEvent.CredentialRetrievalFailed)
                }
            }
        }
    }

    Scaffold { padding ->
        val modifier = Modifier.padding(padding)
            .consumeWindowInsets(padding)
            .verticalScroll(rememberScrollState())
        when (state.authMode) {
            AuthMode.Login -> LoginScreen(state, viewModel::onViewEvent, modifier)
            AuthMode.SignUp -> SignUpScreen(modifier, state, viewModel::onViewEvent)
        }
    }
}
