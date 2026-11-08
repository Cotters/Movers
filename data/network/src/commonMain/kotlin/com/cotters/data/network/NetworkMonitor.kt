package com.cotters.data.network

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkMonitor {
    val networkStatus: Flow<NetworkStatus>
}

@OptIn(FlowPreview::class)
internal fun Flow<NetworkStatus>.stabilizeNetworkStatus(): Flow<NetworkStatus> =
    distinctUntilChanged().debounce { if (it == NetworkStatus.Offline) 500L else 0L }
