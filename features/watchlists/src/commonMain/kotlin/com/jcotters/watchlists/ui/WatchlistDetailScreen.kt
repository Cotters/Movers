package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.designsystem.MoversAlertDialog
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.ScrollToTopButton
import com.jcotters.presentation.formatActivityTime
import com.jcotters.watchlists.WatchlistDetailState
import com.jcotters.watchlists.WatchlistDetailViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WatchlistDetailRoute(
    listId: String,
    navigator: Navigator,
    stackId: String,
    fromNotification: Boolean = false,
) {
    val viewModel: WatchlistDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions = viewModel.suggestions.collectAsLazyPagingItems()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable(listId) { mutableStateOf(false) }
    var showGrid by rememberSaveable(listId) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val headerScrollState = rememberWatchlistHeaderScrollState(listState, gridState, showGrid)
    val showScrollToTop by remember(listState, gridState, showGrid, headerScrollState) {
        derivedStateOf {
            (if (showGrid) gridState.canScrollBackward else listState.canScrollBackward) || !headerScrollState.visible
        }
    }
    var attribution by remember(listId) { mutableStateOf<FilmContribution?>(null) }
    WatchlistDetailEffects(
        listId, state, viewModel, navigator, listState, gridState, onRenamed = { renaming = false },
        fromNotification = fromNotification,
    )
    Scaffold(topBar = {
        WatchlistDetailTopBar(
            listId, state, viewModel, navigator, editing, showGrid,
            onLayoutChange = {
                val anchor = if (showGrid) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
                if (showGrid) listState.requestScrollToItem(anchor) else gridState.requestScrollToItem(anchor)
                showGrid = !showGrid
            },
            onEditingChanged = { editing = it },
            onRename = { renaming = true },
            onDelete = { deleting = true },
        )
    }, floatingActionButton = {
        WatchlistScrollButton(
            listState, gridState, headerScrollState, showGrid, visible = state.content != null && showScrollToTop,
        )
    }) { padding ->
        WatchlistDetailContent(
            state = state, viewModel = viewModel, navigator = navigator,
            listId = listId, stackId = stackId, showGrid = showGrid, editing = editing,
            listState = listState, gridState = gridState, headerScrollState = headerScrollState,
            suggestions = suggestions, showScrollToTop = showScrollToTop, padding = padding,
            onContribution = { attribution = it },
        )
    }
    WatchlistDetailDialogs(
        state, viewModel, renaming, deleting, attribution,
        onDismissRename = { renaming = false },
        onDismissDelete = { deleting = false },
        onDismissAttribution = { attribution = null },
    )
}

@Composable
private fun ListRemovalDialog(
    list: Watchlist,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shared = list.shared != null
    MoversAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (shared) "Leave list?" else "Delete list?") },
        text = {
            Column {
                Text(
                    if (shared) "Remove “${list.name}” from this device? " +
                        "Other members keep access. If you are the last member, the list will be retired."
                    else "Delete “${list.name}” and its movie selections?"
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            MoversTextButton(
                onClick = onConfirm, enabled = !busy,
                destructive = true
            ) {
                Text(if (shared) "Leave" else "Delete")
            }
        },
        dismissButton = { MoversTextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun FilmAttributionDialog(contribution: FilmContribution, onDismiss: () -> Unit) {
    val action = if (contribution.imported) "Imported" else "Added"
    val addedAt = formatActivityTime(contribution.addedAt)
    MoversAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contribution.actor.name) },
        text = { Text("$action · $addedAt") },
        confirmButton = { MoversTextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun WatchlistDetailEffects(
    listId: String,
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    listState: LazyListState,
    gridState: LazyGridState,
    onRenamed: () -> Unit,
    fromNotification: Boolean,
) {
    LaunchedEffect(listId) { viewModel.load(listId, fromNotification) }
    LaunchedEffect(state.deleted) { if (state.deleted) navigator.goBack() }
    LaunchedEffect(state.renamed) {
        if (state.renamed) {
            onRenamed()
            viewModel.consumeRenamed()
        }
    }
    var previousSort by remember { mutableStateOf<com.jcotters.watchlists.preferences.WatchlistSort?>(null) }
    LaunchedEffect(state.sort, state.preferencesReady) {
        if (state.preferencesReady) {
            if (previousSort != null && previousSort != state.sort) {
                listState.requestScrollToItem(0)
                gridState.requestScrollToItem(0)
            }
            previousSort = state.sort
        }
    }
}

@Composable
private fun WatchlistDetailDialogs(
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    renaming: Boolean,
    deleting: Boolean,
    attribution: FilmContribution?,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissAttribution: () -> Unit,
) {
    val list = state.content?.watchlist
    if (renaming && list != null) ListNameDialog(
        "Rename list",
        state.lists,
        list.name,
        list.id,
        state.busy,
        state.error,
        onDismiss = {
            onDismissRename()
            viewModel.clearError()
        },
        onSave = viewModel::rename
    )
    if (deleting && list != null) ListRemovalDialog(
        list = list,
        busy = state.busy,
        error = state.error,
        onDismiss = {
            onDismissDelete()
            viewModel.clearError()
        },
        onConfirm = { if (list.shared != null) viewModel.leave() else viewModel.delete() },
    )
    attribution?.let { contribution ->
        FilmAttributionDialog(contribution, onDismiss = onDismissAttribution)
    }
}

@Composable
private fun WatchlistScrollButton(
    listState: LazyListState,
    gridState: LazyGridState,
    headerScrollState: WatchlistHeaderScrollState,
    showGrid: Boolean,
    visible: Boolean,
) {
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = {
            scope.launch {
                if (showGrid) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
                headerScrollState.show()
            }
        },
    )
}
