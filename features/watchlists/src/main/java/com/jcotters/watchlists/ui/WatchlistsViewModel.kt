package com.jcotters.watchlists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal enum class WatchlistsViewState { Loading, AuthRequired, ComingSoon }

@HiltViewModel
internal class WatchlistsViewModel @Inject constructor(userRepository: IUserRepository) : ViewModel() {
    val uiState = userRepository.userSession.map { session ->
        when (session) {
            UserSession.Loading -> WatchlistsViewState.Loading
            UserSession.NotAuthenticated -> WatchlistsViewState.AuthRequired
            is UserSession.Authenticated -> WatchlistsViewState.ComingSoon
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), WatchlistsViewState.Loading)
}
