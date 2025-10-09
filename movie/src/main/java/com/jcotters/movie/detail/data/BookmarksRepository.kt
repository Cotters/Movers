package com.jcotters.movie.detail.data

import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.DbMovie
import com.jcotters.movie.detail.domain.IBookmarksRepository
import com.jcotters.movie.detail.domain.models.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BookmarksRepository @Inject constructor(
  private val bookmarkDao: BookmarkDao,
  private val movieMapper: MovieMapper,
) : IBookmarksRepository {

  companion object {
    const val UNABLE_TO_BOOKMARK_MESSAGE = "Unable to bookmark this movie."
    const val UNABLE_TO_GET_BOOKMARKS_MESSAGE = "Unable to get bookmarked movies."
  }

  override suspend fun bookmarkMovie(movieId: Int, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      bookmarkDao.insertBookmark(Bookmark(movieId = movieId, userId = userId))
      return@withContext Result.success(Unit)
    } catch (_: Throwable) {
      return@withContext Result.failure(Throwable(UNABLE_TO_BOOKMARK_MESSAGE))
    }
  }

  override suspend fun getUserBookmarks(userId: Int): Result<List<Movie>> = withContext(Dispatchers.IO) {
    try {
      val dbMovies: List<DbMovie> = bookmarkDao.getBookmarkedMoviesForUser(userId)
      val movies: List<Movie> = dbMovies.map(movieMapper::toDomainModel)
      return@withContext Result.success(movies)
    } catch (_: Throwable) {
      return@withContext Result.failure(Throwable(UNABLE_TO_GET_BOOKMARKS_MESSAGE))
    }
  }
}