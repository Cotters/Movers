package com.jcotters.movers.ui

import androidx.lifecycle.ViewModelStore
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.profile.domain.DeviceProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelShould {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val profiles = mockk<DeviceProfileRepository>()
    private val network = object : NetworkMonitor {
        override val networkStatus = flowOf(NetworkStatus.Offline)
    }

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun `wait for the device profile before opening the app and ignore duplicate startup requests`() = runTest {
        val initialized = CompletableDeferred<Unit>()
        coEvery { profiles.initialize() } coAnswers { initialized.await() }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.initializeProfile()
        assertEquals(StartupState.Loading, viewModel.startupState.value)
        coVerify(exactly = 1) { profiles.initialize() }

        initialized.complete(Unit)
        runCurrent()
        assertEquals(StartupState.Ready, viewModel.startupState.value)
    }

    @Test
    fun `recover from a failed local profile initialization when the user retries`() = runTest {
        var firstAttempt = true
        coEvery { profiles.initialize() } coAnswers {
            if (firstAttempt) {
                firstAttempt = false
                throw IllegalStateException("Storage unavailable")
            }
        }
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(StartupState.Failed, viewModel.startupState.value)

        viewModel.initializeProfile()
        assertEquals(StartupState.Loading, viewModel.startupState.value)
        runCurrent()
        assertEquals(StartupState.Ready, viewModel.startupState.value)
        coVerify(exactly = 2) { profiles.initialize() }
    }

    @Test
    fun `avoid reporting cancellation as a storage failure`() = runTest {
        coEvery { profiles.initialize() } throws CancellationException()
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(StartupState.Loading, viewModel.startupState.value)
    }

    private fun createViewModel() = AppViewModel(profiles, network).also { store.put("app", it) }
}
