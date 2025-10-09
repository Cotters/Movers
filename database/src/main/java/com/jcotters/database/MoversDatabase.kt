package com.jcotters.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieDao
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao

@Database(entities = [User::class, Bookmark::class, DbMovie::class], version = 4)
abstract class MoversDatabase : RoomDatabase() {

  abstract fun userDao(): UserDao

  abstract fun bookmarkDao(): BookmarkDao

  abstract fun movieDao(): MovieDao

}