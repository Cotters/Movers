package com.jcotters.movers.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.cotters.data.network.NetworkStatus
import com.jcotters.auth.domain.UserSession
import com.jcotters.movers.R
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.LocalMovieTransitionScope

@Composable
fun MoversApp(installers: Set<EntryProviderInstaller>) {
    val viewModel: AppViewModel = hiltViewModel()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val navigationState = rememberNavigationState((userSession as? UserSession.Authenticated)?.userId)
    if (userSession == UserSession.Loading) {
        Box(Modifier.fillMaxSize()) {
            MoversSplashScreen()
            OfflineNotice(networkStatus, Modifier.align(Alignment.BottomCenter))
        }
    } else {
        MoversNavigationShell(userSession, networkStatus, installers, navigationState)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MoversNavigationShell(
    session: UserSession,
    networkStatus: NetworkStatus,
    installers: Set<EntryProviderInstaller>,
    state: NavigationState = rememberNavigationState((session as? UserSession.Authenticated)?.userId),
) {
    val userId = (session as? UserSession.Authenticated)?.userId
    val sessionMatches = state.matchesSession(userId)
    if (!sessionMatches) SideEffect { state.reconcileSession(userId) }
    val navigator = remember(state) { AppNavigator(state) }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { state.selectTab(tab) },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            key(state.generation) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalMovieTransitionScope provides this) {
                        val entries = state.decoratedEntries(installers, navigator)
                        if (sessionMatches) {
                            NavDisplay(
                                entries = entries,
                                onBack = navigator::goBack,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            MoversSplashScreen()
                        }
                    }
                }
            }
            OfflineNotice(networkStatus, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private val TopLevelDestination.labelRes: Int get() = when (this) {
    TopLevelDestination.Home -> R.string.navigation_home
    TopLevelDestination.Watchlists -> R.string.navigation_watchlists
    TopLevelDestination.Profile -> R.string.navigation_profile
}

private val TopLevelDestination.iconRes: Int get() = when (this) {
    TopLevelDestination.Home -> R.drawable.navigation_home
    TopLevelDestination.Watchlists -> R.drawable.navigation_watchlists
    TopLevelDestination.Profile -> R.drawable.navigation_profile
}
