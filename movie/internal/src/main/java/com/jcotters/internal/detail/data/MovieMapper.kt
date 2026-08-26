package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueEntry
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.detail.data.models.MovieDto
import javax.inject.Inject

internal class MovieMapper @Inject constructor() {
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

  fun toDatabaseModel(
    movies: List<CatalogueMovieDto?>,
  ): List<DbMovie> {
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

  fun toDomainModel(dbMovie: DbMovie): Movie {
    return Movie(
      id = dbMovie.id,
      title = dbMovie.title,
      synopsis = dbMovie.synopsis,
      releaseDate = dbMovie.releaseDate,
      posterUrl = dbMovie.posterUrl,
      backdropUrl = dbMovie.backdropUrl
    )
  }

    fun toCatalogueEntry(catalogue: String, movies: List<DbMovie>, page: Int): List<DbMovieCatalogueEntry> {
        return movies.mapIndexed { index, movie ->
            DbMovieCatalogueEntry(catalogue, movie.id, page, index)
        }
    }
}