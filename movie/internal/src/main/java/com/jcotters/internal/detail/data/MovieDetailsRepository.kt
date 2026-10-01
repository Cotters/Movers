package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class MovieDetailsRepository @Inject constructor(
    private val api: MovieApi,
    private val movieDao: MovieDao,
    private val movieMapper: MovieMapper,
) : IMovieDetailsRepository {

    companion object {
        private const val NO_MOVIE_MESSAGE = "Could not find requested movie."
    }

    override suspend fun getMovieWithId(id: Int): Result<Movie> {
        return try {
            val dbMovie = movieDao.getMovieById(id)
            if (dbMovie != null) {
                return Result.success(movieMapper.toDomainModel(dbMovie))
            }
            val movieDto = api.getMovieById(movieId = id)
            val movie = movieMapper.toDomainModel(movieDto)
                ?: throw NoSuchElementException(NO_MOVIE_MESSAGE)
            Result.success(movie)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(IllegalStateException(NO_MOVIE_MESSAGE, error))
        }
    }
}
