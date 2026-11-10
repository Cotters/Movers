package com.jcotters.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val RemoveBookmarksMigration9To10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        // Retire only bookmark membership; films and list relationships remain intact.
        connection.execSQL("DROP TABLE IF EXISTS bookmarks")
    }
}
