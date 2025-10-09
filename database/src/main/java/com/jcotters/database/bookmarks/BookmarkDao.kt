package com.jcotters.database.bookmarks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jcotters.database.movies.DbMovie

@Dao
interface BookmarkDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
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

  @Query(
    """
    SELECT EXISTS(
      SELECT * FROM bookmarks
      WHERE userId = :userId AND movieId = :movieId
    )
  """
  )
  suspend fun hasUserBookmarkedMovie(userId: Int, movieId: Int): Boolean

  @Query("DELETE FROM bookmarks WHERE userId = :userId AND movieId = :movieId")
  suspend fun removeBookmark(userId: Int, movieId: Int)
}