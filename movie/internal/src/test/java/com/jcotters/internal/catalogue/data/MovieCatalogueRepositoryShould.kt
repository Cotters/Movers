package com.jcotters.internal.catalogue.data

import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieMapper
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before

class MovieCatalogueRepositoryShould {

    private val movieMapper: MovieMapper = MovieMapper()

    @RelaxedMockK
    private lateinit var movieDao: MovieDao

    @RelaxedMockK
    private lateinit var remoteMediator: PopularMoviesRemoteMediator

    private lateinit var underTest: IMovieCatalogueRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = MovieCatalogueRepository(
            movieMapper = movieMapper,
            movieDao = movieDao,
            popularMoviesRemoteMediator = remoteMediator
        )
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
                CatalogueMovieDto(id = 0, title = "DbMovie 1", overview = "DbMovie 1"),
                CatalogueMovieDto(id = 1, title = "DbMovie 2", overview = "DbMovie 2"),
                CatalogueMovieDto(id = 2, title = "DbMovie 3", overview = "DbMovie 3"),
            ),
            totalPages = 1,
            totalResults = 3,
        )
    }
}