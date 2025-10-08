package com.jcotters.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SignUpScreen(
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
          text = "Sign Up",
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

        PasswordField(
          password = viewState.confirmPassword,
          onPasswordChanged = { onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(it)) },
          label = "Confirm Password",
        )
      }
      Button(
        onClick = { onViewEvent(AuthViewEvent.SignUpTapped) },
        enabled = viewState.signUpButtonEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
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
        .clip(RoundedCornerShape(8))
        .background(Color.Red.copy(alpha = 0.7f))
        .padding(10.dp)
    ) {
      Text(
        text = errorMessage,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
      )
    }
  }
}

@Preview
@Composable
private fun SignUpScreenPreview() {
  Surface {
    SignUpScreen(
      modifier = Modifier,
      viewState = AuthViewState(
        errorMessage = "Preview error message!"
      ),
      onViewEvent = { _ -> }
    )
  }
}
