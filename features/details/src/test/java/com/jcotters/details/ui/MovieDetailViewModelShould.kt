package com.jcotters.details.ui

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.details.domain.BookmarkMovieUseCase
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelShould {
    private val movieRepository = mockk<IMovieDetailsRepository>()
    private val savedMovie = MutableStateFlow<Movie?>(Movie(7, "Movie", "Overview", "2026"))
    private val observeBookmark = mockk<ObserveMovieBookmarkUseCase>()
    private val writeBookmark = mockk<BookmarkMovieUseCase>()
    private val savedBookmark = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { observeBookmark(7) } returns savedBookmark
        every { movieRepository.observeMovieWithId(7) } returns savedMovie
        coEvery { movieRepository.refreshMovieWithId(7) } returns Result.success(Unit)
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
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
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
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        model.onViewEvent(MovieDetailViewEvent.BookmarkTapped(7))
        runCurrent()
        assertFalse(model.uiState.value.isBookmarked)
        assertFalse(model.uiState.value.isSavingBookmark)
    }

    @Test
    fun `reflect writes made elsewhere without reloading the screen`() = runTest {
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        savedBookmark.value = true
        runCurrent()
        assertTrue(model.uiState.value.isBookmarked)
        savedBookmark.value = false
        runCurrent()
        assertFalse(model.uiState.value.isBookmarked)
    }

    @Test
    fun `load cached content from Room while the remote request is suspended`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { movieRepository.refreshMovieWithId(7) } coAnswers { gate.await(); Result.success(Unit) }
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        assertTrue(model.uiState.value.isLoading)
        runCurrent()
        assertEquals(savedMovie.value, model.uiState.value.movie)
        assertFalse(model.uiState.value.isLoading)
        assertTrue(model.uiState.value.isRefreshingDetails)
        gate.complete(Unit)
        runCurrent()
        assertFalse(model.uiState.value.isRefreshingDetails)
    }

    @Test
    fun `wait for the first Room emission when fresh-cache refresh finishes immediately`() = runTest {
        val gate = CompletableDeferred<Unit>()
        every { movieRepository.observeMovieWithId(7) } returns kotlinx.coroutines.flow.flow { gate.await(); emit(savedMovie.value) }
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        assertTrue(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.isRefreshingDetails)
        assertNull(model.uiState.value.movie)
        gate.complete(Unit)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(savedMovie.value, model.uiState.value.movie)
    }

    @Test
    fun `observe enriched database updates independently from bookmark updates`() = runTest {
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        savedMovie.value = savedMovie.value!!.copy(runtimeMinutes = 125, tagline = "Loaded", detailsFetchedAt = 1)
        savedBookmark.value = true
        runCurrent()
        assertEquals(125, model.uiState.value.movie?.runtimeMinutes)
        assertEquals("Loaded", model.uiState.value.movie?.tagline)
        assertTrue(model.uiState.value.isBookmarked)
    }

    @Test
    fun `retain content on failure and guard repeated loads and retry taps`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { movieRepository.refreshMovieWithId(7) } returns Result.failure(IllegalStateException("Offline"))
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        assertEquals(savedMovie.value, model.uiState.value.movie)
        assertNotNull(model.uiState.value.detailsError)
        assertFalse(model.uiState.value.isRefreshingDetails)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        coVerify(exactly = 1) { movieRepository.refreshMovieWithId(7) }
        coEvery { movieRepository.refreshMovieWithId(7) } coAnswers { gate.await(); Result.success(Unit) }
        repeat(3) { model.onViewEvent(MovieDetailViewEvent.RetryDetails) }
        runCurrent()
        coVerify(exactly = 2) { movieRepository.refreshMovieWithId(7) }
        assertNull(model.uiState.value.detailsError)
        gate.complete(Unit)
        runCurrent()
        assertFalse(model.uiState.value.isRefreshingDetails)
    }

    @Test
    fun `switch IDs without allowing an old request to replace new content`() = runTest {
        var cancelled = false
        coEvery { movieRepository.refreshMovieWithId(7) } coAnswers {
            try { kotlinx.coroutines.awaitCancellation() } finally { cancelled = true }
        }
        val secondMovie = Movie(8, "Second movie", "Overview", "2027")
        every { movieRepository.observeMovieWithId(8) } returns MutableStateFlow(secondMovie)
        every { observeBookmark(8) } returns MutableStateFlow(false)
        coEvery { movieRepository.refreshMovieWithId(8) } returns Result.success(Unit)
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        runCurrent()
        model.onViewEvent(MovieDetailViewEvent.OnLoad(8))
        runCurrent()
        assertTrue(cancelled)
        assertEquals(secondMovie, model.uiState.value.movie)
        assertFalse(model.uiState.value.isRefreshingDetails)
        savedMovie.value = savedMovie.value!!.copy(title = "Old request")
        runCurrent()
        assertEquals(secondMovie, model.uiState.value.movie)
    }

    @Test
    fun `handle no-cache failure using only the requested ID`() = runTest {
        savedMovie.value = null
        coEvery { movieRepository.refreshMovieWithId(7) } returns Result.failure(IllegalStateException("Offline"))
        val model = MovieDetailViewModel(movieRepository, observeBookmark, writeBookmark)
        model.onViewEvent(MovieDetailViewEvent.OnLoad(7))
        assertNull(model.uiState.value.movie)
        runCurrent()
        assertNull(model.uiState.value.movie)
        assertFalse(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.isRefreshingDetails)
        assertNotNull(model.uiState.value.detailsError)
    }

}
