package com.jcotters.movie.catalogue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.detail.domain.models.Movie
import com.jcotters.movie.detail.ui.MoviePostImageView

@Composable
fun MovieCatalogueScreen(
  movies: List<Movie>,
  modifier: Modifier = Modifier,
  onMovieTapped: (movieId: Int) -> Unit,
) {
  LazyColumn(
    modifier = modifier,
  ) {
    items(movies, key = { it.id }) { movie ->
      Row(
        modifier = Modifier
          .height(150.dp)
          .clickable { onMovieTapped(movie.id) }
      ) {
        movie.posterUrl?.let {
          MoviePostImageView(
            posterUrl = it,
            modifier = Modifier
              .fillMaxHeight()
              .aspectRatio(9f / 16f)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxHeight()
            .padding(all = 8.dp),
          verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
        ) {
          Text(text = movie.title, style = MaterialTheme.typography.titleLarge)
          Text(
            text = movie.synopsis,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      HorizontalDivider()
    }
  }
}

@Preview
@Composable
private fun MovieCatalogueScreenPreview() {
  MovieCatalogueScreen(
    movies = listOf(
      Movie(id = 1, title = "Preview: The Movie", synopsis = "A thrilling preview!", releaseDate = "Today"),
      Movie(id = 2, title = "Preview: Reloaded", synopsis = "A thrilling hot reload!", releaseDate = "Tomorrow"),
      Movie(id = 3, title = "Preview: Recomp", synopsis = "A stunning recomposition!", releaseDate = "Friday"),
    ),
    onMovieTapped = { movieId -> },
  )
}