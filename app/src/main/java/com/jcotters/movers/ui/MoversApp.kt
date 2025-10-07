package com.jcotters.movers.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jcotters.movers.R
import com.jcotters.movie.catalogue.ui.MovieCatalogueScreen
import com.jcotters.movie.catalogue.ui.MovieCatalogueViewModel
import com.jcotters.movie.detail.ui.MovieDetailScreen
import com.jcotters.movie.detail.ui.MovieDetailViewEvent
import com.jcotters.movie.detail.ui.MovieDetailViewModel

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
      composable<NavigationRoutes.Catalogue>(
        enterTransition = {
          slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350))
        },
        exitTransition = {
          slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350))
        },
      ) {
        val viewModel: MovieCatalogueViewModel = hiltViewModel()
        val viewState by viewModel.uiState.collectAsState()
        MovieCatalogueScreen(
          movies = viewState.movies,
          onMovieTapped = { movieId ->
            navController.navigate(NavigationRoutes.MovieDetails(movieId = movieId))
          }
        )
      }

      composable<NavigationRoutes.MovieDetails>(
        enterTransition = {
          slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350))
        },
        exitTransition = {
          slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350))
        },
      ) { backStackEntry ->
        val movieId: Int = backStackEntry.toRoute<NavigationRoutes.MovieDetails>().movieId
        val viewModel: MovieDetailViewModel = hiltViewModel()
        val viewState by viewModel.uiState.collectAsState()

        LaunchedEffect(movieId) {
          viewModel.onViewEvent(MovieDetailViewEvent.OnLoad(movieId))
        }

        MovieDetailScreen(
          viewState = viewState,
          onViewEvent = viewModel::onViewEvent,
        )
      }
    }
  }
}