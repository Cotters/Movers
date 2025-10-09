package com.jcotters.database.bookmarks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.jcotters.database.user.User
import kotlin.random.Random

@Entity(
  tableName = "bookmarks",
  primaryKeys = ["id", "movieId"],
  foreignKeys = [ForeignKey(
    entity = User::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("userId"),
    onDelete = ForeignKey.CASCADE,
  )]
)
data class Bookmark(
  val id: Int = Random.Default.nextInt(),
  val movieId: Int,
  @ColumnInfo(index = true)
  val userId: Int,
)