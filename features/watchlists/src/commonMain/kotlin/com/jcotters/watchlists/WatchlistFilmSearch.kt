package com.jcotters.watchlists

import com.jcotters.contract.detail.domain.models.Movie
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class WatchlistFilmSearch(
    private val repository: IWatchlistsRepository,
    search: IMovieSearchRepository,
    private val state: MutableStateFlow<WatchlistDetailState>,
    private val savedState: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    val query = savedState.getStateFlow("query", "")
    private val candidates = query.map(String::trim).distinctUntilChanged().flatMapLatest { value ->
        flow {
            // Clear the old generation and cancel its search immediately, before debouncing the new query.
            emit(PagingData.empty<Movie>())
            if (value.isNotEmpty()) delay(600)
            state.update { it.copy(suggestionQuery = value) }
            if (value.isNotEmpty()) emitAll(search.searchMovies(value))
        }
    }.cachedIn(scope)

    val suggestions =
        combine(candidates, state.map { it.excludedSuggestionIds }.distinctUntilChanged()) { page, excluded ->
            page.filter { it.id !in excluded }
        }.cachedIn(scope)

    fun changeQuery(query: String) {
        state.update { it.copy(suggestionError = null) }
        savedState["query"] = query
    }

    fun addSuggestedMovie(movieId: Int) {
        val id = state.value.listId ?: return
        val current = state.value
        if (!current.canAddSuggestions || movieId in current.adding || movieId in current.excludedSuggestionIds) return
        state.update { it.copy(adding = it.adding + movieId, suggestionError = null) }
        scope.launch {
            try {
                repository.setMembership(id, movieId, true).onSuccess {
                    // Keep the action guarded until the observed cache confirms membership.
                    state.first { it.membershipReadFailed || it.content == null || movieId in it.memberIds }
                }.onFailure {
                    state.update { it.copy(suggestionError = "Couldn't add this film. Try again.") }
                }
            } finally {
                state.update { it.copy(adding = it.adding - movieId) }
            }
        }
    }

    fun clearSuggestionError() {
        state.update { it.copy(suggestionError = null) }
    }
}
