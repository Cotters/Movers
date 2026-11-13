package com.jcotters.watchlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.filter
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.watchlists.preferences.WatchlistPreferences
import com.jcotters.watchlists.preferences.WatchlistSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class WatchlistDetailState(
    val content: WatchlistContent? = null, val loading: Boolean = true, val busy: Boolean = false,
    val error: String? = null, val deleted: Boolean = false, val renamed: Boolean = false,
    val lists: List<Watchlist> = emptyList(),
    val removing: Set<Int> = emptySet(),
    val query: String = "",
    val listId: String? = null,
    val sort: WatchlistSort = WatchlistSort(),
    val preferencesReady: Boolean = false,
    val sortSaving: Boolean = false,
    val sortError: String? = null,
    val memberIds: Set<Int> = emptySet(),
    val hiddenIds: Set<Int> = emptySet(),
    val membershipReadFailed: Boolean = false,
    val suggestionQuery: String = "",
    val adding: Set<Int> = emptySet(),
    val suggestionError: String? = null,
) {
    val waitingForSuggestions get() = query.trim() != suggestionQuery
    val canAddSuggestions get() = !loading && !busy && !membershipReadFailed && content != null
    val excludedSuggestionIds: Set<Int> = memberIds + hiddenIds
    val visibleMovies: List<com.jcotters.contract.detail.domain.models.Movie>
        get() {
            val movies = content?.movies.orEmpty()
            val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
            val matches = if (terms.isEmpty()) movies else movies.filter { movie ->
                terms.all { movie.title.contains(it, ignoreCase = true) }
            }
            return sort.apply(matches)
        }
}

internal class WatchlistDetailViewModel(
    private val repository: IWatchlistsRepository,
    private val sharing: com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository,
    private val removals: WatchlistRemovalCoordinator,
    savedState: SavedStateHandle,
    preferences: WatchlistPreferences,
    search: IMovieSearchRepository,
) : ViewModel() {
    private val state = MutableStateFlow(WatchlistDetailState())
    val sorting = WatchlistSorting(preferences, state, viewModelScope)
    val filmSearch = WatchlistFilmSearch(repository, search, state, savedState, viewModelScope)
    val uiState = combine(state, filmSearch.query, preferences.state) { content, query, saved ->
        content.copy(
            query = query,
            sort = content.listId?.let { saved.sorts[it] } ?: WatchlistSort(),
            preferencesReady = saved.ready)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        state.value.copy(query = savedState["query"] ?: "")
    )

    val suggestions = filmSearch.suggestions

    private var listId: String? = null
    private var observation: Job? = null

    fun load(id: String) {
        sorting.retry()
        if (id == listId && observation?.isActive == true) return
        observation?.cancel()
        listId = id
        state.update { it.copy(listId = id, loading = true, error = null) }
        observation = viewModelScope.launch {
            combine(
                repository.observeWatchlist(id),
                repository.observeWatchlists(),
                removals.removals
            ) { content, lists, pending ->
                val hidden = pending.hiddenMovieIds(id)
                ObservedWatchlist(
                    content?.copy(movies = content.movies.filterNot { it.id in hidden }), lists,
                    content?.movies.orEmpty().map { it.id }.toSet(), hidden,
                )
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update {
                    it.copy(
                        loading = false,
                        membershipReadFailed = true,
                        error = "Unable to read this list."
                    )
                }
            }.collect { observed ->
                state.update {
                    it.copy(
                        content = observed.content, lists = observed.lists, memberIds = observed.memberIds,
                        hiddenIds = observed.hiddenIds, membershipReadFailed = false, loading = false
                    )
                }
            }
        }
    }

    private data class ObservedWatchlist(
        val content: WatchlistContent?,
        val lists: List<Watchlist>,
        val memberIds: Set<Int>,
        val hiddenIds: Set<Int>,
    )

    fun rename(name: String) = write(renaming = true) { id -> repository.rename(id, name) }
    fun pickMovie(): Int? {
        val current = state.value
        if (current.loading || current.busy) return null
        val movies = current.content?.movies.orEmpty().filterNot { it.id in current.removing }
        return pickWatchlistMovie(movies)?.id
    }

    fun stageMovieRemoval(movieId: Int) {
        val id = listId ?: return
        val current = state.value
        if (current.busy || movieId in current.removing) return
        val movie = current.content?.movies?.firstOrNull { it.id == movieId } ?: return
        removals.stage(id, movie)
    }

    fun removeMovie(movieId: Int) {
        val id = listId ?: return
        if (state.value.busy || movieId in state.value.removing) return
        state.update { it.copy(removing = it.removing + movieId, error = null) }
        viewModelScope.launch {
            try {
                repository.removeMovie(id, movieId)
                    .onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally {
                state.update { it.copy(removing = it.removing - movieId) }
            }
        }
    }

    fun leave() = write(deleting = true) { id -> sharing.leave(id) }
    fun delete() = write(deleting = true) { id -> repository.delete(id) }
    fun clearError() {
        state.update { it.copy(error = null) }
    }

    fun consumeRenamed() {
        state.update { it.copy(renamed = false) }
    }

    private fun write(deleting: Boolean = false, renaming: Boolean = false, action: suspend (String) -> Result<Unit>) {
        val id = listId ?: return
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                action(id).onSuccess { state.update { it.copy(deleted = deleting, renamed = renaming) } }
                    .onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }
}
