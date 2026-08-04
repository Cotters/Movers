package com.jcotters.internal.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.MovieMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
internal class MovieCatalogueRepository @Inject constructor(
    private val movieApi: MovieApi,
    private val remoteMediator: MovieRemoteMediator,
    private val movieDao: MovieDao,
    private val movieMapper: MovieMapper,
) : IMovieCatalogueRepository {

    private companion object {
        const val PAGE_SIZE: Int = 20
        const val PREFETCH_DISTANCE: Int = 50
    }

    override fun getPopularMoviesPaging(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE,
            ),
            remoteMediator = remoteMediator,
            pagingSourceFactory = { movieDao.getMoviesPagingSource() }
        )
            .flow
            .map { pagingData -> pagingData.map(movieMapper::toDomainModel) }
    }

    override suspend fun getPopularMovies(page: Int): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val movies = movieApi.getPopularMovies(page = page).results
//      val dbMovies = movieMapper.toDatabaseModel(movies.orEmpty())
//      movieDao.insertMovies(dbMovies)
            return@withContext movieMapper.toDomainModel(catalogueResults = movies.orEmpty())
        } catch (_: Throwable) {
            return@withContext emptyList()
        }
    }
}
