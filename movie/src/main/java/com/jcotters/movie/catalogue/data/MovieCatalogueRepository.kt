package com.jcotters.movie.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.jcotters.database.movies.MovieDao
import com.jcotters.movie.MovieApi
import com.jcotters.movie.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.movie.detail.data.MovieMapper
import com.jcotters.movie.detail.domain.models.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class MovieCatalogueRepository @Inject constructor(
  private val movieApi: MovieApi,
  private val remoteMediator: MovieRemoteMediator,
  private val movieDao: MovieDao,
  private val movieMapper: MovieMapper,
) : IMovieCatalogueRepository {

  override fun getPopularMoviesPaging(): Flow<PagingData<Movie>> {
    return Pager(
      config = PagingConfig(
        pageSize = 20,
        enablePlaceholders = false,
        prefetchDistance = 5,
        initialLoadSize = 20
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