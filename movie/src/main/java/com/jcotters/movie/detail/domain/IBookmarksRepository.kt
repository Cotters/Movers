package com.jcotters.movie.detail.domain

import com.jcotters.movie.detail.domain.models.Movie

interface IBookmarksRepository {
  suspend fun bookmarkMovie(movieId: Int, userId: Int): Result<Unit>
  suspend fun getUserBookmarks(userId: Int): Result<List<Movie>>
}