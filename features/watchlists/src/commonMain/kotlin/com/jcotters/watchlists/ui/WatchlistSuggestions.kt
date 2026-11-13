package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.presentation.MovieMembershipButton
import com.jcotters.presentation.MovieSearchResult
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.watchlists.WatchlistDetailState

internal fun LazyListScope.watchlistSuggestions(
    state: WatchlistDetailState,
    movies: LazyPagingItems<Movie>,
    stackId: String,
    onOpen: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onDismissError: () -> Unit,
) {
    if (state.query.isBlank()) return
    item(key = "suggestions-heading") { SuggestionHeader(state, movies, onDismissError) }
    if (!state.waitingForSuggestions) {
        items(movies.itemCount, key = movies.itemKey { "suggestion-${it.id}" }) { index ->
            movies[index]?.let { SuggestionRow(it, state, stackId, onOpen, onAdd) }
        }
        item(key = "suggestions-footer") { SuggestionFooter(movies) }
    }
}

internal fun LazyGridScope.watchlistSuggestions(
    state: WatchlistDetailState,
    movies: LazyPagingItems<Movie>,
    stackId: String,
    onOpen: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onDismissError: () -> Unit,
) {
    if (state.query.isBlank()) return
    item(key = "suggestions-heading", span = { GridItemSpan(maxLineSpan) }) {
        SuggestionHeader(
            state,
            movies,
            onDismissError
        )
    }
    if (!state.waitingForSuggestions) {
        items(
            movies.itemCount,
            key = movies.itemKey { "suggestion-${it.id}" },
            span = { GridItemSpan(maxLineSpan) }) { index ->
            movies[index]?.let { SuggestionRow(it, state, stackId, onOpen, onAdd) }
        }
        item(key = "suggestions-footer", span = { GridItemSpan(maxLineSpan) }) { SuggestionFooter(movies) }
    }
}

@Composable
private fun SuggestionHeader(state: WatchlistDetailState, movies: LazyPagingItems<Movie>, onDismissError: () -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Add to list", style = MaterialTheme.typography.titleMedium)
        state.suggestionError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            MoversTextButton(onClick = onDismissError) { Text("Dismiss") }
        }
        when {
            state.waitingForSuggestions || movies.loadState.refresh is LoadState.Loading -> Text("Finding films…")
            movies.loadState.refresh is LoadState.Error -> {
                Text(
                    (movies.loadState.refresh as LoadState.Error).error.message ?: "Couldn't find films.",
                    color = MaterialTheme.colorScheme.error
                )
                MoversTextButton(onClick = movies::retry) { Text("Retry search") }
            }

            movies.itemCount == 0 -> Text("No new films found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SuggestionRow(
    movie: Movie,
    state: WatchlistDetailState,
    stackId: String,
    onOpen: (Int) -> Unit,
    onAdd: (Int) -> Unit
) {
    if (movie.id in state.excludedSuggestionIds) return
    Column {
        MovieSearchResult(
            movie, compact = true,
            posterModifier = moviePosterTransitionModifier(movie.id, stackId), onOpen = { onOpen(movie.id) }) {
            MovieMembershipButton(
                movie.title,
                isAdded = false,
                onClick = { onAdd(movie.id) },
                enabled = state.canAddSuggestions && movie.id !in state.excludedSuggestionIds,
                saving = movie.id in state.adding
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun SuggestionFooter(movies: LazyPagingItems<Movie>) {
    when (movies.loadState.append) {
        is LoadState.Loading -> Text("Finding more films…", Modifier.padding(8.dp))
        is LoadState.Error -> MoversTextButton(onClick = movies::retry) { Text("Retry more films") }
        else -> Unit
    }
}
