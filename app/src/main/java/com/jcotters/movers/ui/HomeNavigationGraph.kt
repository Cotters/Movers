package com.jcotters.movers.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.jcotters.auth.domain.UserSession
import com.jcotters.movie.catalogue.ui.MovieCatalogueScreen
import com.jcotters.movie.catalogue.ui.MovieCatalogueViewModel
import com.jcotters.movie.detail.ui.MovieDetailScreen
import com.jcotters.movie.detail.ui.MovieDetailViewEvent
import com.jcotters.movie.detail.ui.MovieDetailViewModel
import com.jcotters.profile.ui.ProfileScreen
import com.jcotters.profile.ui.ProfileViewEvent
import com.jcotters.profile.ui.ProfileViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.homeNavigationGraph(
  navController: NavHostController,
  userSessionFlow: Flow<UserSession>,
  sharedTransitionScope: SharedTransitionScope,
) {

  navigation<NavigationRoutes.Home>(startDestination = NavigationRoutes.Catalogue) {
    composable<NavigationRoutes.Catalogue> {
      val viewModel: MovieCatalogueViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      val userSession by userSessionFlow.collectAsState(initial = UserSession.Unknown)

      MovieCatalogueScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        isAuthenticated = userSession is UserSession.Authenticated,
        movies = viewState.movies,
        onMovieTapped = { movieId ->
          navController.navigate(NavigationRoutes.MovieDetails(movieId = movieId))
        },
        onAccountTapped = {
          if (userSession is UserSession.Authenticated) {
            navController.navigate(NavigationRoutes.Profile)
          } else if (userSession is UserSession.NotAuthenticated) {
            navController.navigate(NavigationRoutes.Auth) {
              launchSingleTop = true
            }
          }
        }
      )
    }

    composable<NavigationRoutes.MovieDetails>(
      enterTransition = {
        scaleIn(
          tween(500, easing = LinearOutSlowInEasing),
          transformOrigin = TransformOrigin.Center.copy(pivotFractionX = 0.5f, pivotFractionY = 1.0f)
        )
      },
      exitTransition = {
        slideOutOfContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Down,
          animationSpec = tween(500, easing = LinearOutSlowInEasing),
        )
      },
    ) { backStackEntry ->
      val movieId: Int = backStackEntry.toRoute<NavigationRoutes.MovieDetails>().movieId
      val viewModel: MovieDetailViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()

      ErrorMessageHandler(errorMessage = viewModel.errorMessage, context = LocalContext.current)

      LaunchedEffect(movieId) {
        viewModel.onViewEvent(MovieDetailViewEvent.OnLoad(movieId))
      }

      MovieDetailScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        viewState = viewState,
        onViewEvent = viewModel::onViewEvent,
      )
    }
  }

  composable<NavigationRoutes.Profile>(
    enterTransition = {
      slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350))
    },
    exitTransition = {
      slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(350))
    },
  ) { backStackEntry ->
    val viewModel: ProfileViewModel = hiltViewModel()
    val viewState by viewModel.uiState.collectAsState()
    val userSession by userSessionFlow.collectAsState(initial = UserSession.Unknown)

    ErrorMessageHandler(errorMessage = viewModel.errorMessage, context = LocalContext.current)

    LaunchedEffect(userSession) {
      if (userSession is UserSession.Authenticated) {
        val userId = (userSession as UserSession.Authenticated).userId
        viewModel.onViewEvent(ProfileViewEvent.UserSessionFound(userId))
      }
    }

    ProfileScreen(
      viewState = viewState,
      onViewEvent = viewModel::onViewEvent,
    )
  }
}

@Composable
fun ErrorMessageHandler(
  errorMessage: SharedFlow<String>,
  context: Context,
) {
  LaunchedEffect(Unit) {
    errorMessage.collect { message ->
      Toast.makeText(
        context,
        message,
        Toast.LENGTH_LONG,
      ).show()
    }
  }

}