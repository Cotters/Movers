package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.models.Genre
import com.jcotters.internal.detail.data.models.MovieDto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovieDetailsRepositoryShould {

    private val mapper = MovieMapper()

    @RelaxedMockK
    private lateinit var movieApi: MovieApi

    @RelaxedMockK
    private lateinit var movieDao: MovieDao

    private lateinit var underTest: IMovieDetailsRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = MovieDetailsRepository(
            api = movieApi,
            movieDao = movieDao,
            movieMapper = mapper,
        )
    }

    @Test
    fun `return movie from database`() {
        coEvery { movieDao.getMovieById(MOVIE_ID) } returns DB_MOVIE

        val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }
        val mappedDbMovie = mapper.toDomainModel(DB_MOVIE)

        coVerify(exactly = 0) { movieApi.getMovieById(any()) }
        assertThat(movie.getOrNull()!!, equalTo(mappedDbMovie))
    }

    @Test
    fun `return failure with its cause when a network request fails`() {
        val failure = RuntimeException("Network unavailable")
        coEvery { movieDao.getMovieById(any()) } returns null
        coEvery { movieApi.getMovieById(MOVIE_ID) } throws failure

        val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

        coVerify(exactly = 1) { movieApi.getMovieById(MOVIE_ID) }
        val error = movie.exceptionOrNull()
        assertThat(error!!.message, equalTo("Could not find requested movie."))
        assertSame(failure, error.cause)
    }

    @Test
    fun `propagate cancellation during a database read without fetching remotely`() {
        val cancellation = CancellationException("Screen closed")
        coEvery { movieDao.getMovieById(MOVIE_ID) } throws cancellation

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { underTest.getMovieWithId(MOVIE_ID) }
        }

        assertSame(cancellation, thrown)
        coVerify(exactly = 0) { movieApi.getMovieById(any()) }
    }

    @Test
    fun `propagate cancellation during a network request`() {
        val cancellation = CancellationException("Screen closed")
        coEvery { movieDao.getMovieById(MOVIE_ID) } returns null
        coEvery { movieApi.getMovieById(MOVIE_ID) } throws cancellation

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { underTest.getMovieWithId(MOVIE_ID) }
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `report a missing movie when the response omits required fields`() {
        coEvery { movieDao.getMovieById(MOVIE_ID) } returns null
        coEvery { movieApi.getMovieById(MOVIE_ID) } returns MovieDto()

        val result = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

        assertTrue(result.isFailure)
        assertThat(result.exceptionOrNull()?.message, equalTo("Could not find requested movie."))
        assertTrue(result.exceptionOrNull()?.cause is NoSuchElementException)
    }

    @Test
    fun `do not turn fatal errors into a missing movie result`() {
        coEvery { movieDao.getMovieById(MOVIE_ID) } throws AssertionError("Broken invariant")

        assertThrows(AssertionError::class.java) {
            runBlocking { underTest.getMovieWithId(MOVIE_ID) }
        }
    }

    @Test
    fun `return movie when api returns movie`() {
        coEvery { movieDao.getMovieById(any()) } returns null
        coEvery { movieApi.getMovieById(MOVIE_ID) } returns MOCKED_MOVIE_DTO

        val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

        coVerify(exactly = 1) { movieApi.getMovieById(MOVIE_ID) }
        assertThat(movie, equalTo(Result.success(MOCKED_MOVIE)))
    }

    companion object {
        private const val MOVIE_ID: Int = 123
        private const val MOVIE_TITLE = "Mock: The DbMovie"
        private const val MOVIE_OVERVIEW = "The hilarious mocking of unit testing."
        private const val MOVIE_RELEASE_DATE = "2025/12/25"
        private const val MOVIE_REVENUE = 345245234
        private val MOVIE_GENRES = listOf(Genre(id = 12, name = "Comedy"))
        private val MOCKED_MOVIE_DTO = MovieDto(
            id = MOVIE_ID,
            title = MOVIE_TITLE,
            overview = MOVIE_OVERVIEW,
            releaseDate = MOVIE_RELEASE_DATE,
            revenue = MOVIE_REVENUE,
            genres = MOVIE_GENRES,
        )
        private val DB_MOVIE = DbMovie(
            id = MOVIE_ID,
            title = MOVIE_TITLE,
            synopsis = MOVIE_OVERVIEW,
            releaseDate = MOVIE_RELEASE_DATE,
        )

        private val MOCKED_MOVIE = MovieMapper().toDomainModel(MOCKED_MOVIE_DTO)

    }
}
