package com.jcotters.movie.detail.domain

interface IBookmarksRepository {
  suspend fun bookmarkMovie(movieId: Int, userId: Int): Result<Unit>
}