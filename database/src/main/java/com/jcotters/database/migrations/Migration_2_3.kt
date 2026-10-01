package com.jcotters.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Version 2 existed both with and without these cache tables.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `movie_catalogue_entries` (
                `catalogue` TEXT NOT NULL,
                `movieId` INTEGER NOT NULL,
                `page` INTEGER NOT NULL,
                `pageIndex` INTEGER NOT NULL,
                PRIMARY KEY(`catalogue`, `movieId`),
                FOREIGN KEY(`movieId`) REFERENCES `movies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_movie_catalogue_entries_movieId` ON `movie_catalogue_entries` (`movieId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_movie_catalogue_entries_catalogue_page_pageIndex` ON `movie_catalogue_entries` (`catalogue`, `page`, `pageIndex`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `movie_catalogue_remote_keys` (
                `catalogue` TEXT NOT NULL,
                `nextPage` INTEGER,
                PRIMARY KEY(`catalogue`)
            )
        """.trimIndent())

        // SQLite requires rebuilding the table to change the movie foreign key's delete action.
        // Keep user CASCADE behavior and preserve every bookmark while adopting movie NO ACTION.
        db.execSQL("""
            CREATE TABLE `bookmarks_new` (
                `movieId` INTEGER NOT NULL,
                `userId` INTEGER NOT NULL,
                PRIMARY KEY(`movieId`, `userId`),
                FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`movieId`) REFERENCES `movies`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """.trimIndent())
        db.execSQL("INSERT INTO `bookmarks_new` (`movieId`, `userId`) SELECT `movieId`, `userId` FROM `bookmarks`")
        db.execSQL("DROP TABLE `bookmarks`")
        db.execSQL("ALTER TABLE `bookmarks_new` RENAME TO `bookmarks`")
        db.execSQL("CREATE INDEX `index_bookmarks_userId` ON `bookmarks` (`userId`)")
    }
}
