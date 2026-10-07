package com.jcotters.database.migrations

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.database.MoversDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Old schemas are explicit because multiple different schemas were published as version 2. */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var database: MoversDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrateVersionOne() = runTest {
        assertUpgrade(version = 1, withCatalogue = false, movieDeleteAction = "CASCADE")
    }

    @Test
    fun migrateVersionTwoWithoutCatalogue() = runTest {
        assertUpgrade(version = 2, withCatalogue = false, movieDeleteAction = "NO ACTION")
    }

    @Test
    fun migrateVersionTwoWithCatalogueAndCascadeBookmarks() = runTest {
        assertUpgrade(version = 2, withCatalogue = true, movieDeleteAction = "CASCADE")
    }

    @Test
    fun migrateVersionTwoWithCatalogueAndProtectedBookmarks() = runTest {
        assertUpgrade(version = 2, withCatalogue = true, movieDeleteAction = "NO ACTION")
    }

    @Test
    fun migrateVersionThree() = runTest {
        assertUpgrade(version = 3, withCatalogue = true, movieDeleteAction = "NO ACTION")
    }

    @Test
    fun migrateVersionFour() = runTest {
        assertUpgrade(version = 4, withCatalogue = true, movieDeleteAction = "NO ACTION")
    }

    @Test
    fun migrateVersionFive() = runTest {
        assertUpgrade(version = 5, withCatalogue = true, movieDeleteAction = "NO ACTION")
    }

    private suspend fun assertUpgrade(version: Int, withCatalogue: Boolean, movieDeleteAction: String) {
        createLegacyDatabase(version, withCatalogue, movieDeleteAction)
        val migrated = Room.databaseBuilder(context, MoversDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
        database = migrated

        // Opening through Room validates the complete final schema, not just our SELECT queries.
        assertEquals("existing-user", migrated.userDao().getUserById(1)?.username)
        assertEquals(if (version >= 4) "existing-hash" else null, migrated.userDao().getUserById(1)?.passwordHash)
        migrated.userDao().importCredentials(1, "0f", "existing-hash")
        assertEquals("existing-hash", migrated.userDao().getUserById(1)?.passwordHash)
        val movie = migrated.movieDao().getMovieById(7)
        assertNotNull(movie)
        assertEquals("Saved movie", movie?.title)
        assertEquals(null, movie?.detailsFetchedAt)
        assertEquals(null, movie?.genres)
        assertEquals(null, movie?.runtimeMinutes)
        assertEquals(listOf(7), migrated.bookmarkDao().getBookmarkedMoviesForUser(1).map { it.id })
        if (withCatalogue) {
            assertEquals(if (version >= 5) 123L else null, migrated.movieCatalogueRemoteKeysDao().getRemoteKey("popular")?.lastRefreshedAt)
            assertEquals(2, migrated.movieCatalogueRemoteKeysDao().getRemoteKey("popular")?.nextPage)
            val rows = migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM movie_catalogue_entries")
            rows.use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }
        assertThrows(SQLiteConstraintException::class.java) {
            migrated.openHelper.writableDatabase.execSQL("DELETE FROM movies WHERE id = 7")
        }
        migrated.userDao().delete(requireNotNull(migrated.userDao().getUserById(1)))
        assertEquals(emptyList<Int>(), migrated.bookmarkDao().getBookmarkedMoviesForUser(1).map { it.id })
    }

    private fun createLegacyDatabase(version: Int, withCatalogue: Boolean, movieDeleteAction: String) {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL)")
            if (version >= 2) {
                db.execSQL("CREATE UNIQUE INDEX index_users_username ON users(username)")
            }
            db.execSQL("""
                CREATE TABLE movies (
                    id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, synopsis TEXT NOT NULL,
                    releaseDate TEXT NOT NULL, posterUrl TEXT, backdropUrl TEXT
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE bookmarks (
                    movieId INTEGER NOT NULL, userId INTEGER NOT NULL, PRIMARY KEY(movieId, userId),
                    FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(movieId) REFERENCES movies(id) ON UPDATE NO ACTION ON DELETE $movieDeleteAction
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX index_bookmarks_userId ON bookmarks(userId)")
            db.execSQL("INSERT INTO users(id, username) VALUES(1, 'existing-user')")
            db.execSQL("INSERT INTO movies(id, title, synopsis, releaseDate) VALUES(7, 'Saved movie', 'Overview', '2026-01-01')")
            db.execSQL("INSERT INTO bookmarks(movieId, userId) VALUES(7, 1)")
            if (withCatalogue) {
                db.execSQL("""
                    CREATE TABLE movie_catalogue_entries (
                        catalogue TEXT NOT NULL, movieId INTEGER NOT NULL, page INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL, PRIMARY KEY(catalogue, movieId),
                        FOREIGN KEY(movieId) REFERENCES movies(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_movie_catalogue_entries_movieId ON movie_catalogue_entries(movieId)")
                db.execSQL("CREATE INDEX index_movie_catalogue_entries_catalogue_page_pageIndex ON movie_catalogue_entries(catalogue, page, pageIndex)")
                db.execSQL("CREATE TABLE movie_catalogue_remote_keys (catalogue TEXT NOT NULL PRIMARY KEY, nextPage INTEGER)")
                db.execSQL("INSERT INTO movie_catalogue_entries VALUES('popular', 7, 1, 0)")
                db.execSQL("INSERT INTO movie_catalogue_remote_keys VALUES('popular', 2)")
            }
            if (version >= 4) {
                db.execSQL("ALTER TABLE users ADD COLUMN passwordSalt TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT")
                db.execSQL("UPDATE users SET passwordSalt = '0f', passwordHash = 'existing-hash' WHERE id = 1")
            }
            if (version >= 5) {
                db.execSQL("ALTER TABLE movie_catalogue_remote_keys ADD COLUMN lastRefreshedAt INTEGER")
                db.execSQL("UPDATE movie_catalogue_remote_keys SET lastRefreshedAt = 123")
            }
            db.version = version
        }
    }

    private companion object {
        const val DATABASE_NAME = "migration-test"
    }
}
