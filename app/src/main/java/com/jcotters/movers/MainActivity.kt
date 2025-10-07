package com.jcotters.movers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jcotters.movers.ui.theme.MoversTheme
import com.jcotters.movie.detail.ui.MovieDetailScreen
import com.jcotters.movie.detail.ui.MovieDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MovieDetailViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      MoversTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MovieDetailScreen(
            modifier = Modifier.padding(innerPadding),
            onViewEvent = viewModel::onViewEvent,
            viewState = viewState,
          )
        }
      }
    }
  }
}
