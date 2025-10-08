package com.jcotters.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
  viewState: ProfileViewState,
  onViewEvent: (ProfileViewEvent) -> Unit,
  userSession: String = "",
) {
  Scaffold { innerPadding ->
    Column(
      modifier = Modifier.padding(innerPadding)
    ) {
      Text(text = "Session: $userSession")

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = { onViewEvent(ProfileViewEvent.LogoutTapped) },
        modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
        shape = RoundedCornerShape(8.dp),
      ) {
        Text("Logout")
      }
    }
  }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
  ProfileScreen(
    viewState = ProfileViewState(),
    onViewEvent = {},
  )
}