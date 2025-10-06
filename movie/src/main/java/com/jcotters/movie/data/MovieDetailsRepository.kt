package com.jcotters.movie.data

import com.jcotters.movie.domain.IMovieDetailsRepository
import com.jcotters.movie.domain.models.Movie
import javax.inject.Inject

class MovieDetailsRepository @Inject constructor(
  val api: MovieApi,
  val movieMapper: MovieMapper,
) : IMovieDetailsRepository {

  companion object {
    private const val NO_MOVIE_MESSAGE = "Could not find requested movie."
  }

  override fun getMovieWithId(id: Int): Result<Movie> {
    try {
      val movie = movieMapper.toDomainModel(api.getMovieById(movieId = id))
      return if (movie != null) {
        Result.success(movie)
      } else {
        throw Throwable(NO_MOVIE_MESSAGE)
      }
    } catch (e: Throwable) {
      return Result.failure(Throwable(NO_MOVIE_MESSAGE))
    }
  }
}
