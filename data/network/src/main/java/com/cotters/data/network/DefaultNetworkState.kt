package com.cotters.data.network

/** Protect initial snapshots and handovers from overwriting newer default-network callbacks. */
internal class DefaultNetworkState<N>(private val publish: (NetworkStatus) -> Unit) {
    private var receivedCallback = false
    private var currentNetwork: N? = null

    @Synchronized
    fun initialize(network: N?, status: NetworkStatus) {
        if (!receivedCallback) {
            currentNetwork = network
            publish(status)
        }
    }

    @Synchronized
    fun capabilitiesChanged(network: N, status: NetworkStatus) {
        receivedCallback = true
        currentNetwork = network
        publish(status)
    }

    @Synchronized
    fun lost(network: N) {
        if (currentNetwork == network || (!receivedCallback && currentNetwork == null)) {
            receivedCallback = true
            currentNetwork = null
            publish(NetworkStatus.Offline)
        }
    }
}
