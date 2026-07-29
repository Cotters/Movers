package com.jcotters.details.domain

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import javax.inject.Inject

class GetMovieByIdUseCase @Inject constructor(
    private val repository: IMovieDetailsRepository,
) {
  suspend fun invoke(id: Int): Result<Movie> {
    return repository.getMovieWithId(id)
  }
}