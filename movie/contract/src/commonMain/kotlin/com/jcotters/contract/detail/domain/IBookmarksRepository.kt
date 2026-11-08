package com.jcotters.contract.detail.domain

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IBookmarksRepository {
  suspend fun addBookmark(movieId: Int): Result<Unit>
  suspend fun removeBookmark(movieId: Int): Result<Unit>
  fun observeBookmarks(): Flow<List<Movie>>
  fun observeMovieBookmark(movieId: Int): Flow<Boolean>
}
