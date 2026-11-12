package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.watchlists.preferences.MaximumPinnedWatchlists
import com.jcotters.watchlists.preferences.WatchlistPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class WatchlistsState(
    val lists: List<Watchlist> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
    val syncError: String? = null,
    val pinnedIds: List<String> = emptyList(),
    val pinning: Set<String> = emptySet(),
    val preferencesReady: Boolean = false,
    val preferenceError: String? = null,
) {
    val pinnedLists get() = pinnedIds.mapNotNull { id -> lists.firstOrNull { it.id == id } }
    val otherLists get() = lists.filterNot { it.id in pinnedIds }
    val canPinMore get() = pinnedIds.size < MaximumPinnedWatchlists
}

internal class WatchlistsViewModel(
    private val repository: IWatchlistsRepository,
    private val sharing: ISharedWatchlistsRepository,
    private val preferences: WatchlistPreferences,
) : ViewModel() {
    private val state = MutableStateFlow(WatchlistsState())
    val uiState = combine(state, preferences.state) { content, saved ->
        content.copy(pinnedIds = saved.pinnedIds, preferencesReady = saved.ready,
            loading = content.loading || (!saved.ready && content.preferenceError == null))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)
    private var observation: Job? = null
    private var failedPin: Pair<String, Boolean>? = null

    init {
        retry()
        viewModelScope.launch { sharing.syncStatus.collect { status -> state.update { it.copy(syncError = status.error) } } }
    }

    fun refreshShared() { viewModelScope.launch { sharing.refresh() } }

    fun retryPreferences() {
        failedPin?.let { (id, pinned) -> setPinned(id, pinned); return }
        viewModelScope.launch {
            val result = if (state.value.loading) preferences.load()
                else preferences.retainLists(state.value.lists.map { it.id }.toSet())
            result.onSuccess { state.update { it.copy(preferenceError = null) } }
                .onFailure { state.update { it.copy(preferenceError = "Couldn't read saved pins. Try again.") } }
        }
    }

    fun retry() {
        retryPreferences()
        if (observation?.isActive == true) return
        state.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            repository.observeWatchlists().catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read your lists.") }
            }.collect { lists ->
                state.update { it.copy(lists = lists, loading = false) }
                // Deleted/left lists release pin slots and discard their local sort settings.
                preferences.retainLists(lists.map { it.id }.toSet()).onFailure {
                    state.update { it.copy(preferenceError = "Couldn't update saved pins. Try again.") }
                }
            }
        }
    }

    fun setPinned(id: String, pinned: Boolean) {
        val current = state.value
        if (current.busy || id in current.pinning || current.lists.none { it.id == id }) return
        state.update { it.copy(pinning = it.pinning + id, preferenceError = null) }
        viewModelScope.launch {
            try {
                preferences.setPinned(id, pinned).onSuccess { failedPin = null }.onFailure { error ->
                    failedPin = id to pinned
                    val message = if (error is IllegalArgumentException) error.message else "Couldn't save pins. Try again."
                    state.update { it.copy(preferenceError = message) }
                }
            } finally { state.update { it.copy(pinning = it.pinning - id) } }
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
