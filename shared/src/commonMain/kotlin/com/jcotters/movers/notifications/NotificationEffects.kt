package com.jcotters.movers.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.movers.ui.NavigationState
import com.jcotters.movers.ui.TopLevelDestination
import com.jcotters.profile.domain.Notifications
import com.jcotters.watchlists.navigation.SharedListKey
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.getKoin
import kotlin.uuid.Uuid

@Composable
internal fun NotificationEffects(state: NavigationState, allowPrompt: Boolean, networkStatus: NetworkStatus) {
    IncomingNotificationNavigation(state)
    val notifications = getKoin().getOrNull<Notifications>() ?: return
    val repository = getKoin().get<IWatchlistsRepository>()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, notifications) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { notifications.refresh() }
    }
    LaunchedEffect(networkStatus, notifications) {
        if (networkStatus == NetworkStatus.Online) notifications.refresh()
    }
    val listId = when (val key = state.currentStack.lastOrNull()) {
        is WatchlistDetailKey -> key.listId
        is SharedListKey -> key.listId
        else -> null
    }
    LaunchedEffect(listId, allowPrompt, notifications) {
        if (listId != null && allowPrompt) {
            repository.observeWatchlist(listId).map { it?.watchlist?.shared != null }.distinctUntilChanged()
                .collect { shared -> if (shared) notifications.sharedListUsed() }
        }
    }
}

@Composable
private fun IncomingNotificationNavigation(state: NavigationState) {
    val target by IncomingWatchlistNotification.target.collectAsStateWithLifecycle()
    LaunchedEffect(target) {
        val incoming = target ?: return@LaunchedEffect
        if (state.selectedTab != TopLevelDestination.Watchlists) state.selectTab(TopLevelDestination.Watchlists)
        val stack = state.currentStack
        while (stack.size > 1) stack.removeLastOrNull()
        if (stack.isEmpty()) stack.add(WatchlistsKey)
        stack.add(WatchlistDetailKey(incoming.listId, notificationEntryId = Uuid.random().toString()))
        IncomingWatchlistNotification.consume(incoming)
    }
}
