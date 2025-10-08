package com.jcotters.movers.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.jcotters.auth.ui.AuthMode
import com.jcotters.auth.ui.AuthViewEvent
import com.jcotters.auth.ui.AuthViewModel
import com.jcotters.auth.ui.LoginScreen
import com.jcotters.auth.ui.SignUpScreen

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
      val context = LocalContext.current
      val viewModel: AuthViewModel = hiltViewModel()
      val viewState by viewModel.uiState.collectAsState()
      LaunchedEffect(Unit) {
        viewModel.onViewEvent(AuthViewEvent.OnLoad(context))
      }
      LaunchedEffect(viewState.successfulLogin) {
        if (viewState.successfulLogin) {
          navController.navigate(NavigationRoutes.Catalogue)
        }
      }
      when (viewState.authMode) {
        AuthMode.Login -> {
          LoginScreen(
            viewState = viewState,
            onViewEvent = viewModel::onViewEvent,
          )
        }
        AuthMode.SignUp -> {
          SignUpScreen(
            viewState = viewState,
            onViewEvent = viewModel::onViewEvent,
          )
        }
      }
    }
  }

}