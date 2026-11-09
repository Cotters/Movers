package com.jcotters.watchlists

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AddToListsState(
    val title: String = "", val lists: List<Watchlist> = emptyList(),
    val existing: Set<String> = emptySet(), val selected: Set<String> = emptySet(),
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null,
    val completed: Boolean = false, val createdId: String? = null,
) {
    val destinations: Set<String> get() = selected.intersect(lists.map { it.id }.toSet()) - existing
}

internal class AddToListsViewModel(
    private val repository: IWatchlistsRepository,
    private val movies: IMovieDetailsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(AddToListsState())
    val uiState = state.asStateFlow()
    private var movieId: Int? = null
    private var observation: Job? = null

    fun load(id: Int, initialListId: String?) {
        if (movieId == id && observation?.isActive == true) return
        observation?.cancel()
        movieId = id
        val selection = savedState.get<List<String>>("selected") ?: listOfNotNull(initialListId)
        state.update { it.copy(selected = selection.toSet(), loading = true, error = null) }
        observation = viewModelScope.launch {
            combine(repository.observeWatchlists(), repository.observeListIds(id), movies.observeMovieWithId(id)) { lists, existing, movie ->
                Triple(lists, existing, movie?.title.orEmpty())
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read your lists.") }
            }.collect { (lists, existing, title) ->
                state.update { it.copy(lists = lists, existing = existing, title = title, loading = false, selected = it.selected - existing) }
            }
        }
    }

    fun toggle(id: String) {
        if (state.value.busy || id in state.value.existing) return
        state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }
        saveSelection()
    }

    fun create(name: String) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.create(name).onSuccess { id ->
                    state.update { it.copy(selected = it.selected + id, createdId = id) }
                    saveSelection()
                }.onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally { state.update { it.copy(busy = false) } }
        }
    }

    fun add() {
        val id = movieId ?: return
        val lists = state.value.destinations
        if (state.value.busy || lists.isEmpty()) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.addMovieToLists(id, lists).onSuccess { state.update { it.copy(completed = true) } }
                    .onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally { state.update { it.copy(busy = false) } }
        }
    }

    fun consumeCreated() { state.update { it.copy(createdId = null) } }
    fun clearError() { state.update { it.copy(error = null) } }
    private fun saveSelection() { savedState["selected"] = state.value.selected.toList() }
}
