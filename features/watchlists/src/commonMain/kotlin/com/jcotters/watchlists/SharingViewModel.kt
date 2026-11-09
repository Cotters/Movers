package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.contract.watchlists.domain.WatchlistNotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SharingState(
    val list: Watchlist? = null,
    val events: List<FilmActivity> = emptyList(),
    val invitation: String = "",
    val openedFromLink: Boolean = false,
    val inviteUrl: String? = null,
    val joinedId: String? = null,
    val busy: Boolean = false,
    val moreActivity: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

internal class SharingViewModel(
    private val sharing: ISharedWatchlistsRepository,
    private val lists: IWatchlistsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(SharingState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var activityObservation: Job? = null
    private var pendingActivityListId: String? = null

    fun load(id: String, includeActivity: Boolean = false, sharingRequested: Boolean = false) {
        if (state.value.busy) return
        observation?.cancel()
        activityObservation?.cancel()
        pendingActivityListId = null
        action {
            if (sharingRequested) prepareSharing(id) else sharing.refresh(id).getOrThrow()
        }
        observation = viewModelScope.launch {
            var observedRevision: Long? = null
            lists.observeWatchlist(id).catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(error = "Unable to read this list.") }
            }.collect { content ->
                state.update { it.copy(list = content?.watchlist) }
                val revision = content?.watchlist?.shared?.revision
                if (includeActivity && revision != null && revision != observedRevision) {
                    observedRevision = revision
                    refreshActivityWhenIdle(id)
                }
            }
        }
        if (includeActivity) observeActivity(id)
    }

    private fun observeActivity(id: String) {
        activityObservation = viewModelScope.launch {
            sharing.observeActivity(id).catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(error = "Unable to read activity.") }
            }.collect { events -> state.update { it.copy(events = events) } }
        }
    }

    private suspend fun prepareSharing(id: String) {
        val list = lists.observeWatchlist(id).first()?.watchlist ?: throw WatchlistNotFoundException()
        if (list.shared == null) sharing.publish(id).getOrThrow() else sharing.refresh(id).getOrThrow()
    }

    private fun refreshActivityWhenIdle(id: String) {
        // Coalesce revisions arriving during another request; do not drop the latest refresh.
        if (state.value.busy) pendingActivityListId = id else activity(id)
    }

    fun activity(id: String, older: Boolean = false) = action {
        val more = sharing.loadActivity(id, older).getOrThrow()
        state.update { it.copy(moreActivity = more) }
    }

    fun invite(id: String) = action {
        val url = sharing.invite(id).getOrThrow()
        state.update { it.copy(inviteUrl = url) }
    }

    fun consumeInvite() { state.update { it.copy(inviteUrl = null) } }

    fun revoke(id: String) = action {
        sharing.revokeInvites(id).getOrThrow()
        state.update { it.copy(message = "Links revoked.") }
    }

    fun changeInvitation(value: String) {
        if (!state.value.busy) state.update { it.copy(invitation = value, error = null) }
    }

    fun openInvitation(value: String) {
        if (state.value.busy) return
        state.update { it.copy(invitation = value, openedFromLink = true) }
        join()
    }

    fun editInvitation() {
        if (!state.value.busy) state.update { it.copy(openedFromLink = false, error = null) }
    }

    fun join() = action {
        val id = sharing.join(state.value.invitation).getOrThrow()
        state.update { it.copy(joinedId = id, invitation = "") }
    }

    fun consumeJoined() { state.update { it.copy(joinedId = null) } }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                pendingActivityListId = null
                throw error
            } catch (error: Exception) {
                state.update { it.copy(error = error.message ?: "Unable to complete this shared change.") }
            } finally {
                state.update { it.copy(busy = false) }
                pendingActivityListId?.let { id ->
                    pendingActivityListId = null
                    activity(id)
                }
            }
        }
    }
}
