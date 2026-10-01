package com.jcotters.details.ui

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.details.domain.BookmarkMovieUseCase
import com.jcotters.details.domain.GetMovieByIdUseCase
import com.jcotters.details.domain.ObserveMovieBookmarkUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelShould {
    private val getMovie = mockk<GetMovieByIdUseCase>()
    private val observeBookmark = mockk<ObserveMovieBookmarkUseCase>()
    private val writeBookmark = mockk<BookmarkMovieUseCase>()
    private val savedBookmark = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { observeBookmark(7) } returns savedBookmark
        coEvery { getMovie.invoke(7) } returns Result.success(Movie(7, "Movie", "Overview", "2026"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ignore repeated taps while saving and wait for persisted status`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { writeBookmark.addBookmark(7) } coAnswers {
            gate.await()
            savedBookmark.value = true
            Result.success(Unit)
        }
        val model = MovieDetailViewModel(getMovie, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        repeat(3) { model.onViewEvent(MovieDetailViewEvent.BookmarkTapped(7)) }
        runCurrent()
        assertTrue(model.uiState.value.isSavingBookmark)
        assertFalse(model.uiState.value.isBookmarked)
        coVerify(exactly = 1) { writeBookmark.addBookmark(7) }
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.isBookmarked)
        assertFalse(model.uiState.value.isSavingBookmark)
    }

    @Test
    fun `leave the saved icon unchanged when a write fails`() = runTest {
        coEvery { writeBookmark.addBookmark(7) } returns Result.failure(IllegalStateException("Disk full"))
        val model = MovieDetailViewModel(getMovie, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        model.onViewEvent(MovieDetailViewEvent.BookmarkTapped(7))
        runCurrent()
        assertFalse(model.uiState.value.isBookmarked)
        assertFalse(model.uiState.value.isSavingBookmark)
    }

    @Test
    fun `reflect writes made elsewhere without reloading the screen`() = runTest {
        val model = MovieDetailViewModel(getMovie, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        savedBookmark.value = true
        runCurrent()
        assertTrue(model.uiState.value.isBookmarked)
        savedBookmark.value = false
        runCurrent()
        assertFalse(model.uiState.value.isBookmarked)
    }
}
