package com.cotters.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class DeviceNetworkMonitor constructor(
    context: Context,
) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
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

