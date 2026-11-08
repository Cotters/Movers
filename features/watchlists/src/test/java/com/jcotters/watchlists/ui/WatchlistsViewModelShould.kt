package com.jcotters.watchlists.ui

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistsViewModelShould {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `do not show auth required before session initialization finishes`() = runTest {
        val session = MutableStateFlow<UserSession>(UserSession.Loading)
        val repository = mockk<IUserRepository> { every { userSession } returns session }
        val model = WatchlistsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(WatchlistsViewState.Loading, model.uiState.value)
        session.value = UserSession.NotAuthenticated
        advanceUntilIdle()
        assertEquals(WatchlistsViewState.AuthRequired, model.uiState.value)
    }

    @Test
    fun `react to login and logout without exposing fictitious lists`() = runTest {
        val session = MutableStateFlow<UserSession>(UserSession.NotAuthenticated)
        val repository = mockk<IUserRepository> { every { userSession } returns session }
        val model = WatchlistsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(WatchlistsViewState.AuthRequired, model.uiState.value)

        session.value = UserSession.Authenticated(1)
        advanceUntilIdle()
        assertEquals(WatchlistsViewState.ComingSoon, model.uiState.value)

        session.value = UserSession.NotAuthenticated
        advanceUntilIdle()
        assertEquals(WatchlistsViewState.AuthRequired, model.uiState.value)
    }
}
