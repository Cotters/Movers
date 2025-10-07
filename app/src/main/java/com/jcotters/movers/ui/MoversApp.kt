package com.jcotters.movers.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoversApp() {
  val navController: NavHostController = rememberNavController()
  NavHost(
    navController = navController,
    startDestination = NavigationRoutes.Home,
  ) {

    authNavGraph(navController = navController)

    homeNavigationGraph(navController = navController)
  }
}