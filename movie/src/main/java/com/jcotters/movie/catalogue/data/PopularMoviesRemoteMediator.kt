package com.jcotters.movie.catalogue.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.movie.MovieApi
import com.jcotters.movie.detail.data.MovieMapper
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator @Inject constructor(
  private val movieApi: MovieApi,
  private val database: MoversDatabase,
  private val movieMapper: MovieMapper,
) : RemoteMediator<Int, DbMovie>() {

  private val movieDao = database.movieDao()

  private companion object {
    const val START_PAGE = 1
  }

  override suspend fun initialize(): InitializeAction {
    return InitializeAction.LAUNCH_INITIAL_REFRESH
  }

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, DbMovie>
  ): MediatorResult {
    return try {
      val page = when (loadType) {
        LoadType.REFRESH -> START_PAGE
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

        LoadType.APPEND -> {
          movieDao.getLastPage()?.plus(1) ?: START_PAGE
        }
      }

//      delay(3000.milliseconds)
      Log.d("TJ", "Fetching from API (page=$page)")
      val response = movieApi.getPopularMovies(page = page)
      val movies = response.results.orEmpty().filterNotNull()
      val endOfPaginationReached = movies.isEmpty() || page >= (response.totalPages ?: 0)

      database.withTransaction {
        if (loadType == LoadType.REFRESH) {
          movieDao.clearAll()
        }
        val dbMovies = movieMapper.toDatabaseModel(movies = movies, page = page)
        movieDao.upsertAll(dbMovies)
      }
      MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
    } catch (e: IOException) {
      MediatorResult.Error(e)
    } catch (e: HttpException) {
      MediatorResult.Error(e)
    } catch (e: Exception) {
      MediatorResult.Error(e)
    }
  }
}