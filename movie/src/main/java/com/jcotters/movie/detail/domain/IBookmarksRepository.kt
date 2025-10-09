package com.jcotters.movie.detail.domain

import com.jcotters.movie.detail.domain.models.Movie

interface IBookmarksRepository {
  suspend fun addBookmark(movieId: Int, userId: Int): Result<Unit>
  suspend fun removeBookmark(movieId: Int, userId: Int): Result<Unit>
  suspend fun getUserBookmarks(userId: Int): Result<List<Movie>>
  suspend fun isMovieBookmarked(userId: Int, movieId: Int): Result<Boolean>
}