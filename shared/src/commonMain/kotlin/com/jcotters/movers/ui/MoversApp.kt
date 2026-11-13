package com.jcotters.movers.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.cotters.data.network.NetworkStatus
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.BottomSheetSceneStrategy
import com.jcotters.presentation.LocalMovieTransitionScope
import com.jcotters.watchlists.ui.WatchlistUndoMessages
import org.koin.compose.viewmodel.koinViewModel

private const val TAB_FADE_MILLIS = 220

@Composable
fun MoversApp(installers: Set<EntryProviderInstaller>) {
    val viewModel: AppViewModel = koinViewModel()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val startup by viewModel.startupState.collectAsStateWithLifecycle()
    when (startup) {
        StartupState.Loading -> MoversSplashScreen()
        StartupState.NameRequired -> {
            val busy by viewModel.savingName.collectAsStateWithLifecycle()
            val error by viewModel.nameError.collectAsStateWithLifecycle()
            val suggestedName by viewModel.suggestedName.collectAsStateWithLifecycle()
            com.jcotters.profile.ui.NameEntryScreen(busy, error, suggestedName, viewModel::saveName)
        }

        StartupState.Ready -> {
            val tourPreference = rememberFeatureTourPreference()
            var tourFinished by rememberSaveable { mutableStateOf(tourPreference.seen) }
            SharedListSynchronization()
            MoversNavigationShell(
                networkStatus, installers, snackbarEffects = { WatchlistUndoMessages() },
                navigationEffects = { navigation ->
                    com.jcotters.movers.notifications.NotificationEffects(navigation, tourFinished, networkStatus)
                }
            )
            FeatureTour(onFinish = { tourFinished = true })
        }

        StartupState.Failed -> androidx.compose.foundation.layout.Column {
            Text("Unable to open local app data.")
            com.jcotters.designsystem.MoversButton(onClick = viewModel::initializeProfile) { Text("Retry") }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MoversNavigationShell(
    networkStatus: NetworkStatus,
    installers: Set<EntryProviderInstaller>,
    state: NavigationState = rememberNavigationState(),
    snackbarEffects: @Composable () -> Unit = {},
    navigationEffects: @Composable (NavigationState) -> Unit = {},
) {
    val navigator = remember(state) { AppNavigator(state) }
    IncomingInvitations(state, navigator)
    navigationEffects(state)
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
    val forwardTransition = remember { defaultTransitionSpec<NavKey>() }
    val backTransition = remember { defaultPopTransitionSpec<NavKey>() }
    val predictiveBackTransition = remember { defaultPredictivePopTransitionSpec<NavKey>() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            MoversBottomBar(selectedTab = state.selectedTab, onSelectTab = state::selectTab)
        },
    ) { padding ->
        // Only destination content avoids the keyboard; the bottom bar stays at the window edge.
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            key(state) {
                SharedTransitionLayout {
                    CompositionLocalProvider(
                        LocalMovieTransitionScope provides this,
                        com.jcotters.presentation.LocalSnackbarHostState provides snackbar
                    ) {
                        snackbarEffects()
                        val entries = state.decoratedEntries(installers, navigator)
                        NavDisplay(
                            sceneStrategies = listOf(bottomSheetStrategy),
                            entries = entries,
                            onBack = navigator::goBack,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (changesTab()) tabFade() else forwardTransition()
                            },
                            popTransitionSpec = {
                                if (changesTab()) tabFade() else backTransition()
                            },
                            predictivePopTransitionSpec = { edge ->
                                if (changesTab()) tabFade() else predictiveBackTransition(edge)
                            },
                        )
                    }
                }
            }
            OfflineNotice(networkStatus, Modifier.align(Alignment.BottomCenter))
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { message ->
                com.jcotters.designsystem.MoversNotice(
                    message = message.visuals.message,
                    modifier = Modifier.padding(12.dp),
                    actionLabel = message.visuals.actionLabel,
                    onAction = message::performAction,
                    onDismiss = message::dismiss,
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.changesTab(): Boolean {
    val from = initialState.entries.lastOrNull()?.metadata?.get(NAVIGATION_TAB_METADATA)
    val to = targetState.entries.lastOrNull()?.metadata?.get(NAVIGATION_TAB_METADATA)
    return from != null && to != null && from != to
}

private fun tabFade(): ContentTransform = fadeIn(tween(TAB_FADE_MILLIS)) togetherWith fadeOut(tween(TAB_FADE_MILLIS))

@Composable
private fun IncomingInvitations(state: NavigationState, navigator: com.jcotters.navigation.Navigator) {
    val invitation by com.jcotters.contract.watchlists.domain.InvitationInbox.invitation.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(invitation) {
        if (invitation != null) {
            if (state.selectedTab != TopLevelDestination.Watchlists) state.selectTab(TopLevelDestination.Watchlists)
            navigator.navigate(com.jcotters.watchlists.navigation.JoinWatchlistKey)
        }
    }
}
