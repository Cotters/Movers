package com.jcotters.contract.catalogue.domain

import com.jcotters.contract.detail.domain.models.Movie
import javax.inject.Inject

class GetPopularMoviesUseCase @Inject constructor(
    private val movieCatalogueRepository: IMovieCatalogueRepository,
) {

  val popularMovies = movieCatalogueRepository.getPopularMoviesPaging()

  suspend operator fun invoke(page: Int): List<Movie> {
    return movieCatalogueRepository.getPopularMovies(page = page)
  }
}