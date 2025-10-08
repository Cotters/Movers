package com.jcotters.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao

@Database(entities = [User::class], version = 1)
abstract class MoversDatabase: RoomDatabase() {

  abstract fun userDao(): UserDao

}