package com.jcotters.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movie_catalogue_remote_keys ADD COLUMN lastRefreshedAt INTEGER")
    }
}
