package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class BookmarksRepository(
    private val bookmarkDao: BookmarkDao,
    private val movieMapper: MovieMapper,
) : IBookmarksRepository {
    override suspend fun addBookmark(movieId: Int): Result<Unit> =
        writeBookmark("Unable to bookmark this movie.") {
            bookmarkDao.insertBookmark(Bookmark(movieId = movieId))
        }

    override suspend fun removeBookmark(movieId: Int): Result<Unit> =
        writeBookmark("Unable to remove bookmark.") {
            bookmarkDao.removeBookmark(movieId = movieId)
        }

    override fun observeBookmarks(): Flow<List<Movie>> =
        bookmarkDao.observeBookmarkedMovies()
            .map { movies -> movies.map(movieMapper::toDomainModel) }
            .distinctUntilChanged()

    override fun observeMovieBookmark(movieId: Int): Flow<Boolean> =
        bookmarkDao.observeMovieBookmark(movieId).distinctUntilChanged()

    private suspend fun writeBookmark(message: String, write: suspend () -> Unit): Result<Unit> = try {
        write()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(IllegalStateException(message, error))
    }
}
