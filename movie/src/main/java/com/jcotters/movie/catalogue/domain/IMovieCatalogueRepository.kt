package com.jcotters.movie.catalogue.domain

import com.jcotters.movie.detail.domain.models.Movie

interface IMovieCatalogueRepository {
  suspend fun getPopularMovies(page: Int = 1): List<Movie>
}