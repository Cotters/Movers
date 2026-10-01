package com.cotters.data.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusShould {
    @Test
    fun `brief handover gaps do not reach the UI`() = runTest {
        val statuses = MutableSharedFlow<NetworkStatus>()
        val observed = mutableListOf<NetworkStatus>()
        backgroundScope.launch { statuses.stabilizeNetworkStatus().collect { observed += it } }
        runCurrent()
        statuses.emit(NetworkStatus.Online)
        runCurrent()
        statuses.emit(NetworkStatus.Offline)
        advanceTimeBy(400)
        statuses.emit(NetworkStatus.Online)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Online, NetworkStatus.Online), observed)
    }

    @Test
    fun `sustained loss is shown and recovery is immediate`() = runTest {
        val statuses = MutableSharedFlow<NetworkStatus>()
        val observed = mutableListOf<NetworkStatus>()
        backgroundScope.launch { statuses.stabilizeNetworkStatus().collect { observed += it } }
        runCurrent()
        statuses.emit(NetworkStatus.Offline)
        advanceTimeBy(499)
        runCurrent()
        assertEquals(emptyList<NetworkStatus>(), observed)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Offline), observed)
        statuses.emit(NetworkStatus.Online)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Offline, NetworkStatus.Online), observed)
    }

    @Test
    fun `repeated loss callbacks do not postpone the offline notice`() = runTest {
        val statuses = MutableSharedFlow<NetworkStatus>()
        val observed = mutableListOf<NetworkStatus>()
        backgroundScope.launch { statuses.stabilizeNetworkStatus().collect { observed += it } }
        runCurrent()
        statuses.emit(NetworkStatus.Offline)
        advanceTimeBy(400)
        statuses.emit(NetworkStatus.Offline)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(NetworkStatus.Offline), observed)
    }
}
