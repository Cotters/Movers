package com.jcotters.movie.data

import com.jcotters.movie.data.models.MovieDto
import com.jcotters.movie.domain.models.Movie

object MovieMapper {
  fun toDomainModel(movieDto: MovieDto): Movie? {
    return Movie(
      id = movieDto.id ?: return null,
      title = movieDto.title ?: return null,
      releaseDate = movieDto.releaseDate ?: "Unknown release date",
      synopsis = movieDto.overview ?: return null,
    )
  }
}