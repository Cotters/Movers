package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.data.InvitePreview
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SharingState(
    val list: Watchlist? = null,
    val events: List<FilmActivity> = emptyList(),
    val preview: InvitePreview? = null,
    val invitation: String = "",
    val inviteUrl: String? = null,
    val joinedId: String? = null,
    val busy: Boolean = false,
    val moreActivity: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

internal class SharingViewModel(private val sharing: ISharedWatchlistsRepository, private val lists: IWatchlistsRepository) : ViewModel() {
    private val state = MutableStateFlow(SharingState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var activityObservation: Job? = null
    fun load(id: String, includeActivity: Boolean = false) {
        observation?.cancel(); activityObservation?.cancel()
        observation = viewModelScope.launch {
            lists.observeWatchlist(id).catch { e ->
                if (e is CancellationException) throw e
                state.update { it.copy(error = "Unable to read this list.") }
            }.collect { content -> state.update { it.copy(list = content?.watchlist) } }
        }
        activityObservation = viewModelScope.launch {
            sharing.observeActivity(id).catch { e ->
                if (e is CancellationException) throw e
                state.update { it.copy(error = "Unable to read activity.") }
            }.collect { events -> state.update { it.copy(events = events) } }
        }
        action {
            sharing.refresh(id).getOrThrow()
            if (includeActivity) { val more = sharing.loadActivity(id).getOrThrow(); state.update { it.copy(moreActivity = more) } }
        }
    }
    fun activity(id: String, older: Boolean = false) = action {
        val more = sharing.loadActivity(id,older).getOrThrow()
        state.update { it.copy(moreActivity = more) }
    }
    fun publish(id: String) = action { sharing.publish(id).getOrThrow(); state.update { it.copy(message = "This list is now shared.") } }
    fun invite(id: String) = action { val url = sharing.invite(id).getOrThrow(); state.update { it.copy(inviteUrl = url) } }
    fun consumeInvite() { state.update { it.copy(inviteUrl = null) } }
    fun revoke(id: String) = action { sharing.revokeInvites(id).getOrThrow(); state.update { it.copy(message = "Outstanding invitations revoked.") } }
    fun changeInvitation(value: String) { if (!state.value.busy) state.update { it.copy(invitation = value,preview = null,error = null) } }
    fun preview() = action { val preview = sharing.preview(state.value.invitation).getOrThrow(); state.update { it.copy(preview = preview) } }
    fun join() = action { val id = sharing.join(state.value.invitation).getOrThrow(); state.update { it.copy(joinedId = id) } }
    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true,error = null,message = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(error = e.message ?: "Unable to complete this shared change.") } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
}
