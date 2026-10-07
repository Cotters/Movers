package com.jcotters.internal.detail.data

import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.models.MovieDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailsRepositoryShould {
    private val api = mockk<MovieApi>()
    private val dao = mockk<MovieDao>(relaxed = true)
    private val mapper = MovieMapper()
    private var now = Duration.ofDays(2).toMillis()
    private val clock = mockk<Clock> { every { millis() } answers { now } }
    private var cached: DbMovie? = null
    private val savedMovie = MutableStateFlow<DbMovie?>(null)
    private val repository = MovieDetailsRepository(api, dao, mapper, clock)

    @Before
    fun setUp() {
        coEvery { dao.getMovieById(7) } answers { cached }
        every { dao.observeMovieById(7) } returns savedMovie
        coEvery { dao.saveMovieDetails(any()) } coAnswers {
            cached = firstArg()
            savedMovie.value = cached
        }
        coEvery { api.getMovieById(7) } returns MovieDto(id = 7, title = "Movie", runtime = 125)
    }

    @Test
    fun `fetch and persist details for a catalogue-only movie`() = runTest {
        cached = basicMovie
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        assertEquals(125, cached?.runtimeMinutes)
        assertEquals(now, cached?.detailsFetchedAt)
        coVerify(exactly = 1) { api.getMovieById(7) }
        assertEquals(mapper.toDomainModel(cached!!), repository.observeMovieWithId(7).first())
    }

    @Test
    fun `fetch and persist a movie absent from the database`() = runTest {
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        assertEquals(7, cached?.id)
        coVerify(exactly = 1) { dao.saveMovieDetails(any()) }
    }

    @Test
    fun `reuse details up to but not including 24 hours`() = runTest {
        cached = basicMovie.copy(detailsFetchedAt = now - Duration.ofHours(24).toMillis() + 1)
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        coVerify(exactly = 0) { api.getMovieById(any()) }
        now += 1
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        coVerify(exactly = 1) { api.getMovieById(7) }
    }

    @Test
    fun `refresh timestamps from the future`() = runTest {
        cached = basicMovie.copy(detailsFetchedAt = now + 1)
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        coVerify(exactly = 1) { api.getMovieById(7) }
    }

    @Test
    fun `missing optional fields do not repeatedly fetch a successful response`() = runTest {
        coEvery { api.getMovieById(7) } returns MovieDto(id = 7, title = "Movie")
        repeat(2) { assertTrue(repository.refreshMovieWithId(7).isSuccess) }
        assertEquals(now, cached?.detailsFetchedAt)
        assertNull(cached?.runtimeMinutes)
        coVerify(exactly = 1) { api.getMovieById(7) }
    }

    @Test
    fun `retain stale data and timestamp on network failure and allow retry`() = runTest {
        val original = basicMovie.copy(detailsFetchedAt = 1, tagline = "Saved tagline")
        cached = original
        val error = IOException("Offline")
        coEvery { api.getMovieById(7) } throws error
        val result = repository.refreshMovieWithId(7)
        assertSame(error, result.exceptionOrNull()?.cause)
        assertEquals(original, cached)
        coVerify(exactly = 0) { dao.saveMovieDetails(any()) }
        coEvery { api.getMovieById(7) } returns MovieDto(id = 7, title = "Movie")
        assertTrue(repository.refreshMovieWithId(7).isSuccess)
        assertEquals(now, cached?.detailsFetchedAt)
    }

    @Test
    fun `reject mismatched IDs and missing or blank titles without writing`() = runTest {
        for (dto in listOf(MovieDto(), MovieDto(id = 8, title = "Wrong"),
            MovieDto(id = 7), MovieDto(id = 7, title = " "))) {
            coEvery { api.getMovieById(7) } returns dto
            assertTrue(repository.refreshMovieWithId(7).isFailure)
        }
        coVerify(exactly = 0) { dao.saveMovieDetails(any()) }
    }

    @Test
    fun `report persistence failures without marking details fresh`() = runTest {
        coEvery { dao.saveMovieDetails(any()) } throws IOException("Disk full")
        assertTrue(repository.refreshMovieWithId(7).isFailure)
        assertNull(cached)
    }

    @Test
    fun `propagate cancellation during database and network access`() = runTest {
        val cancellation = CancellationException("Screen closed")
        coEvery { dao.getMovieById(7) } throws cancellation
        assertSame(cancellation, try { repository.refreshMovieWithId(7); null } catch (e: CancellationException) { e })
        coVerify(exactly = 0) { api.getMovieById(any()) }
        coEvery { dao.getMovieById(7) } returns null
        coEvery { api.getMovieById(7) } throws cancellation
        assertSame(cancellation, try { repository.refreshMovieWithId(7); null } catch (e: CancellationException) { e })
    }

    @Test
    fun `do not swallow fatal errors`() = runTest {
        coEvery { dao.getMovieById(7) } throws AssertionError("Broken invariant")
        assertThrows(AssertionError::class.java) {
            kotlinx.coroutines.runBlocking { repository.refreshMovieWithId(7) }
        }
    }

    @Test
    fun `concurrent refresh callers reuse the first persisted response`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { api.getMovieById(7) } coAnswers {
            gate.await()
            MovieDto(id = 7, title = "Movie")
        }
        val first = async { repository.refreshMovieWithId(7) }
        val second = async { repository.refreshMovieWithId(7) }
        runCurrent()
        coVerify(exactly = 1) { api.getMovieById(7) }
        gate.complete(Unit)
        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        coVerify(exactly = 1) { api.getMovieById(7) }
    }

    private val basicMovie = DbMovie(7, "Movie", "Overview", "2026-01-01")
}
