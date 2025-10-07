package com.jcotters.movie.data

import com.jcotters.movie.MovieApi
import com.jcotters.movie.detail.data.MovieDetailsRepository
import com.jcotters.movie.detail.data.MovieMapper
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

  private lateinit var underTest: IMovieDetailsRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    underTest = MovieDetailsRepository(
      api = movieApi,
      movieMapper = mapper,
    )
  }

  @Test
  fun `return movie when api returns movie`() {
    coEvery { movieApi.getMovieById(MOVIE_ID) } returns MOCKED_MOVIE_DTO

    val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

    coVerify(exactly = 1) { movieApi.getMovieById(MOVIE_ID) }
    assertThat(movie, equalTo(Result.success(MOCKED_MOVIE)))
  }

  @Test
  fun `return failure with message when api returns null`() {
    coEvery { movieApi.getMovieById(MOVIE_ID) } throws Throwable("Mocked error")

    val movie = runBlocking { underTest.getMovieWithId(MOVIE_ID) }

    coVerify(exactly = 1) { movieApi.getMovieById(MOVIE_ID) }
    val error = movie.exceptionOrNull()
    assertThat(error!!.message, equalTo("Could not find requested movie."))
  }

  companion object {
    private const val MOVIE_ID: Int = 123
    private const val MOVIE_TITLE = "Mock: The Movie"
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
    private val MOCKED_MOVIE = MovieMapper().toDomainModel(MOCKED_MOVIE_DTO)

  }
}