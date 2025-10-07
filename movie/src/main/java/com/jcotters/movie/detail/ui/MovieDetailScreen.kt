package com.jcotters.movie.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jcotters.movie.R
import com.jcotters.movie.detail.domain.models.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
  onViewEvent: (MovieDetailViewEvent) -> Unit,
  viewState: MovieDetailViewState,
  modifier: Modifier = Modifier,
) {
  val horizontalPadding = 8.dp
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
          MoviePostImageView(
            posterUrl = movie.backdropUrl.orEmpty(),
            contentDescription = "Movie backdrop image for ${movie.title}",
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f),
          )

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

          Row(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth()
              .padding(horizontal = horizontalPadding, vertical = 8.dp)
              .graphicsLayer {
                alpha = 0.95f
              },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = movie.title,
              modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
              style = MaterialTheme.typography.titleLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
              ),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Box(
              modifier = Modifier
                .padding(2.dp)
                .clip(CircleShape)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f))
            ) {
              IconButton(onClick = { onViewEvent(MovieDetailViewEvent.BookmarkTapped(movie.id)) }) {
                Icon(
                  painter = painterResource(R.drawable.bookmark_outline),
                  contentDescription = "Bookmark film button",
                  modifier = Modifier.size(25.dp),
                  tint = Color.White,
                )
              }
            }
          }
        }

        Column(
          modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
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
