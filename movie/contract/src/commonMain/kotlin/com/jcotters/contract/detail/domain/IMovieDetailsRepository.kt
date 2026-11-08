package com.jcotters.contract.detail.domain

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IMovieDetailsRepository {
  fun observeMovieWithId(id: Int): Flow<Movie?>
  suspend fun refreshMovieWithId(id: Int): Result<Unit>
}
