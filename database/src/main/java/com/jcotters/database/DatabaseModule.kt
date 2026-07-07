package com.jcotters.database

import android.content.Context
import androidx.room.Room
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.migrations.MIGRATION_1_2
import com.jcotters.database.movies.MovieDao
import com.jcotters.database.user.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  private const val DATABASE_NAME: String = "movers_database"

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): MoversDatabase {
    return Room.databaseBuilder(
      context,
      MoversDatabase::class.java,
      DATABASE_NAME,
    )
      .addMigrations(MIGRATION_1_2)
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

  @Provides
  fun provideMovieDao(database: MoversDatabase): MovieDao {
    return database.movieDao()
  }
}