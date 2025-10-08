package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.jcotters.auth.domain.UserSession

@Composable
fun MoversApp() {

  val navController: NavHostController = rememberNavController()
  val viewModel: AppViewModel = hiltViewModel()
  val userSession by viewModel.userSession.collectAsState(initial = UserSession.NotAuthenticated)

  LaunchedEffect(userSession) {
    navController.navigate(NavigationRoutes.Home) {
      popUpTo(NavigationRoutes.Home)
      launchSingleTop = true
    }
  }

  NavHost(
    navController = navController,
    startDestination = NavigationRoutes.Home,
  ) {
    authNavGraph(navController = navController)

    homeNavigationGraph(
      navController = navController,
      userSessionFlow = viewModel.userSession,
    )
  }
}