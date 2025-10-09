package com.jcotters.movie.detail.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MoverImageView(
  posterUrl: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  MoverImageView(
    posterUrl = posterUrl,
    contentDescription = contentDescription,
    modifier = modifier.sharedElement(
      sharedContentState = rememberSharedContentState(key = posterUrl),
      animatedVisibilityScope = animatedVisibilityScope,
    )
  )
}


@Composable
fun MoverImageView(
  posterUrl: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
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
