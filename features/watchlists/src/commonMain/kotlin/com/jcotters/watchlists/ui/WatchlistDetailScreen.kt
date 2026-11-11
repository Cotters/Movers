package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.MovieLayoutToggle
import com.jcotters.presentation.ScrollToTopButton
import com.jcotters.presentation.formatActivityTime
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.presentation.resources.more
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.watchlists.WatchlistDetailViewModel
import com.jcotters.watchlists.navigation.ListTransferKey
import com.jcotters.watchlists.navigation.SharedListKey
import com.jcotters.watchlists.navigation.AddToListsKey
import com.jcotters.watchlists.navigation.SharedSection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchlistDetailRoute(listId: String, navigator: Navigator, stackId: String) {
    val viewModel: WatchlistDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable(listId) { mutableStateOf(false) }
    var showGrid by rememberSaveable(listId) { mutableStateOf(false) }
    var query by rememberSaveable(listId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val headerScrollState = rememberWatchlistHeaderScrollState(listState, gridState, showGrid)
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember(listState, gridState, showGrid, headerScrollState) {
        derivedStateOf {
            (if (showGrid) gridState.canScrollBackward else listState.canScrollBackward) || !headerScrollState.visible
        }
    }
    var attribution by remember(listId) { mutableStateOf<FilmContribution?>(null) }
    LaunchedEffect(listId) { viewModel.load(listId) }
    LaunchedEffect(state.deleted) { if (state.deleted) navigator.goBack() }
    LaunchedEffect(state.renamed) { if (state.renamed) { renaming = false; viewModel.consumeRenamed() } }
    val list = state.content?.watchlist
    Scaffold(topBar = {
        TopAppBar(title = { Text(list?.name ?: "Watchlist", maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
            if (!editing) {
                IconButton(onClick = navigator::goBack) { Icon(painterResource(Res.drawable.arrow_back), "Back") }
            }
        }, actions = {
            MovieLayoutToggle(showGrid = showGrid, onClick = {
                val anchor = if (showGrid) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
                if (showGrid) listState.requestScrollToItem(anchor) else gridState.requestScrollToItem(anchor)
                showGrid = !showGrid
            })
            if (editing) {
                TextButton(onClick = { editing = false }) { Text("Done") }
            } else {
                IconButton(onClick = { navigator.navigate(MovieSearchKey(listId)) }, enabled = list != null && !state.busy) {
                    Icon(painterResource(Res.drawable.add), "Add movies")
                }
                IconButton(onClick = { menu = true }, enabled = list != null && !state.busy) {
                    Icon(painterResource(Res.drawable.more), "List options")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Import / export") }, onClick = { menu = false; navigator.navigate(ListTransferKey(listId)) })
                    DropdownMenuItem(text = { Text("Edit list") }, onClick = { menu = false; editing = true })
                    DropdownMenuItem(text = { Text("Rename list") }, onClick = { menu = false; viewModel.clearError(); renaming = true })
                    DropdownMenuItem(text = { Text(if (list?.shared != null) "Share link" else "Share list") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Sharing)) })
                    if (list?.shared != null) {
                        DropdownMenuItem(text = { Text("Members") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Members)) })
                        DropdownMenuItem(text = { Text("Activity") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Activity)) })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(if (list?.shared != null) "Leave list" else "Delete list", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; viewModel.clearError(); deleting = true })
                }
            }
        })
    }, floatingActionButton = {
        ScrollToTopButton(
            visible = state.content != null && showScrollToTop,
            onClick = {
                coroutineScope.launch {
                    if (showGrid) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
                    headerScrollState.show()
                }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator()
            state.error?.let {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.clearError(); viewModel.load(listId) }) { Text("Retry") }
            }
            if (!state.loading && state.error == null && state.content == null) Text("This list no longer exists.", Modifier.padding(24.dp))
            state.content?.let { content ->
                val visibleMovies = remember(content.movies, query) {
                    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
                    if (terms.isEmpty()) content.movies else content.movies.filter { movie ->
                        terms.all { movie.title.contains(it, ignoreCase = true) }
                    }
                }
                LaunchedEffect(visibleMovies.isEmpty(), headerScrollState) {
                    if (visibleMovies.isEmpty()) headerScrollState.show()
                }
                WatchlistSearchHeader(
                    scrollState = headerScrollState,
                    query = query,
                    onQueryChange = {
                        query = it
                        headerScrollState.show()
                        listState.requestScrollToItem(0)
                        gridState.requestScrollToItem(0)
                    },
                    summary = {
                        WatchlistSummary(
                            movieCount = visibleMovies.size,
                            totalMovieCount = content.movies.size,
                            sharedInfo = content.watchlist.shared,
                            onMembers = { navigator.navigate(SharedListKey(listId, SharedSection.Members)) },
                        )
                    },
                )
                if (content.movies.isEmpty()) Text("No movies yet. Tap + to find films.", Modifier.padding(24.dp))
                else if (visibleMovies.isEmpty()) Text("No matching films.", Modifier.padding(24.dp))
                WatchlistMovies(
                    content = content.copy(movies = visibleMovies),
                    showGrid = showGrid,
                    listState = listState,
                    gridState = gridState,
                    stackId = stackId,
                    editing = editing,
                    enabled = !state.busy,
                    removing = state.removing,
                    onOpen = { navigator.navigate(MovieDetailsKey(it, targetListId = listId)) },
                    onRemove = viewModel::removeMovie,
                    onSwipeRemove = viewModel::stageMovieRemoval,
                    onAddToLists = { navigator.navigate(AddToListsKey(it, listId)) },
                    onContribution = { attribution = it },
                    bottomContentPadding = if (showScrollToTop) 88.dp else 24.dp,
                    modifier = Modifier.weight(1f).clipToBounds().nestedScroll(headerScrollState),
                )
            }
        }
    }
    if (renaming && list != null) ListNameDialog("Rename list", state.lists, list.name, list.id, state.busy, state.error,
        onDismiss = { renaming = false; viewModel.clearError() }, onSave = viewModel::rename)
    if (deleting && list != null) ListRemovalDialog(
        list = list,
        busy = state.busy,
        error = state.error,
        onDismiss = { deleting = false; viewModel.clearError() },
        onConfirm = { if (list.shared != null) viewModel.leave() else viewModel.delete() },
    )
    attribution?.let { contribution ->
        FilmAttributionDialog(contribution, onDismiss = { attribution = null })
    }
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
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (shared) "Leave list?" else "Delete list?") },
        text = {
            Column {
                Text(if (shared) "Remove “${list.name}” from this device? Other members keep access. If you are the last member, the list will be retired."
                    else "Delete “${list.name}” and its movie selections?")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(if (shared) "Leave" else "Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun FilmAttributionDialog(contribution: FilmContribution, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contribution.actor.name) },
        text = { Text("${if (contribution.imported) "Imported" else "Added"} · ${formatActivityTime(contribution.addedAt)}") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
