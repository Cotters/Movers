package com.jcotters.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SignUpScreen(
  modifier: Modifier = Modifier,
  viewState: AuthViewState,
  onViewEvent: (AuthViewEvent) -> Unit,
) {
  Surface {
    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(48.dp))
      Text(
        text = "Sign Up",
        style = MaterialTheme.typography.headlineLarge,
      )
      TextField(
        value = viewState.username,
        onValueChange = { onViewEvent(AuthViewEvent.UsernameUpdated(it)) }
      )
      TextField(
        value = viewState.password,
        onValueChange = { onViewEvent(AuthViewEvent.PasswordUpdated(it)) },
        visualTransformation = PasswordVisualTransformation(),
      )
      TextField(
        value = viewState.confirmPassword,
        onValueChange = { onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(it)) },
        visualTransformation = PasswordVisualTransformation(),
      )
      Button(
        onClick = { onViewEvent(AuthViewEvent.LoginTapped) },
        enabled = viewState.signUpButtonEnabled,
      ) {
        Text(text = "Sign Up")
      }
      TextButton(onClick = { onViewEvent(AuthViewEvent.HaveExistingAccountTapped) }) {
        Text(text = "Already have an Account?")
      }
      AuthErrorView(errorMessage = viewState.errorMessage)
    }
  }
}

@Composable
fun AuthErrorView(errorMessage: String) {
  if (errorMessage.isNotBlank()) {
    Box(
      modifier = Modifier
        .background(Color.Red.copy(alpha = 0.6f))
        .padding(8.dp)
    ) {
      Text(text = errorMessage)
    }
  }
}
