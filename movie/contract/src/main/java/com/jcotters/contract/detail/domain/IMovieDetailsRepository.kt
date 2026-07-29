package com.jcotters.contract.detail.domain

import com.jcotters.contract.detail.domain.models.Movie

interface IMovieDetailsRepository {
  suspend fun getMovieWithId(id: Int): Result<Movie>
}