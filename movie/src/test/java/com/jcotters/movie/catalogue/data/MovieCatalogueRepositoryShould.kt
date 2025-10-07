package com.jcotters.movie.catalogue.data

import com.jcotters.movie.MovieApi
import com.jcotters.movie.catalogue.data.models.CatalogueMovieDto
import com.jcotters.movie.catalogue.data.models.CataloguePageResponse
import com.jcotters.movie.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.movie.detail.data.MovieMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovieCatalogueRepositoryShould {
  @RelaxedMockK
  private lateinit var movieApi: MovieApi

  private val movieMapper: MovieMapper = MovieMapper()

  private lateinit var underTest: IMovieCatalogueRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    underTest = MovieCatalogueRepository(movieApi = movieApi, movieMapper = movieMapper)
  }

  @Test
  fun `return empty list of popular movies when first page requested given result is empty`() {
    coEvery { movieApi.getPopularMovies(page = 1) } returns EMPTY_FIRST_PAGE

    val response = runBlocking { underTest.getPopularMovies(page = 1) }

    assertTrue(response.isEmpty())
    coVerify(exactly = 1) { movieApi.getPopularMovies(page = 1) }
  }

  @Test
  fun `return list of popular movies when first page requested`() {
    coEvery { movieApi.getPopularMovies(page = 1) } returns MOCK_FIRST_PAGE

    val response = runBlocking { underTest.getPopularMovies(page = 1) }

    assertTrue(response.isNotEmpty())
    (0..<3).forEach { index ->
      assertThat(response[index].id, equalTo(index))
    }
    coVerify(exactly = 1) { movieApi.getPopularMovies(page = 1) }
  }

  private companion object {
    val EMPTY_FIRST_PAGE = CataloguePageResponse(
      page = 1,
      results = emptyList(),
      totalPages = 0,
      totalResults = 0,
    )
    val MOCK_FIRST_PAGE = CataloguePageResponse(
      page = 1,
      results = listOf(
        CatalogueMovieDto(id = 0, title = "Movie 1", overview = "Movie 1"),
        CatalogueMovieDto(id = 1, title = "Movie 2", overview = "Movie 2"),
        CatalogueMovieDto(id = 2, title = "Movie 3", overview = "Movie 3"),
      ),
      totalPages = 1,
      totalResults = 3,
    )
  }
}