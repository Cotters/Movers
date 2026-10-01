package com.cotters.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultNetworkStateShould {
    @Test
    fun `ignore a lost old network after handover to the new default`() {
        val observed = mutableListOf<NetworkStatus>()
        val state = DefaultNetworkState<String> { observed += it }
        state.capabilitiesChanged("wifi", NetworkStatus.Online)
        state.capabilitiesChanged("mobile", NetworkStatus.Online)
        state.lost("wifi")
        assertEquals(listOf(NetworkStatus.Online, NetworkStatus.Online), observed)
        state.lost("mobile")
        assertEquals(NetworkStatus.Offline, observed.last())
    }

    @Test
    fun `a startup snapshot cannot overwrite a more recent callback`() {
        val observed = mutableListOf<NetworkStatus>()
        val state = DefaultNetworkState<String> { observed += it }
        state.capabilitiesChanged("wifi", NetworkStatus.Online)
        state.initialize(null, NetworkStatus.Offline)
        assertEquals(listOf(NetworkStatus.Online), observed)
    }

    @Test
    fun `a startup snapshot cannot revive a network already lost`() {
        val observed = mutableListOf<NetworkStatus>()
        val state = DefaultNetworkState<String> { observed += it }
        state.lost("wifi")
        state.initialize("wifi", NetworkStatus.Online)
        assertEquals(listOf(NetworkStatus.Offline), observed)
    }

    @Test
    fun `ignore loss of a different network after initializing the default`() {
        val observed = mutableListOf<NetworkStatus>()
        val state = DefaultNetworkState<String> { observed += it }
        state.initialize("mobile", NetworkStatus.Online)
        state.lost("wifi")
        assertEquals(listOf(NetworkStatus.Online), observed)
    }
}
