package com.jcotters.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
  modifier: Modifier,
  viewState: AuthViewState,
  onViewEvent: (AuthViewEvent) -> Unit,
) {
  Surface {
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(30.dp))
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Login",
          style = MaterialTheme.typography.headlineLarge,
        )
        TextField(
          value = viewState.username,
          onValueChange = { onViewEvent(AuthViewEvent.UsernameUpdated(it)) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Username") },
        )
        PasswordField(
          password = viewState.password,
          onPasswordChanged = { onViewEvent(AuthViewEvent.PasswordUpdated(it)) }
        )
      }
      Button(
        onClick = { onViewEvent(AuthViewEvent.LoginTapped) },
        modifier = Modifier.fillMaxWidth(),
        enabled = viewState.loginButtonEnabled,
        shape = RectangleShape,
      ) {
        Text(text = "Login")
      }
      TextButton(onClick = { onViewEvent(AuthViewEvent.CreateAccountTapped) }) {
        Text(text = "Create an Account")
      }
      AuthErrorView(errorMessage = viewState.errorMessage)
    }
  }
}

@Preview
@Composable
private fun LoginScreenPreview() {
  Surface {
    LoginScreen(
      modifier = Modifier,
      viewState = AuthViewState(
        errorMessage = "Preview error message!"
      ),
      onViewEvent = { _ -> }
    )
  }
}