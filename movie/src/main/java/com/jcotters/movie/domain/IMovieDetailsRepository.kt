package com.jcotters.movie.domain

import com.jcotters.movie.domain.models.Movie

interface IMovieDetailsRepository {
  suspend fun getMovieWithId(id: Int): Result<Movie>
}