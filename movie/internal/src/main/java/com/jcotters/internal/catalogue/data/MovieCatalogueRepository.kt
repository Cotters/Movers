package com.jcotters.internal.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.detail.data.MovieMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
internal class MovieCatalogueRepository @Inject constructor(
    private val popularMoviesRemoteMediator: PopularMoviesRemoteMediator,
    private val movieDao: MovieDao,
    private val movieMapper: MovieMapper,
) : IMovieCatalogueRepository {

    private companion object {
        const val PAGE_SIZE: Int = 20
        const val PREFETCH_DISTANCE: Int = 10
    }

    override fun getPopularMoviesPaging(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE,
            ),
            remoteMediator = popularMoviesRemoteMediator,
            pagingSourceFactory = { movieDao.getPopularMoviesPagingSource(MovieCatalogue.Popular.key) }
        )
            .flow
            .map { pagingData -> pagingData.map(movieMapper::toDomainModel) }
    }
}
