package com.jcotters.movie.catalogue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.R
import com.jcotters.movie.detail.domain.models.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieCatalogueScreen(
  isAuthenticated: Boolean,
  movies: List<Movie>,
  onMovieTapped: (movieId: Int) -> Unit,
  onAccountTapped: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accountIcon = if (isAuthenticated) R.drawable.person_filled else R.drawable.person_outline
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Movers") },
        actions = {
          IconButton(onClick = onAccountTapped) {
            Icon(
              painter = painterResource(accountIcon),
              contentDescription = "Account top bar button",
              modifier = Modifier.size(30.dp),
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      )
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier.padding(innerPadding),
    ) {
      if (movies.isEmpty()) {
        item {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
      items(movies, key = { it.id }) { movie ->
        MovieCatalogueListItem(
          movie = movie,
          modifier = Modifier
            .height(150.dp)
            .clickable { onMovieTapped(movie.id) }
        )
        HorizontalDivider()
      }
    }
  }
}

@Preview
@Composable
private fun MovieCatalogueScreenPreview() {
  MovieCatalogueScreen(
    isAuthenticated = false,
    movies = listOf(
      Movie(id = 1, title = "Preview: The Movie", synopsis = "A thrilling preview!", releaseDate = "Today"),
      Movie(id = 2, title = "Preview: Reloaded", synopsis = "A thrilling hot reload!", releaseDate = "Tomorrow"),
      Movie(id = 3, title = "Preview: Recomp", synopsis = "A stunning recomposition!", releaseDate = "Friday"),
    ),
    onMovieTapped = { movieId -> },
    onAccountTapped = {},
  )
}