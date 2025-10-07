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
import com.jcotters.auth.ui.LoginScreen
import com.jcotters.auth.ui.LoginViewModel

fun NavGraphBuilder.authNavGraph(
  navController: NavHostController,
) {
  navigation<NavigationRoutes.Auth>(startDestination = NavigationRoutes.Login) {
    composable<NavigationRoutes.Login>(
      enterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(350))
      },
      exitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(350))
      },
    ) {
      val viewModel: LoginViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      LaunchedEffect(viewState.successfulLogin) {
        if (viewState.successfulLogin) {
          navController.navigate(NavigationRoutes.Catalogue)
        }
      }
      LoginScreen(
        viewState = viewState,
        onViewEvent = viewModel::onViewEvent,
      )
    }
  }

}