package com.jcotters.database.bookmarks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.user.User

@Entity(
  tableName = "bookmarks",
  primaryKeys = ["movieId", "userId"],
  foreignKeys = [ForeignKey(
    entity = User::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("userId"),
    onDelete = ForeignKey.CASCADE,
  ),ForeignKey(
    entity = DbMovie::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("movieId"),
    onDelete = ForeignKey.CASCADE,
  )]
)
data class Bookmark(
  val movieId: Int,
  @ColumnInfo(index = true)
  val userId: Int,
)