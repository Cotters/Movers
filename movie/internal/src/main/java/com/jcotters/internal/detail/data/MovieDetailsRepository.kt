package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class MovieDetailsRepository @Inject constructor(
    val api: MovieApi,
    val movieDao: MovieDao,
    val movieMapper: MovieMapper,
) : IMovieDetailsRepository {

    companion object {
        private const val NO_MOVIE_MESSAGE = "Could not find requested movie."
    }

    override suspend fun getMovieWithId(id: Int): Result<Movie> = withContext(Dispatchers.IO) {
        try {
            val dbMovie = movieDao.getMovieById(id)
            if (dbMovie != null) {
                return@withContext Result.success(movieMapper.toDomainModel(dbMovie))
            }
            val movieDto = api.getMovieById(movieId = id)
            val movie = movieMapper.toDomainModel(movieDto)
            return@withContext if (movie != null) {
                Result.success(movie)
            } else {
                throw Throwable(NO_MOVIE_MESSAGE)
            }
        } catch (_: Throwable) {
            return@withContext Result.failure(Throwable(NO_MOVIE_MESSAGE))
        }
    }
}
