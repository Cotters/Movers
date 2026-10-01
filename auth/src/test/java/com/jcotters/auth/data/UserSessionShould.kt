package com.jcotters.auth.data

import com.jcotters.auth.domain.UserSession
import com.jcotters.database.user.UserDao
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserSessionShould {
    @Test
    fun `a failed disk read cannot authorize a user and is retried on resume`() = runTest {
        var readFails = true
        val clock = mockk<Clock> { every { millis() } returns 0L }
        val manager = mockk<SessionManager> {
            every { sessionState } returns flow {
                if (readFails) throw IOException("Disk temporarily unavailable")
                emit(SessionState(userId = 7, expiresAt = 1_000L))
            }
        }
        val repository = UserRepository(mockk(), mockk(), mockk<UserDao>(), manager, clock)
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { repository.userSession.collect { observed += it } }
        runCurrent()
        assertEquals(UserSession.NotAuthenticated, observed.last())
        readFails = false
        repository.refreshSession()
        runCurrent()
        assertEquals(UserSession.Authenticated(7), observed.last())
    }

    @Test
    fun `foreground recheck observes elapsed wall time before the timer resumes`() = runTest {
        var wallTime = 0L
        val clock = mockk<Clock> { every { millis() } answers { wallTime } }
        val persistedSession = MutableStateFlow(SessionState(userId = 7, expiresAt = 1_000L))
        val manager = mockk<SessionManager> { every { sessionState } returns persistedSession }
        val repository = UserRepository(mockk(), mockk(), mockk<UserDao>(), manager, clock)
        val observed = mutableListOf<UserSession>()
        backgroundScope.launch { repository.userSession.collect { observed += it } }
        runCurrent()
        assertEquals(UserSession.Authenticated(7), observed.last())

        // Android can suspend the process while real time keeps advancing.
        wallTime = 2_000L
        repository.refreshSession()
        runCurrent()
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(UserSession.NotAuthenticated, observed.last())
        assertEquals(SessionState(userId = 7, expiresAt = 1_000L), persistedSession.value)
    }

    @Test
    fun `one shot authorization checks persisted expiry rather than cached UI state`() = runTest {
        var wallTime = 0L
        val clock = mockk<Clock> { every { millis() } answers { wallTime } }
        val manager = mockk<SessionManager> {
            every { sessionState } returns MutableStateFlow(SessionState(userId = 7, expiresAt = 1_000L))
        }
        val repository = UserRepository(mockk(), mockk(), mockk<UserDao>(), manager, clock)
        assertEquals(7, repository.getUserIdOrNull())
        wallTime = 1_000L
        assertNull(repository.getUserIdOrNull())
    }
}
