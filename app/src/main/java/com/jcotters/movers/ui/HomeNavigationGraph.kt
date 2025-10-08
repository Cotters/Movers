package com.jcotters.movers.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

fun NavGraphBuilder.homeNavigationGraph(
  navController: NavHostController,
  userSessionFlow: Flow<UserSession>,
) {

  navigation<NavigationRoutes.Home>(startDestination = NavigationRoutes.Catalogue) {
    composable<NavigationRoutes.Catalogue> {
      val viewModel: MovieCatalogueViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      val userSession by userSessionFlow.collectAsState(initial = UserSession.NotAuthenticated)

      MovieCatalogueScreen(
        isAuthenticated = userSession is UserSession.Authenticated,
        movies = viewState.movies,
        onMovieTapped = { movieId ->
          navController.navigate(NavigationRoutes.MovieDetails(movieId = movieId))
        },
        onAccountTapped = {
          when (userSession) {
            is UserSession.Authenticated -> {
              val userId = (userSession as UserSession.Authenticated).userId
              navController.navigate(NavigationRoutes.Profile(userId))
            }

            is UserSession.NotAuthenticated -> {
              navController.navigate(NavigationRoutes.Auth) {
                launchSingleTop = true
              }
            }
          }
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

  composable<NavigationRoutes.Profile>(
    enterTransition = {
      slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(350))
    },
    exitTransition = {
      slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(350))
    },
  ) { backStackEntry ->
    val userId: Int = backStackEntry.toRoute<NavigationRoutes.Profile>().userId
    val viewModel: ProfileViewModel = hiltViewModel()
    val viewState by viewModel.uiState.collectAsState()
    val userSession by userSessionFlow.collectAsState(initial = UserSession.NotAuthenticated)

    LaunchedEffect(userSession) {
      if (userSession is UserSession.Authenticated) {
        val userId = (userSession as UserSession.Authenticated).userId
        viewModel.onViewEvent(ProfileViewEvent.UserSessionFound(userId))
      }
    }

    ProfileScreen(
      viewState = viewState,
      onViewEvent = viewModel::onViewEvent,
      userSession = "$userSession",
    )
  }
}
