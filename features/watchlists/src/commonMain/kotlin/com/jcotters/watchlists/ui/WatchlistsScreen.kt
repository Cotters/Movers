package com.jcotters.watchlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversIcon
import com.jcotters.designsystem.MoversListItem
import com.jcotters.designsystem.MoversSwipeAction
import com.jcotters.designsystem.MoversSwipeActions
import com.jcotters.designsystem.MoversSymbol
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.navigation.Navigator
import com.jcotters.watchlists.WatchlistsState
import com.jcotters.watchlists.WatchlistsViewModel
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import com.jcotters.watchlists.preferences.MaximumPinnedWatchlists
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchlistsRoute(navigator: Navigator) {
    val viewModel: WatchlistsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.createdId) {
        state.createdId?.let { id ->
            creating = false
            viewModel.consumeCreated()
            navigator.navigate(WatchlistDetailKey(id))
        }
    }
    Scaffold(topBar = {
        MoversTopBar(title = { Text("Watchlists") }, actions = {
            MoversTextButton(onClick = { navigator.navigate(com.jcotters.watchlists.navigation.JoinWatchlistKey) }) {
                Text(
                    "Join list"
                )
            }
            MoversTextButton(
                onClick = { viewModel.clearError(); creating = true },
                enabled = !state.loading && !state.busy
            ) { Text("Create list") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator()
            state.syncError?.let {
                Text("Shared updates unavailable. Cached lists remain readable.", Modifier.padding(16.dp))
                MoversTextButton(onClick = viewModel::refreshShared) { Text("Retry shared updates") }
            }
            state.error?.let {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                MoversTextButton(onClick = { viewModel.clearError(); viewModel.retry() }) { Text("Retry") }
            }
            if (!state.loading && state.error == null && state.lists.isEmpty()) {
                Column(Modifier.padding(24.dp)) {
                    Text("Create a list for your next movie night.")
                    MoversButton(onClick = {
                        viewModel.clearError(); creating = true
                    }) { Text("Create your first list") }
                }
            }
            state.preferenceError?.let { message ->
                Text(message, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                MoversTextButton(onClick = viewModel::retryPreferences) { Text("Retry pins") }
            }
            LazyColumn(Modifier.weight(1f)) {
                if (state.pinnedLists.isNotEmpty()) {
                    item(key = "pinned-heading") {
                        Text(
                            "Pinned · ${state.pinnedLists.size}/$MaximumPinnedWatchlists",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    watchlistRows(state.pinnedLists, state, viewModel::setPinned) {
                        navigator.navigate(
                            WatchlistDetailKey(it)
                        )
                    }
                    if (state.otherLists.isNotEmpty()) item(key = "other-heading") {
                        Text(
                            "Other lists", Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                watchlistRows(
                    state.otherLists,
                    state,
                    viewModel::setPinned
                ) { navigator.navigate(WatchlistDetailKey(it)) }
            }
        }
    }
    if (creating) ListNameDialog(
        "Create list", state.lists, busy = state.busy, error = state.error,
        onDismiss = { creating = false; viewModel.clearError() }, onSave = viewModel::create
    )
}

private fun LazyListScope.watchlistRows(
    lists: List<Watchlist>,
    state: WatchlistsState,
    onPin: (String, Boolean) -> Unit,
    onOpen: (String) -> Unit,
) {
    items(lists, key = { it.id }) { list ->
        val pinned = list.id in state.pinnedIds
        val canPin = pinned || state.canPinMore
        MoversSwipeActions(
            onOpen = { onOpen(list.id) },
            endAction = MoversSwipeAction(
                label = if (pinned) "Unpin" else if (canPin) "Pin" else "$MaximumPinnedWatchlists pin limit",
                accessibilityLabel = "${if (pinned) "Unpin" else "Pin"} ${list.name}",
                onClick = { onPin(list.id, !pinned) },
                enabled = canPin,
                allowsFullSwipe = true,
                icon = { MoversIcon(if (pinned) MoversSymbol.PinOff else MoversSymbol.Pin, tint = it) },
            ),
            enabled = state.preferencesReady && !state.busy && list.id !in state.pinning,
            modifier = Modifier.animateItem(),
        ) { swipeModifier, open ->
            Column(swipeModifier) {
                MoversListItem(
                    headlineContent = { Text(list.name) },
                    supportingContent = {
                        Text(
                            "${list.movieCount} ${if (list.movieCount == 1) "movie" else "movies"}" +
                                (list.shared?.let { " · Shared · ${it.members.size} members" } ?: ""))
                    },
                    leadingContent = list.shared?.let { info ->
                        {
                            com.jcotters.presentation.ParticipantAvatar(
                                info.creator.name,
                                info.creator.color
                            )
                        }
                    },
                    trailingContent = if (pinned) ({
                        MoversIcon(
                            MoversSymbol.Pin,
                            tint = MoversDesign.tokens.colors.accent
                        )
                    }) else null,
                    modifier = Modifier.heightIn(min = 72.dp).clickable(onClick = open).semantics {
                        if (pinned) stateDescription = "Pinned"
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MoversDesign.tokens.colors.border)
            }
        }
    }
}
