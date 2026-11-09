package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AddToListsState(
    val title: String = "",
    val lists: List<Watchlist> = emptyList(),
    val existing: Set<String> = emptySet(),
    val saving: Set<String> = emptySet(),
    val loading: Boolean = true,
    val creating: Boolean = false,
    val readFailed: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
) {
    val busy: Boolean get() = creating || saving.isNotEmpty()
}

internal class AddToListsViewModel(
    private val repository: IWatchlistsRepository,
    private val movies: IMovieDetailsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(AddToListsState())
    val uiState = state.asStateFlow()
    private var movieId: Int? = null
    private var observation: Job? = null

    fun load(id: Int) {
        if (movieId == id && observation?.isActive == true) return
        observation?.cancel()
        movieId = id
        state.update { it.copy(loading = true, readFailed = false, error = null) }
        observation = viewModelScope.launch {
            combine(repository.observeWatchlists(), repository.observeListIds(id), movies.observeMovieWithId(id)) { lists, existing, movie ->
                Triple(lists, existing, movie?.title.orEmpty())
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, readFailed = true, error = "Unable to read your lists.") }
            }.collect { (lists, existing, title) ->
                state.update { it.copy(lists = lists, existing = existing, title = title, loading = false) }
            }
        }
    }

    fun setMembership(listId: String, added: Boolean) {
        val id = movieId ?: return
        val current = state.value
        if (current.loading || current.readFailed || current.creating || listId in current.saving) return
        if (current.lists.none { it.id == listId } || (listId in current.existing) == added) return
        state.update { it.copy(saving = it.saving + listId, error = null) }
        viewModelScope.launch {
            try {
                updateMembership(id, listId, added)
            } finally {
                state.update { it.copy(saving = it.saving - listId) }
            }
        }
    }

    fun create(name: String) {
        val id = movieId ?: return
        if (state.value.busy || state.value.loading || state.value.readFailed) return
        state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            try {
                repository.create(name).onSuccess { listId ->
                    state.update { it.copy(createdId = listId) }
                    updateMembership(id, listId, added = true)
                }.onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally {
                state.update { it.copy(creating = false) }
            }
        }
    }

    private suspend fun updateMembership(movieId: Int, listId: String, added: Boolean) {
        val previouslyListed = state.value.lists.any { it.id == listId }
        val result = if (added) repository.addMovieToLists(movieId, setOf(listId)) else repository.removeMovie(listId, movieId)
        result.onSuccess {
            // Keep the row busy until persisted observation confirms the new checkbox state.
            state.first { it.readFailed || (previouslyListed && it.lists.none { list -> list.id == listId }) || (listId in it.existing) == added }
        }.onFailure { error -> state.update { it.copy(error = error.message) } }
    }

    fun consumeCreated() { state.update { it.copy(createdId = null) } }
    fun clearError() { state.update { it.copy(error = null) } }
}
