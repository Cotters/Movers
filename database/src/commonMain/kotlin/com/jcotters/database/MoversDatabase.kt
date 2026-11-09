package com.jcotters.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueEntry
import com.jcotters.database.movies.DbMovieCatalogueRemoteKey
import com.jcotters.database.movies.MovieCatalogueDao
import com.jcotters.database.movies.MovieCatalogueRemoteKeysDao
import com.jcotters.database.movies.MovieDao
import com.jcotters.database.profile.DeviceProfile
import com.jcotters.database.profile.DeviceProfileDao
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import com.jcotters.database.watchlists.WatchlistDao
import com.jcotters.database.watchlists.WatchlistMigration7To8
import kotlin.coroutines.CoroutineContext

@Database(
    entities = [DeviceProfile::class, Bookmark::class, DbMovie::class, DbMovieCatalogueEntry::class, DbMovieCatalogueRemoteKey::class, DbWatchlist::class, DbWatchlistMovie::class],
    version = 8,
)
@ConstructedBy(MoversDatabaseConstructor::class)
abstract class MoversDatabase : RoomDatabase() {
    abstract fun profileDao(): DeviceProfileDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun movieDao(): MovieDao
    abstract fun movieCatalogueDao(): MovieCatalogueDao
    abstract fun movieCatalogueRemoteKeysDao(): MovieCatalogueRemoteKeysDao
    abstract fun watchlistDao(): WatchlistDao
}

@Suppress("KotlinNoActualForExpect")
expect object MoversDatabaseConstructor : RoomDatabaseConstructor<MoversDatabase> {
    override fun initialize(): MoversDatabase
}

fun createDatabase(builder: RoomDatabase.Builder<MoversDatabase>, queryContext: CoroutineContext): MoversDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(queryContext)
    .addMigrations(WatchlistMigration7To8)
    // One-time, explicitly authorized reset from the retired account-based app.
    .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6)
    .build()
