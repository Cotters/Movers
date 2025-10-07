package com.jcotters.movie.detail.domain

import com.jcotters.movie.detail.domain.models.Movie

interface IMovieDetailsRepository {
  suspend fun getMovieWithId(id: Int): Result<Movie>
}