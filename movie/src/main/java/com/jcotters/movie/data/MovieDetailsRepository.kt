package com.jcotters.movie.data

import android.util.Log
import com.jcotters.movie.domain.IMovieDetailsRepository
import com.jcotters.movie.domain.models.Movie
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
      Log.d("TJ", "Error in Repo: ${e.message}")
      return@withContext Result.failure(Throwable(NO_MOVIE_MESSAGE))
    }
  }
}
