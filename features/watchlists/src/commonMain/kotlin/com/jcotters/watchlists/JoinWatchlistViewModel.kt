package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class JoinWatchlistState(
    val invitation: String = "",
    val openedFromLink: Boolean = false,
    val joinedId: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

/** Joining is entry-owned and independent of an existing list's sharing/activity state. */
internal class JoinWatchlistViewModel(private val sharing: ISharedWatchlistsRepository) : ViewModel() {
    private val state = MutableStateFlow(JoinWatchlistState())
    val uiState = state.asStateFlow()

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

    fun join() {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                recoverableResult {
                    val id = sharing.join(state.value.invitation).getOrThrow()
                    state.update { it.copy(joinedId = id, invitation = "") }
                }.onFailure { error ->
                    state.update { it.copy(error = error.message ?: "Unable to complete this shared change.") }
                }
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    fun consumeJoined() {
        state.update { it.copy(joinedId = null) }
    }
}
