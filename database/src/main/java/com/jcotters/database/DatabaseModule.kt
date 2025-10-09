package com.jcotters.database

import android.content.Context
import androidx.room.Room
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.user.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  private const val DATABASE_NAME: String = "movers_database"

  @Provides
  fun provideDatabase(@ApplicationContext context: Context): MoversDatabase {
    return Room.databaseBuilder(
      context,
      MoversDatabase::class.java,
      DATABASE_NAME,
    )
      .build()
  }

  @Provides
  fun provideUserDao(database: MoversDatabase): UserDao {
    return database.userDao()
  }

  @Provides
  fun provideBookmarkDao(database: MoversDatabase): BookmarkDao {
    return database.bookmarkDao()
  }
}