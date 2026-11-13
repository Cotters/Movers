package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.watchlists.WatchlistDetailState
import com.jcotters.watchlists.WatchlistDetailViewModel
import com.jcotters.watchlists.navigation.AddToListsKey
import com.jcotters.watchlists.navigation.SharedListKey
import com.jcotters.watchlists.navigation.SharedSection

@Composable
internal fun WatchlistDetailContent(
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    listId: String,
    stackId: String,
    showGrid: Boolean,
    editing: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    headerScrollState: WatchlistHeaderScrollState,
    suggestions: LazyPagingItems<Movie>,
    showScrollToTop: Boolean,
    padding: PaddingValues,
    onContribution: (FilmContribution) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
        if (state.loading) LinearProgressIndicator()
        state.error?.let {
            Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            MoversTextButton(onClick = viewModel::retry) { Text("Retry") }
        }
        state.sortError?.let { message ->
            Text(message, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            MoversTextButton(onClick = viewModel.sorting::retry) { Text("Retry sorting") }
        }
        if (!state.loading && state.error == null && state.content == null) Text(
            "This list no longer exists.",
            Modifier.padding(24.dp)
        )
        state.content?.let { content ->
            WatchlistFilmContent(
                content, state, viewModel, navigator, listId, stackId, showGrid, editing,
                listState, gridState, headerScrollState, suggestions, showScrollToTop, onContribution,
            )
        }
    }
}

@Composable
private fun ColumnScope.WatchlistFilmContent(
    content: WatchlistContent,
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    listId: String,
    stackId: String,
    showGrid: Boolean,
    editing: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    headerScrollState: WatchlistHeaderScrollState,
    suggestions: LazyPagingItems<Movie>,
    showScrollToTop: Boolean,
    onContribution: (FilmContribution) -> Unit,
) {
    val openMovie: (Int) -> Unit = { navigator.navigate(MovieDetailsKey(it, targetListId = listId)) }
    val addSuggestion = viewModel.filmSearch::addSuggestedMovie
    val dismissError = viewModel.filmSearch::clearSuggestionError
    val visibleMovies = state.visibleMovies
    LaunchedEffect(visibleMovies.isEmpty(), headerScrollState) {
        if (visibleMovies.isEmpty()) headerScrollState.show()
    }
    WatchlistSearchArea(
        state, content, viewModel, navigator, listId, listState, gridState, headerScrollState,
        visibleCount = visibleMovies.size,
    )
    WatchlistMovies(
        content = content.copy(movies = visibleMovies),
        showGrid = showGrid,
        listState = listState,
        gridState = gridState,
        stackId = stackId,
        editing = editing,
        enabled = !state.busy,
        removing = state.removing,
        onOpen = openMovie,
        onRemove = viewModel::removeMovie,
        onSwipeRemove = viewModel::stageMovieRemoval,
        onAddToLists = { navigator.navigate(AddToListsKey(it, listId)) },
        onContribution = onContribution,
        bottomContentPadding = if (showScrollToTop) 88.dp else 24.dp,
        modifier = Modifier.weight(1f).clipToBounds().nestedScroll(headerScrollState),
        emptyMessage = if (visibleMovies.isNotEmpty()) null
        else if (state.query.isNotBlank()) "No matches in this list."
        else "No movies yet. Tap + to find films.",
        listFooter = {
            watchlistSuggestions(state, suggestions, stackId, openMovie, addSuggestion, dismissError)
        },
        gridFooter = {
            watchlistSuggestions(state, suggestions, stackId, openMovie, addSuggestion, dismissError)
        },
    )
}

@Composable
private fun WatchlistSearchArea(
    state: WatchlistDetailState,
    content: WatchlistContent,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    listId: String,
    listState: LazyListState,
    gridState: LazyGridState,
    headerScrollState: WatchlistHeaderScrollState,
    visibleCount: Int,
) {
    WatchlistSearchHeader(
        scrollState = headerScrollState,
        query = state.query,
        onQueryChange = {
            viewModel.filmSearch.changeQuery(it)
            headerScrollState.show()
            listState.requestScrollToItem(0)
            gridState.requestScrollToItem(0)
        },
        summary = {
            WatchlistSummary(
                movieCount = visibleCount,
                totalMovieCount = content.movies.size,
                sharedInfo = content.watchlist.shared,
                sort = state.sort,
                sortEnabled = state.preferencesReady && !state.sortSaving,
                onSort = viewModel.sorting::change,
                onMembers = { navigator.navigate(SharedListKey(listId, SharedSection.Members)) },
            )
        },
    )
}
