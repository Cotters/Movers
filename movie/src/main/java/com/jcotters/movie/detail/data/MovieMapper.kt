package com.jcotters.movie.detail.data

import com.jcotters.database.movies.DbMovie
import com.jcotters.movie.catalogue.data.models.CatalogueMovieDto
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
      posterUrl = movieDto.posterPath?.let { "https://image.tmdb.org/t/p/w500/$it" },
      backdropUrl = movieDto.backdropPath?.let { "https://image.tmdb.org/t/p/w500/$it" },
    )
  }

  fun toDomainModel(catalogueResults: List<CatalogueMovieDto?>): List<Movie> {
    return catalogueResults.mapNotNull { dto ->
      return@mapNotNull Movie(
        id = dto?.id ?: return@mapNotNull null,
        title = dto.title ?: return@mapNotNull null,
        synopsis = dto.overview ?: return@mapNotNull null,
        releaseDate = dto.releaseDate ?: "Unknown release date",
        posterUrl = dto.posterPath?.let { "https://image.tmdb.org/t/p/w500/$it" },
      )
    }
  }

  fun toDatabaseModel(movies: List<CatalogueMovieDto?>): List<DbMovie> {
    return movies.mapNotNull { dto ->
      DbMovie(
        id = dto?.id ?: return@mapNotNull null,
        title = dto.title ?: return@mapNotNull null,
        synopsis = dto.overview ?: return@mapNotNull null,
        releaseDate = dto.releaseDate ?: "Unknown release date",
        posterUrl = dto.posterPath?.let { "https://image.tmdb.org/t/p/w500/$it" },
        backdropUrl = dto.backdropPath?.let { "https://image.tmdb.org/t/p/w500/$it" },
      )
    }
  }
}