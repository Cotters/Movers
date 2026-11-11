package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.WatchlistContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ListTransferState(
    val loading: Boolean = true,
    val content: WatchlistContent? = null,
    val error: String? = null,
)

internal class ListTransferViewModel(
    private val repository: IWatchlistsRepository,
    private val removals: WatchlistRemovalCoordinator,
) : ViewModel() {
    private val state = MutableStateFlow(ListTransferState())
    val uiState = state.asStateFlow()
    private var listId: String? = null
    private var observation: Job? = null

    fun load(id: String) {
        if (id == listId && observation?.isActive == true) return
        observation?.cancel()
        listId = id
        state.value = ListTransferState()
        observation = viewModelScope.launch {
            combine(repository.observeWatchlist(id), removals.removals) { content, pending ->
                val hidden = pending.hiddenMovieIds(id)
                content?.copy(movies = content.movies.filterNot { it.id in hidden })
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read this list.") }
            }.collect { content -> state.value = ListTransferState(loading = false, content = content) }
        }
    }
}
