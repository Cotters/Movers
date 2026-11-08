package com.jcotters.movers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.profile.domain.DeviceProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal enum class StartupState { Loading, Ready, Failed }

internal class AppViewModel(private val profiles: DeviceProfileRepository, networkMonitor: NetworkMonitor) : ViewModel() {
    private val startup = MutableStateFlow(StartupState.Loading)
    internal val startupState = startup.asStateFlow()
    private var initialization: Job? = null
    val networkStatus = networkMonitor.networkStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkStatus.Unknown)
    init { initializeProfile() }

    fun initializeProfile() {
        if (initialization?.isActive == true) return
        startup.value = StartupState.Loading
        initialization = viewModelScope.launch {
            try {
                profiles.initialize()
                startup.value = StartupState.Ready
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                startup.value = StartupState.Failed
            }
        }
    }
}
