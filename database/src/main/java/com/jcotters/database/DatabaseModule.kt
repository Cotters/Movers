package com.jcotters.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.MovieDao
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

  private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
      database.execSQL("""
        CREATE TABLE IF NOT EXISTS movies (
          id INTEGER NOT NULL PRIMARY KEY,
          title TEXT NOT NULL,
          synopsis TEXT NOT NULL,
          releaseDate TEXT NOT NULL,
          posterUrl TEXT,
          backdropUrl TEXT
        )
      """)
    }
  }

  @Provides
  fun provideDatabase(@ApplicationContext context: Context): MoversDatabase {
    return Room.databaseBuilder(
      context,
      MoversDatabase::class.java,
      DATABASE_NAME,
    )
//      .addMigrations(MIGRATION_2_3)
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