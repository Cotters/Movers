package com.jcotters.contract.catalogue.domain

import androidx.paging.PagingData
import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IMovieCatalogueRepository {
  fun getPopularMoviesPaging(): Flow<PagingData<Movie>>
  suspend fun getPopularMovies(page: Int = 1): List<Movie>
}