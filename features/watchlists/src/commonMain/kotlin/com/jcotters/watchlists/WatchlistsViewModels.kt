package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.contract.watchlists.domain.WatchlistContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class WatchlistsState(
    val lists: List<Watchlist> = emptyList(), val loading: Boolean = true,
    val busy: Boolean = false, val error: String? = null, val createdId: String? = null,
)

internal class WatchlistsViewModel(private val repository: IWatchlistsRepository) : ViewModel() {
    private val state = MutableStateFlow(WatchlistsState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    init { retry() }

    fun retry() {
        if (observation?.isActive == true) return
        state.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            repository.observeWatchlists().catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read your lists.") }
            }.collect { lists -> state.update { it.copy(lists = lists, loading = false) } }
        }
    }

    fun create(name: String) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.create(name).onSuccess { id -> state.update { it.copy(createdId = id) } }
                    .onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally { state.update { it.copy(busy = false) } }
        }
    }

    fun consumeCreated() { state.update { it.copy(createdId = null) } }
    fun clearError() { state.update { it.copy(error = null) } }
}

internal data class WatchlistDetailState(
    val content: WatchlistContent? = null, val loading: Boolean = true, val busy: Boolean = false,
    val error: String? = null, val deleted: Boolean = false, val renamed: Boolean = false,
    val lists: List<Watchlist> = emptyList(),
)

internal class WatchlistDetailViewModel(private val repository: IWatchlistsRepository) : ViewModel() {
    private val state = MutableStateFlow(WatchlistDetailState())
    val uiState = state.asStateFlow()
    private var listId: String? = null
    private var observation: Job? = null

    fun load(id: String) {
        if (id == listId && observation?.isActive == true) return
        observation?.cancel()
        listId = id
        state.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            combine(repository.observeWatchlist(id), repository.observeWatchlists()) { content, lists -> content to lists }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read this list.") }
            }.collect { (content, lists) -> state.update { it.copy(content = content, lists = lists, loading = false) } }
        }
    }

    fun rename(name: String) = write(renaming = true) { id -> repository.rename(id, name) }
    fun removeMovie(movieId: Int) = write { id -> repository.removeMovie(id, movieId) }
    fun delete() = write(deleting = true) { id -> repository.delete(id) }
    fun clearError() { state.update { it.copy(error = null) } }
    fun consumeRenamed() { state.update { it.copy(renamed = false) } }

    private fun write(deleting: Boolean = false, renaming: Boolean = false, action: suspend (String) -> Result<Unit>) {
        val id = listId ?: return
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                action(id).onSuccess { state.update { it.copy(deleted = deleting, renamed = renaming) } }
                    .onFailure { error -> state.update { it.copy(error = error.message) } }
            } finally { state.update { it.copy(busy = false) } }
        }
    }
}
