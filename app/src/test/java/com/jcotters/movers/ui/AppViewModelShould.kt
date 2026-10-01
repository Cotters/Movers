package com.jcotters.movers.ui

import androidx.lifecycle.viewModelScope
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val sessions = MutableSharedFlow<UserSession>()
    private val repository = mockk<IUserRepository>(relaxed = true) {
        every { userSession } returns sessions
    }
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = AppViewModel(repository)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `represent slow disk initialization as loading`() = runTest {
        runCurrent()
        assertEquals(UserSession.Loading, viewModel.userSession.value)
    }

    @Test
    fun `retain session updates even without a UI collector`() = runTest {
        runCurrent()
        sessions.emit(UserSession.Authenticated(7))
        runCurrent()
        assertEquals(UserSession.Authenticated(7), viewModel.userSession.value)
        sessions.emit(UserSession.NotAuthenticated)
        runCurrent()
        assertEquals(UserSession.NotAuthenticated, viewModel.userSession.value)
    }

    @Test
    fun `recheck persisted expiry on resume`() {
        viewModel.onResume()
        verify(exactly = 1) { repository.refreshSession() }
    }
}
