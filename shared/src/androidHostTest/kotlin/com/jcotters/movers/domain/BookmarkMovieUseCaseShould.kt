package com.jcotters.movers.domain

import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.details.domain.BookmarkMovieUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkMovieUseCaseShould {
    @Test fun `save a bookmark without an account`() = runTest {
        val repository = mockk<IBookmarksRepository>()
        coEvery { repository.addBookmark(12) } returns Result.success(Unit)
        assertTrue(BookmarkMovieUseCase(repository).addBookmark(12).isSuccess)
        coVerify(exactly = 1) { repository.addBookmark(12) }
    }
    @Test fun `return a persistence failure when removing a bookmark`() = runTest {
        val repository = mockk<IBookmarksRepository>()
        val error = IllegalStateException("disk unavailable")
        coEvery { repository.removeBookmark(12) } returns Result.failure(error)
        assertSame(error, BookmarkMovieUseCase(repository).removeBookmark(12).exceptionOrNull())
    }
}
