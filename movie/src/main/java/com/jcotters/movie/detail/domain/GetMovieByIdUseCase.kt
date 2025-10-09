package com.jcotters.movie.detail.domain

import com.jcotters.movie.detail.domain.models.Movie
import javax.inject.Inject

class GetMovieByIdUseCase @Inject constructor(
  private val repository: IMovieDetailsRepository,
) {
  suspend fun invoke(id: Int): Result<Movie> {
    return repository.getMovieWithId(id)
  }
}