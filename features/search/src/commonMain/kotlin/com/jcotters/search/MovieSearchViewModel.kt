package com.jcotters.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SearchActionsState(
    val lists: List<Watchlist> = emptyList(), val loadingLists: Boolean = true,
    val saving: Set<Int> = emptySet(), val error: String? = null,
    val membershipLoading: Boolean = true, val membershipReadFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class MovieSearchViewModel(
    search: IMovieSearchRepository,
    private val lists: IWatchlistsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val query = savedState.getStateFlow("query", "")
    private val target = MutableStateFlow<String?>(null)
    private val membershipRetries = MutableStateFlow(0)
    private val submissions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val actions = MutableStateFlow(SearchActionsState())
    val actionsState = actions.asStateFlow()
    private var observation: Job? = null
    private val active = MutableStateFlow("")
    val searchedQuery = active.asStateFlow()

    val movies = merge(
        query.map(String::trim).distinctUntilChanged().transformLatest { value ->
            if (value.isNotEmpty()) delay(600)
            emit(value)
        },
        submissions,
    ).distinctUntilChanged().onEach { active.value = it }.flatMapLatest { value ->
        if (value.isBlank()) flowOf(PagingData.empty()) else search.searchMovies(value)
    }.cachedIn(viewModelScope)

    val addedIds = combine(target, membershipRetries) { id, _ -> id }.flatMapLatest { id ->
        (if (id == null) flowOf(emptySet()) else lists.observeMovieIds(id)).onStart {
            actions.update { it.copy(membershipLoading = true, membershipReadFailed = false) }
        }.onEach {
            actions.update { it.copy(membershipLoading = false) }
        }.catch { error ->
            if (error is CancellationException || error !is Exception) throw error
            actions.update {
                it.copy(
                    membershipLoading = false,
                    membershipReadFailed = true,
                    error = "Unable to read list membership."
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        retryLists()
    }

    fun setTarget(id: String?) {
        target.value = id
    }

    fun changeQuery(value: String) {
        savedState["query"] = value
    }

    fun submit() {
        query.value.trim().takeIf { it.isNotEmpty() }?.let(submissions::tryEmit)
    }

    fun clearError() {
        actions.update { it.copy(error = null) }
    }

    fun retryLists() {
        membershipRetries.update { it + 1 }
        if (observation?.isActive == true) return
        actions.update { it.copy(loadingLists = true) }
        observation = viewModelScope.launch {
            lists.observeWatchlists().catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                actions.update { it.copy(loadingLists = false, error = "Unable to read your lists.") }
            }.collect { rows -> actions.update { it.copy(lists = rows, loadingLists = false) } }
        }
    }

    fun setMembership(movieId: Int, added: Boolean) {
        val id = target.value ?: return
        val current = actions.value
        if (current.membershipLoading || current.membershipReadFailed || movieId in current.saving) return
        if (movieId in addedIds.value == added) return
        actions.update { it.copy(saving = it.saving + movieId, error = null) }
        viewModelScope.launch {
            try {
                if (current.lists.any { it.id == id && it.shared != null }) {
                    lists.setMembership(id, movieId, added).onSuccess { ack ->
                        actionsState.first { value ->
                            val list = value.lists.firstOrNull { it.id == id }
                            value.error != null || list == null || (list.shared?.revision ?: -1) >= (ack.revision ?: 0)
                        }
                    }.onFailure { error -> actions.update { it.copy(error = error.message) } }
                    return@launch
                }
                val result = if (added) lists.addMovieToLists(movieId, setOf(id)) else lists.removeMovie(id, movieId)
                result.onSuccess {
                    combine(addedIds, actionsState) { ids, state ->
                        state.membershipReadFailed || (!state.membershipLoading && movieId in ids == added)
                    }.first { it }
                }.onFailure { error -> actions.update { it.copy(error = error.message) } }
            } finally {
                actions.update { it.copy(saving = it.saving - movieId) }
            }
        }
    }
}
