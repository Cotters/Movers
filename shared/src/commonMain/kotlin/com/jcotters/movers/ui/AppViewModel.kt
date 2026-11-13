package com.jcotters.movers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.result.recoverableResult
import com.jcotters.profile.domain.DeviceProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal enum class StartupState { Loading, NameRequired, Ready, Failed }

internal class AppViewModel(private val profiles: DeviceProfileRepository, networkMonitor: NetworkMonitor) :
    ViewModel() {
    private val startup = MutableStateFlow(StartupState.Loading)
    internal val startupState = startup.asStateFlow()
    private var initialization: Job? = null
    val nameError = MutableStateFlow<String?>(null)
    val suggestedName = MutableStateFlow("")
    val savingName = MutableStateFlow(false)
    fun saveName(name: String) {
        if (savingName.value) return
        savingName.value = true
        viewModelScope.launch {
            try {
                recoverableResult {
                    profiles.saveName(name)
                    startup.value = StartupState.Ready
                    nameError.value = null
                }.onFailure { nameError.value = it.message ?: "Unable to save your name." }
            } finally {
                savingName.value = false
            }
        }
    }

    val networkStatus = networkMonitor.networkStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NetworkStatus.Unknown
    )

    init {
        initializeProfile()
    }

    fun initializeProfile() {
        if (initialization?.isActive == true) return
        startup.value = StartupState.Loading
        initialization = viewModelScope.launch {
            recoverableResult {
                profiles.initialize()
                val profile = profiles.profile.first()
                suggestedName.value = profile?.displayName.orEmpty()
                startup.value = if (profile?.nameConfirmed == true) StartupState.Ready else StartupState.NameRequired
            }.onFailure { startup.value = StartupState.Failed }
        }
    }
}
