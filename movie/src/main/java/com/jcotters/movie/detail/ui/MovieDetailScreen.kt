package com.jcotters.movie.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jcotters.movie.detail.domain.models.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
  onViewEvent: (MovieDetailViewEvent) -> Unit,
  viewState: MovieDetailViewState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxSize(),
  ) {
    Column {
      if (viewState.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
          )
        }
      } else if (viewState.movie != null) {
        val movie = viewState.movie
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
        ) {
          movie.backdropUrl?.let { url ->
            MoviePostImageView(
              posterUrl = url,
              contentDescription = "Movie backdrop image for ${movie.title}",
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.7f)
                  ),
                  startY = 0f,
                  endY = Float.POSITIVE_INFINITY
                )
              )
          )

          Text(
            text = movie.title,
            style = MaterialTheme.typography.titleLarge.copy(
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth()
              .padding(16.dp)
              .graphicsLayer {
                alpha = 0.95f
              },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Column(
          modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(text = movie.synopsis, style = MaterialTheme.typography.bodyMedium)
          Text(text = "Released ${movie.releaseDate}", style = MaterialTheme.typography.bodyMedium)

        }
      }
    }
  }
}

@Composable
fun MoviePostImageView(
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

@Preview
@Composable
private fun MovieDetailScreenPreview() {
  MovieDetailScreen(
    onViewEvent = { _ -> },
    viewState = MovieDetailViewState(
      isLoading = false,
      movie = Movie(
        id = 1,
        title = "Preview: The Movie",
        synopsis = "A preview to die for...",
        releaseDate = "2025/10/07",
        posterUrl = "https://api.themoviedb.org/3/ovZ0zq0NwRghtWI1oLaM0lWuoEw.jpg"
      )
    )
  )
}
