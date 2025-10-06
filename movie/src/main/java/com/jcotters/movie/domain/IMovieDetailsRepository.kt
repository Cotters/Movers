package com.jcotters.movie.domain

import com.jcotters.movie.domain.models.Movie

interface IMovieDetailsRepository {
  fun getMovieWithId(id: Int): Result<Movie>
}