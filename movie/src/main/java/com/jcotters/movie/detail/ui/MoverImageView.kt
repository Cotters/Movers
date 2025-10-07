package com.jcotters.movie.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun MoverImageView(
  posterUrl: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(Color.LightGray.copy(alpha = 0.3f))
  ) {
    AsyncImage(
      model = posterUrl,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier.matchParentSize()
    )
  }
}
