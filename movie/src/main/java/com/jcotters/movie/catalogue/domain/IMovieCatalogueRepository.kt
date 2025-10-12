package com.jcotters.movie.catalogue.domain

import androidx.paging.PagingData
import com.jcotters.movie.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IMovieCatalogueRepository {
  fun getPopularMoviesPaging(): Flow<PagingData<Movie>>
  suspend fun getPopularMovies(page: Int = 1): List<Movie>
}