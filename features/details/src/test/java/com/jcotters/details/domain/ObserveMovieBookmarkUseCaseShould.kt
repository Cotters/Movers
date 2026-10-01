package com.jcotters.details.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.UserSession
import com.jcotters.contract.detail.domain.IBookmarksRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveMovieBookmarkUseCaseShould {
    @Test
    fun `switch users and stop observing bookmarks after logout`() = runTest {
        val session = MutableStateFlow<UserSession>(UserSession.Authenticated(1))
        val firstUserBookmark = MutableStateFlow(true)
        val secondUserBookmark = MutableStateFlow(false)
        val users = mockk<IUserRepository> { every { userSession } returns session }
        val bookmarks = mockk<IBookmarksRepository> {
            every { observeMovieBookmark(1, 7) } returns firstUserBookmark
            every { observeMovieBookmark(2, 7) } returns secondUserBookmark
        }
        val useCase = ObserveMovieBookmarkUseCase(users, bookmarks)
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch { useCase(7).collect { observed += it } }
        runCurrent()
        assertEquals(true, observed.last())
        session.value = UserSession.Authenticated(2)
        runCurrent()
        assertEquals(false, observed.last())
        session.value = UserSession.NotAuthenticated
        runCurrent()
        secondUserBookmark.value = true
        runCurrent()
        assertEquals(false, observed.last())
    }
}
