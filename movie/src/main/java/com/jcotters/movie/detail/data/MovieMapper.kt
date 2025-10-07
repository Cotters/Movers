package com.jcotters.movie.detail.data

import com.jcotters.movie.detail.data.models.MovieDto
import com.jcotters.movie.detail.domain.models.Movie
import javax.inject.Inject

class MovieMapper @Inject constructor() {
  fun toDomainModel(movieDto: MovieDto): Movie? {
    return Movie(
      id = movieDto.id ?: return null,
      title = movieDto.title ?: return null,
      releaseDate = movieDto.releaseDate ?: "Unknown release date",
      synopsis = movieDto.overview ?: return null,
    )
  }
}