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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
  modifier: Modifier = Modifier,
  viewState: LoginViewState,
  onViewEvent: (LoginViewEvent) -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(48.dp))
    Text(
      text = "Login",
      style = MaterialTheme.typography.headlineLarge,
    )
    TextField(
      value = viewState.username,
      onValueChange = { onViewEvent(LoginViewEvent.UsernameUpdated(it)) }
    )
    TextField(
      value = viewState.password,
      onValueChange = { onViewEvent(LoginViewEvent.PasswordUpdated(it)) },
      visualTransformation = PasswordVisualTransformation(),
    )
    Button(
      onClick = { onViewEvent(LoginViewEvent.LoginTapped) },
      enabled = viewState.loginButtonEnabled,
    ) {
      Text(text = "Login")
    }
    if (viewState.errorMessage.isNotBlank()) {
      Box(
        modifier = Modifier
          .background(Color.Red.copy(alpha = 0.6f))
          .padding(8.dp)
      ) {
        Text(text = viewState.errorMessage)

      }

    }
    Box(
      modifier = Modifier
        .background(Color.Yellow.copy(alpha = 0.1f))
        .padding(16.dp)
    ) {
      Text(text = "Use 'username' and 'password' to login")

    }
  }

}

@Preview
@Composable
private fun LoginScreenPreview() {
  Surface {
    LoginScreen(
      viewState = LoginViewState(
        errorMessage = "Preview error message!"
      ),
      onViewEvent = { _ -> }
    )
  }
}