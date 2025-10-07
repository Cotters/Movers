package com.jcotters.movie.catalogue.domain

import com.jcotters.movie.detail.domain.models.Movie
import javax.inject.Inject

class GetPopularMoviesUseCase @Inject constructor(
  private val movieCatalogueRepository: IMovieCatalogueRepository,
) {
  suspend operator fun invoke(page: Int): List<Movie> {
    return movieCatalogueRepository.getPopularMovies(page = page)
  }
}