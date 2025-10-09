package com.jcotters.movie.detail.data

import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.movie.detail.domain.IBookmarksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BookmarksRepository @Inject constructor(
  private val bookmarkDao: BookmarkDao,
) : IBookmarksRepository {

  companion object {
    const val UNABLE_TO_BOOKMARK_MESSAGE = "Unable to bookmark this movie."
  }

  override suspend fun bookmarkMovie(movieId: Int, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      bookmarkDao.insertBookmark(Bookmark(movieId = movieId, userId = userId))
      return@withContext Result.success(Unit)
    } catch (_: Throwable) {
      return@withContext Result.failure(Throwable(UNABLE_TO_BOOKMARK_MESSAGE))
    }
  }

}