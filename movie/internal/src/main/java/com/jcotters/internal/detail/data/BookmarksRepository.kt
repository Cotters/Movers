package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class BookmarksRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val movieMapper: MovieMapper,
) : IBookmarksRepository {
    override suspend fun addBookmark(movieId: Int, userId: Int): Result<Unit> =
        writeBookmark("Unable to bookmark this movie.") {
            bookmarkDao.insertBookmark(Bookmark(movieId = movieId, userId = userId))
        }

    override suspend fun removeBookmark(movieId: Int, userId: Int): Result<Unit> =
        writeBookmark("Unable to remove bookmark.") {
            bookmarkDao.removeBookmark(userId = userId, movieId = movieId)
        }

    override fun observeUserBookmarks(userId: Int): Flow<List<Movie>> =
        bookmarkDao.observeBookmarkedMovies(userId)
            .map { movies -> movies.map(movieMapper::toDomainModel) }
            .distinctUntilChanged()

    override fun observeMovieBookmark(userId: Int, movieId: Int): Flow<Boolean> =
        bookmarkDao.observeMovieBookmark(userId, movieId).distinctUntilChanged()

    private suspend fun writeBookmark(message: String, write: suspend () -> Unit): Result<Unit> = try {
        write()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(IllegalStateException(message, error))
    }
}
