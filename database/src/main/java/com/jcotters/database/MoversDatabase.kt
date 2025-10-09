package com.jcotters.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao

@Database(entities = [User::class, Bookmark::class], version = 2)
abstract class MoversDatabase : RoomDatabase() {

  abstract fun userDao(): UserDao

  abstract fun bookmarkDao(): BookmarkDao

}