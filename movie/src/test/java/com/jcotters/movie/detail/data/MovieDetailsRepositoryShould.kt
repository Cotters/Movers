package com.jcotters.movie.detail.data

import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieDao
import com.jcotters.movie.MovieApi
import com.jcotters.movie.detail.data.models.Genre
import com.jcotters.movie.detail.data.models.MovieDto
import com.jcotters.movie.detail.domain.IMovieDetailsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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
  fun `return failure with message when api returns null`() {
    coEvery { movieDao.getMovieById(any()) } returns null
    coEvery { movieApi.getMovieById(MOVIE_ID) } throws Throwable("Mocked error")

    val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

    coVerify(exactly = 1) { movieApi.getMovieById(MOVIE_ID) }
    val error = movie.exceptionOrNull()
    assertThat(error!!.message, equalTo("Could not find requested movie."))
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