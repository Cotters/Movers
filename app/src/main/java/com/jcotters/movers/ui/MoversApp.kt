package com.jcotters.movers.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jcotters.auth.domain.UserSession

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoversApp() {
    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = hiltViewModel()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var previousSession: UserSession by remember { mutableStateOf(UserSession.Loading) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    if (userSession == UserSession.Loading) {
        MoversSplashScreen()
        return
    }

    LaunchedEffect(userSession, backStackEntry) {
        val destination = backStackEntry?.destination ?: return@LaunchedEffect
        val hasProfileInBackStack = try {
            navController.getBackStackEntry(NavigationRoutes.Profile)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
        val resetToHome = shouldResetToHome(
            previousSession = previousSession,
            session = userSession,
            onSplash = destination.hasRoute<NavigationRoutes.Splash>(),
            inAuthGraph = destination.hierarchy.any { it.hasRoute<NavigationRoutes.Auth>() },
            hasProfileInBackStack = hasProfileInBackStack,
        )
        previousSession = userSession
        if (resetToHome) {
            navController.navigate(NavigationRoutes.Home) {
                popUpTo(navController.graph.id) {
                    inclusive = false
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
