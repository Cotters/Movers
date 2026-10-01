package com.cotters.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class DeviceNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val state = DefaultNetworkState<Network> { trySend(it) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                state.capabilitiesChanged(network, networkCapabilities.toNetworkStatus())
            }

            override fun onLost(network: Network) {
                state.lost(network)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        try {
            // Callbacks may never arrive when the app starts without a default network.
            val initialNetwork = connectivityManager.activeNetwork
            val capabilities = initialNetwork?.let(connectivityManager::getNetworkCapabilities)
            state.initialize(initialNetwork, capabilities.toNetworkStatus())
            awaitClose { }
        } finally {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.conflate().stabilizeNetworkStatus()
}

internal fun NetworkCapabilities?.toNetworkStatus(): NetworkStatus =
    if (this != null && hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    ) NetworkStatus.Online else NetworkStatus.Offline

/** Hide transient handover gaps; recovery always reaches the UI immediately. */
@OptIn(FlowPreview::class)
internal fun Flow<NetworkStatus>.stabilizeNetworkStatus(): Flow<NetworkStatus> =
    distinctUntilChanged().debounce { status ->
        if (status == NetworkStatus.Offline) 500L else 0L
    }
