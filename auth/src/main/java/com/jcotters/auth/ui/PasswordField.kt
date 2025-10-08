package com.jcotters.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jcotters.auth.R

@Composable
fun PasswordField(
  password: String,
  onPasswordChanged: (String) -> Unit,
  label: String = "Password",
) {
  var showPassword by remember { mutableStateOf(false) }
  val passwordTrailingIcon = if (showPassword) R.drawable.eye_open else R.drawable.eye_closed
  val passwordTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
  TextField(
    value = password,
    onValueChange = onPasswordChanged,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    visualTransformation = passwordTransformation,
    trailingIcon = {
      IconButton(onClick = { showPassword = showPassword.not() }) {
        Icon(
          painter = painterResource(passwordTrailingIcon),
          contentDescription = "Account top bar button",
          modifier = Modifier.size(25.dp),
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }

    }
  )
}
