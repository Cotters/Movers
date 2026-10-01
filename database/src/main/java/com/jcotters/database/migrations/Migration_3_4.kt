package com.jcotters.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN passwordSalt TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT")
    }
}
