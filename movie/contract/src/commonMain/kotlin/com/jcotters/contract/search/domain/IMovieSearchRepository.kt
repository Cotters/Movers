package com.jcotters.contract.search.domain

import androidx.paging.PagingData
import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IMovieSearchRepository {
    fun searchMovies(query: String): Flow<PagingData<Movie>>
}
