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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.cotters.data.network.NetworkStatus
import com.jcotters.movers.resources.Res
import com.jcotters.movers.resources.navigation_home
import com.jcotters.movers.resources.navigation_profile
import com.jcotters.movers.resources.navigation_watchlists
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.LocalMovieTransitionScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MoversApp(installers: Set<EntryProviderInstaller>) {
    val viewModel: AppViewModel = koinViewModel()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val startup by viewModel.startupState.collectAsStateWithLifecycle()
    when (startup) {
        StartupState.Loading -> MoversSplashScreen()
        StartupState.Ready -> MoversNavigationShell(networkStatus, installers)
        StartupState.Failed -> androidx.compose.foundation.layout.Column {
            Text("Unable to open local app data.")
            androidx.compose.material3.Button(onClick = viewModel::initializeProfile) { Text("Retry") }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MoversNavigationShell(
    networkStatus: NetworkStatus,
    installers: Set<EntryProviderInstaller>,
    state: NavigationState = rememberNavigationState(),
) {
    val navigator = remember(state) { AppNavigator(state) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
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
            key(state) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalMovieTransitionScope provides this, com.jcotters.presentation.LocalSnackbarHostState provides snackbar) {
                        val entries = state.decoratedEntries(installers, navigator)
                        NavDisplay(
                            entries = entries,
                            onBack = navigator::goBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            OfflineNotice(networkStatus, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private val TopLevelDestination.labelRes: org.jetbrains.compose.resources.StringResource get() = when (this) {
    TopLevelDestination.Home -> Res.string.navigation_home
    TopLevelDestination.Watchlists -> Res.string.navigation_watchlists
    TopLevelDestination.Profile -> Res.string.navigation_profile
}

private val TopLevelDestination.iconRes: org.jetbrains.compose.resources.DrawableResource get() = when (this) {
    TopLevelDestination.Home -> Res.drawable.navigation_home
    TopLevelDestination.Watchlists -> Res.drawable.navigation_watchlists
    TopLevelDestination.Profile -> Res.drawable.navigation_profile
}
