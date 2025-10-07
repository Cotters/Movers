package com.jcotters.movie.catalogue.data

import com.jcotters.movie.MovieApi
import com.jcotters.movie.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.movie.detail.data.MovieMapper
import com.jcotters.movie.detail.domain.models.Movie
import javax.inject.Inject

class MovieCatalogueRepository @Inject constructor(
  private val movieApi: MovieApi,
  private val movieMapper: MovieMapper,
): IMovieCatalogueRepository {

  override suspend fun getPopularMovies(page: Int): List<Movie> {
    val movies = movieApi.getPopularMovies(page = page).results
    return movieMapper.toDomainModel(catalogueResults = movies.orEmpty())
  }
}