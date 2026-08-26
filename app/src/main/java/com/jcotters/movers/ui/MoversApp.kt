package com.jcotters.movers.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jcotters.auth.domain.UserSession

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoversApp() {

    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = hiltViewModel()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle(UserSession.NotAuthenticated)

    LaunchedEffect(userSession) {
        if (userSession !is UserSession.Unknown) {
            navController.navigate(NavigationRoutes.Home) {
                popUpTo(NavigationRoutes.Splash) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalUserSession provides userSession) {
            NavHost(
                navController = navController,
                startDestination = NavigationRoutes.Splash,
            ) {
                composable<NavigationRoutes.Splash> {
                    MoversSplashScreen()
                }

                authNavGraph(navController = navController)

                homeNavigationGraph(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
        }
    }
}