package com.jcotters.movie.domain

import com.jcotters.movie.domain.models.Movie
import javax.inject.Inject

class GetMovieByIdUseCase @Inject constructor(
  private val repository: IMovieDetailsRepository,
) {
  suspend operator fun invoke(id: Int): Result<Movie> {
    return repository.getMovieWithId(id)
  }
}