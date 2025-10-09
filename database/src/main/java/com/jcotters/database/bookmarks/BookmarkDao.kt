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
}