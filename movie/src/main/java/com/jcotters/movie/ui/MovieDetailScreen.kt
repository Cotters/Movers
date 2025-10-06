package com.jcotters.movie.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
  modifier: Modifier = Modifier,
  onViewEvent: (MovieDetailViewEvent) -> Unit,
  viewState: MovieDetailViewState,
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
    ) {
      if (viewState.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
          CircularProgressIndicator()
        }
      } else if (viewState.movie != null) {
        val movie = viewState.movie
        Text(text = movie.title, style = MaterialTheme.typography.bodyLarge)
        Text(text = movie.synopsis, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Preview
@Composable
private fun MovieDetailScreenPreview() {
//  MovieDetailScreen()
}
