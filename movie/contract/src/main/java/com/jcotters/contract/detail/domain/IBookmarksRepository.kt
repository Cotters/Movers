package com.jcotters.contract.detail.domain

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

interface IBookmarksRepository {
  suspend fun addBookmark(movieId: Int, userId: Int): Result<Unit>
  suspend fun removeBookmark(movieId: Int, userId: Int): Result<Unit>
  fun observeUserBookmarks(userId: Int): Flow<List<Movie>>
  fun observeMovieBookmark(userId: Int, movieId: Int): Flow<Boolean>
}
