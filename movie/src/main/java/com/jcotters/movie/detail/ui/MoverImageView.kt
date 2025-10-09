package com.jcotters.movie.detail.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MoverImageView(
  posterUrl: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  Box(
    modifier = modifier
      .sharedElement(
        sharedContentState = rememberSharedContentState(key = posterUrl),
        animatedVisibilityScope = animatedVisibilityScope,
      )
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
