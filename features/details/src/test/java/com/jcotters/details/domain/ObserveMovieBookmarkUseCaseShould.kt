package com.jcotters.details.domain

import com.jcotters.contract.detail.domain.IBookmarksRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveMovieBookmarkUseCaseShould {
    @Test fun `observe device bookmarks without a user session`() = runTest {
        val repository = mockk<IBookmarksRepository>()
        val saved = MutableStateFlow(false)
        every { repository.observeMovieBookmark(1) } returns saved
        val observation = ObserveMovieBookmarkUseCase(repository)(1)
        assertFalse(observation.first())
        saved.value = true
        assertTrue(observation.first())
    }
}
