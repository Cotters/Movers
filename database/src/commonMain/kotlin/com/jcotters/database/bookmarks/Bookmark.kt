package com.jcotters.database.bookmarks

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jcotters.database.movies.DbMovie

@Entity(tableName = "bookmarks", foreignKeys = [ForeignKey(
    entity = DbMovie::class, parentColumns = ["id"], childColumns = ["movieId"],
    onDelete = ForeignKey.NO_ACTION,
)])
data class Bookmark(@PrimaryKey val movieId: Int)
