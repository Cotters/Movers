package com.jcotters.movie.detail.data

import com.jcotters.movie.MovieApi
import com.jcotters.movie.detail.domain.IMovieDetailsRepository
import com.jcotters.movie.detail.domain.models.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MovieDetailsRepository @Inject constructor(
  val api: MovieApi,
  val movieMapper: MovieMapper,
) : IMovieDetailsRepository {

  companion object {
    private const val NO_MOVIE_MESSAGE = "Could not find requested movie."
  }

  override suspend fun getMovieWithId(id: Int): Result<Movie> = withContext(Dispatchers.IO) {
    try {
      val movieDto = api.getMovieById(movieId = id)
      val movie = movieMapper.toDomainModel(movieDto)
      return@withContext if (movie != null) {
        Result.success(movie)
      } else {
        throw Throwable(NO_MOVIE_MESSAGE)
      }
    } catch (e: Throwable) {
      return@withContext Result.failure(Throwable(NO_MOVIE_MESSAGE))
    }
  }
}
