package com.cotters.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceNetworkMonitorShould {
    private val callback = slot<ConnectivityManager.NetworkCallback>()
    private val manager = mockk<ConnectivityManager> {
        every { registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
    }
    private val context = mockk<Context> {
        every { getSystemService(ConnectivityManager::class.java) } returns manager
    }

    @Test
    fun `startup without a network emits offline even without a callback`() = runTest {
        every { manager.activeNetwork } returns null
        val observed = mutableListOf<NetworkStatus>()
        val collection = backgroundScope.launch {
            DeviceNetworkMonitor(context).networkStatus.collect { observed += it }
        }
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Offline), observed)
        collection.cancel()
        runCurrent()
        verify(exactly = 1) { manager.unregisterNetworkCallback(callback.captured) }
    }

    @Test
    fun `startup on a validated network emits online immediately`() = runTest {
        val network = mockk<Network>()
        every { manager.activeNetwork } returns network
        every { manager.getNetworkCapabilities(network) } returns capabilities(internet = true, validated = true)
        val observed = mutableListOf<NetworkStatus>()
        backgroundScope.launch { DeviceNetworkMonitor(context).networkStatus.collect { observed += it } }
        runCurrent()
        assertEquals(listOf(NetworkStatus.Online), observed)
    }

    @Test
    fun `a callback arriving during registration takes precedence over the snapshot`() = runTest {
        val network = mockk<Network>()
        every { manager.activeNetwork } returns null
        every { manager.registerDefaultNetworkCallback(capture(callback)) } answers {
            callback.captured.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
        }
        val observed = mutableListOf<NetworkStatus>()
        backgroundScope.launch { DeviceNetworkMonitor(context).networkStatus.collect { observed += it } }
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Online), observed)
    }

    @Test
    fun `wifi without validated internet remains offline`() {
        assertEquals(NetworkStatus.Offline, capabilities(internet = true, validated = false).toNetworkStatus())
        assertEquals(NetworkStatus.Offline, capabilities(internet = false, validated = true).toNetworkStatus())
        assertEquals(NetworkStatus.Offline, (null as NetworkCapabilities?).toNetworkStatus())
    }

    private fun capabilities(internet: Boolean, validated: Boolean) = mockk<NetworkCapabilities> {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
    }
}
