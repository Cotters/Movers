package com.jcotters.database.bookmarks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface BookmarkDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertBookmark(bookmark: Bookmark)

  @Delete
  suspend fun delete(user: Bookmark)
}