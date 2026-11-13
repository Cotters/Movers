package com.jcotters.watchlists

import androidx.lifecycle.SavedStateHandle
import com.jcotters.watchlists.preferences.WatchlistPreferences
import com.jcotters.watchlists.preferences.WatchlistSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class WatchlistSorting(
    private val preferences: WatchlistPreferences,
    private val state: MutableStateFlow<WatchlistDetailState>,
    private val savedState: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    private var failedSort: WatchlistSort? = null

    fun retry() {
        failedSort?.let {
            change(it)
            return
        }
        scope.launch {
            preferences.load().onSuccess { state.update { it.copy(sortError = null) } }
                .onFailure { state.update { it.copy(sortError = "Couldn't read saved sorting. Try again.") } }
        }
    }

    fun change(sort: WatchlistSort) {
        val id = state.value.listId ?: return
        if (state.value.sortSaving) return
        state.update { it.copy(sortSaving = true, sortError = null) }
        failedSort = sort
        scope.launch {
            try {
                preferences.setSort(id, sort).onSuccess {
                    failedSort = null
                    savedState["notificationRecentOverride"] = false
                    state.update { it.copy(recentOverride = false) }
                }.onFailure {
                    state.update { it.copy(sortError = "Couldn't save sorting. Try again.") }
                }
            } finally {
                state.update { it.copy(sortSaving = false) }
            }
        }
    }
}
