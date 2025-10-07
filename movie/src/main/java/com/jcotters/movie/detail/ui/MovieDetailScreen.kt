package com.jcotters.movie.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.detail.domain.models.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
  onViewEvent: (MovieDetailViewEvent) -> Unit,
  viewState: MovieDetailViewState,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(Unit) {
    onViewEvent(MovieDetailViewEvent.OnLoad(11))
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(title = { Text("Movie Details") })
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .padding(innerPadding)
        .padding(horizontal = 12.dp)
    ) {
      if (viewState.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
          CircularProgressIndicator()
        }
      } else if (viewState.movie != null) {
        val movie = viewState.movie
        Text(text = movie.title, style = MaterialTheme.typography.titleLarge)
        Text(text = movie.synopsis, style = MaterialTheme.typography.bodyMedium)
        Text(text = "Released ${movie.releaseDate}", style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Preview
@Composable
private fun MovieDetailScreenPreview() {
  MovieDetailScreen(
    onViewEvent = { _ -> },
    viewState = MovieDetailViewState(
      isLoading = false,
      movie = Movie(id = 1, title = "Preview: The Movie", synopsis = "A preview to die for...", releaseDate = "2025/10/07")
    )
  )
}
