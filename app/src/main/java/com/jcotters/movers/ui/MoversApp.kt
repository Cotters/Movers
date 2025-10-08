package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun MoversApp() {

  val navController: NavHostController = rememberNavController()
  val viewModel: AppViewModel = hiltViewModel()

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