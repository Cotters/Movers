package com.jcotters.database.watchlists

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val WatchlistMigration7To8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS watchlists (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS watchlist_movies (
                watchlistId TEXT NOT NULL, movieId INTEGER NOT NULL, addedAt INTEGER NOT NULL,
                PRIMARY KEY(watchlistId, movieId),
                FOREIGN KEY(watchlistId) REFERENCES watchlists(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(movieId) REFERENCES movies(id) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_watchlist_movies_movieId ON watchlist_movies(movieId)")
    }
}
