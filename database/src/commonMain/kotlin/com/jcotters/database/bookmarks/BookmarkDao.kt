package com.jcotters.database.bookmarks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jcotters.database.movies.DbMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("SELECT m.* FROM movies m INNER JOIN bookmarks b ON m.id = b.movieId ORDER BY m.id")
    fun observeBookmarkedMovies(): Flow<List<DbMovie>>

    @Query("SELECT EXISTS(SELECT * FROM bookmarks WHERE movieId = :movieId)")
    fun observeMovieBookmark(movieId: Int): Flow<Boolean>

    @Query("DELETE FROM bookmarks WHERE movieId = :movieId")
    suspend fun removeBookmark(movieId: Int)
}
