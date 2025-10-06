package com.jcotters.movie.data

import com.jcotters.movie.domain.IMovieDetailsRepository
import com.jcotters.movie.domain.models.Movie
import javax.inject.Inject

class MovieDetailsRepository @Inject constructor(
  val api: MovieApi,
  val movieMapper: MovieMapper,
): IMovieDetailsRepository {

  override fun getMovieWithId(id: Int): Movie? {
    return movieMapper.toDomainModel(
      api.getMovieDetails(movieId = id)
    )
  }
}
