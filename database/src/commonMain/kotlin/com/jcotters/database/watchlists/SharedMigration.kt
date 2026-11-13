package com.jcotters.database.watchlists

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val SharedMigration8To9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE device_profile ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE device_profile ADD COLUMN nameConfirmed INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE device_profile SET color = abs(random() % 8)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS shared_lists (listId TEXT NOT NULL PRIMARY " +
                "KEY, participantId TEXT NOT NULL, revision INTEGER NOT NULL, payload " +
                "TEXT NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS shared_operations (id TEXT NOT NULL " +
                "PRIMARY KEY, listId TEXT NOT NULL, participantId TEXT NOT NULL, " +
                "payload TEXT NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS shared_activity_cursors (listId TEXT NOT " +
                "NULL PRIMARY KEY, nextBefore INTEGER)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS shared_activity (listId TEXT NOT NULL, " +
                "eventId TEXT NOT NULL, sequence INTEGER NOT NULL, payload TEXT NOT " +
                "NULL, PRIMARY KEY(listId,eventId))"
        )
    }
}
