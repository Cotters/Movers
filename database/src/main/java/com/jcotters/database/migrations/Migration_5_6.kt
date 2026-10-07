package com.jcotters.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "tagline TEXT", "runtimeMinutes INTEGER", "genres TEXT",
            "voteAverage REAL", "voteCount INTEGER", "status TEXT",
            "originalLanguage TEXT", "detailsFetchedAt INTEGER",
        ).forEach { column -> db.execSQL("ALTER TABLE movies ADD COLUMN $column") }
    }
}
