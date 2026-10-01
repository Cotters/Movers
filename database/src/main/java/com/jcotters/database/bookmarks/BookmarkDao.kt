package com.jcotters.database.bookmarks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jcotters.database.movies.DbMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertBookmark(bookmark: Bookmark)

  @Delete
  suspend fun delete(user: Bookmark)

  @Query(
    """
    SELECT m.* FROM movies m
    INNER JOIN bookmarks b ON m.id = b.movieId
    WHERE b.userId = :userId
  """
  )
  suspend fun getBookmarkedMoviesForUser(userId: Int): List<DbMovie>

  @Query("SELECT m.* FROM movies m INNER JOIN bookmarks b ON m.id = b.movieId WHERE b.userId = :userId ORDER BY m.id")
  fun observeBookmarkedMovies(userId: Int): Flow<List<DbMovie>>

  @Query(
    """
    SELECT EXISTS(
      SELECT * FROM bookmarks
      WHERE userId = :userId AND movieId = :movieId
    )
  """
  )
  fun observeMovieBookmark(userId: Int, movieId: Int): Flow<Boolean>

  @Query("DELETE FROM bookmarks WHERE userId = :userId AND movieId = :movieId")
  suspend fun removeBookmark(userId: Int, movieId: Int)
}
