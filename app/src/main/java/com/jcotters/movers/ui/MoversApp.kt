package com.jcotters.movers.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jcotters.movers.R
import com.jcotters.movie.catalogue.ui.MovieCatalogueScreen
import com.jcotters.movie.catalogue.ui.MovieCatalogueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoversApp() {
  val navController: NavHostController = rememberNavController()
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Movers") },
        navigationIcon = {
          if (navController.previousBackStackEntry != null) {
            IconButton(onClick = navController::popBackStack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_button)
              )
            }
          }
        }
      )
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = NavigationRoutes.Catalogue,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable<NavigationRoutes.Catalogue> {
        val viewModel: MovieCatalogueViewModel = hiltViewModel()
        val viewState by viewModel.uiState.collectAsState()
        MovieCatalogueScreen(
          movies = viewState.movies,
          onMovieTapped = { movieId -> }
        )
      }
    }
  }
}