package com.jcotters.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
