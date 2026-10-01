package com.jcotters.movers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cotters.data.network.DeviceNetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    networkMonitor: DeviceNetworkMonitor,
) : ViewModel() {

    val userSession: StateFlow<UserSession> = userRepository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSession.Loading,
    )
    val networkStatus = networkMonitor.networkStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
            initialValue = NetworkStatus.Unknown,
        )

    fun onResume() {
        userRepository.refreshSession()
    }
}
