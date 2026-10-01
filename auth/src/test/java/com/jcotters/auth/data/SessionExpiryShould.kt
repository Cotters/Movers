package com.jcotters.auth.data

import com.jcotters.auth.domain.UserSession
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiryShould {
    @Test
    fun `emit guest for absent or already expired sessions`() = runTest {
        val clock = mockk<Clock> { every { millis() } returns 1_000L }
        val states = MutableStateFlow(SessionState())
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { states.observeUserSession(clock).collect { observed += it } }
        runCurrent()
        states.value = SessionState(userId = 1, expiresAt = 1_000L)
        runCurrent()
        assertEquals(listOf(UserSession.NotAuthenticated), observed)
    }

    @Test
    fun `expire at the deadline without any preferences write`() = runTest {
        val clock = mockk<Clock> { every { millis() } answers { testScheduler.currentTime } }
        val states = MutableStateFlow(SessionState(userId = 1, expiresAt = 1_000L))
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { states.observeUserSession(clock).collect { observed += it } }
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertEquals(listOf(UserSession.Authenticated(1)), observed)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(UserSession.Authenticated(1), UserSession.NotAuthenticated), observed)
    }

    @Test
    fun `cancel the previous deadline when a session is replaced`() = runTest {
        val clock = mockk<Clock> { every { millis() } answers { testScheduler.currentTime } }
        val states = MutableStateFlow(SessionState(userId = 1, expiresAt = 1_000L))
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { states.observeUserSession(clock).collect { observed += it } }
        runCurrent()
        advanceTimeBy(500)
        states.value = SessionState(userId = 2, expiresAt = 2_000L)
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(UserSession.Authenticated(1), UserSession.Authenticated(2)), observed)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(UserSession.NotAuthenticated, observed.last())
    }

    @Test
    fun `recalculate remaining time if the clock moves backwards`() = runTest {
        var adjustment = 0L
        val clock = mockk<Clock> { every { millis() } answers { testScheduler.currentTime + adjustment } }
        val states = MutableStateFlow(SessionState(userId = 1, expiresAt = 1_000L))
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { states.observeUserSession(clock).collect { observed += it } }
        runCurrent()
        adjustment = -500L
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(UserSession.Authenticated(1)), observed)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(UserSession.NotAuthenticated, observed.last())
    }

    @Test
    fun `logout cancels the active session timer`() = runTest {
        val clock = mockk<Clock> { every { millis() } answers { testScheduler.currentTime } }
        val states = MutableStateFlow(SessionState(userId = 1, expiresAt = 1_000L))
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { states.observeUserSession(clock).collect { observed += it } }
        runCurrent()
        states.value = SessionState()
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(UserSession.Authenticated(1), UserSession.NotAuthenticated), observed)
    }
}
