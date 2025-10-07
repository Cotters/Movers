package com.jcotters.movers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jcotters.movers.ui.theme.MoversTheme
import com.jcotters.movie.catalogue.ui.MovieCatalogueScreen
import com.jcotters.movie.catalogue.ui.MovieCatalogueViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MovieCatalogueViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      MoversTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          if (viewState.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
              CircularProgressIndicator()
            }
          } else if (viewState.movies.isEmpty()) {
            Text("No movies found...")
          } else {
            MovieCatalogueScreen(
              movies = viewState.movies,
              modifier = Modifier.padding(innerPadding),
            )
          }
        }
      }
    }
  }
}
