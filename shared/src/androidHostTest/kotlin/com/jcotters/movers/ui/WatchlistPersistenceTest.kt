package com.jcotters.movers.ui

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.jcotters.database.MoversDatabase
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.watchlists.WatchlistMigration7To8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchlistPersistenceTest {
    @Test
    fun migrationPreservesProfileBookmarksAndEnrichedCatalogueAcrossReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "watchlist-migration-test.db"
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(checkNotNull(javaClass.getResourceAsStream(
            "/com.jcotters.database.MoversDatabase/7.json",
        )).bufferedReader().use { it.readText() }).jsonObject.getValue("database").jsonObject
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).also { it.parentFile?.mkdirs() }, null).use { old ->
                schema.getValue("entities").jsonArray.forEach { element ->
                    val entity = element.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    fun execute(sql: String) = old.execSQL(sql.replace("\${TABLE_NAME}", table))
                    execute(entity.getValue("createSql").jsonPrimitive.content)
                    entity["indices"]?.jsonArray?.forEach { execute(it.jsonObject.getValue("createSql").jsonPrimitive.content) }
                }
                schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
                old.execSQL("INSERT INTO device_profile VALUES(1, 'stable-installation', 'Josh')")
                old.execSQL("INSERT INTO movies(id,title,synopsis,releaseDate,runtimeMinutes,detailsFetchedAt) VALUES(12,'Saved movie','Details','2026-01-01',125,1234)")
                old.execSQL("INSERT INTO bookmarks VALUES(12)")
                old.execSQL("INSERT INTO movie_catalogue_entries VALUES('popular',12,1,0)")
                old.execSQL("INSERT INTO movie_catalogue_remote_keys VALUES('popular',2,999)")
                old.version = 7
            }
            fun open() = Room.databaseBuilder<MoversDatabase>(context, name)
                .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(WatchlistMigration7To8).build()
            MoversUiFixture(open()).use { fixture ->
                val db = fixture.database
                assertEquals("stable-installation", db.profileDao().get()?.installationId)
                assertEquals(125, db.movieDao().getMovieById(12)?.runtimeMinutes)
                assertEquals(1234L, db.movieDao().getMovieById(12)?.detailsFetchedAt)
                assertTrue(db.bookmarkDao().observeMovieBookmark(12).first())
                assertEquals(2, db.movieCatalogueRemoteKeysDao().getRemoteKey("popular")?.nextPage)
                val id = fixture.lists.create("Survives restart").getOrThrow()
                fixture.lists.addMovieToLists(12, setOf(id)).getOrThrow()
            }
            MoversUiFixture(open()).use { fixture ->
                val list = fixture.lists.observeWatchlists().first().single()
                assertEquals("Survives restart", list.name)
                assertEquals(1, list.movieCount)
                assertEquals(listOf(12), fixture.lists.observeWatchlist(list.id).first()?.movies?.map { it.id })
                assertEquals("stable-installation", fixture.database.profileDao().get()?.installationId)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test
    fun duplicateListsIdempotentMembershipAndDeletionPreserveMoviesAndBookmarks() = runBlocking {
        MoversUiFixture().use { fixture ->
            val db = fixture.database
            db.movieDao().insertMovie(DbMovie(12, "Movie", "Synopsis", "2026-01-01", runtimeMinutes = 125))
            db.bookmarkDao().insertBookmark(Bookmark(12))
            val one = fixture.lists.create("Same").getOrThrow()
            val two = fixture.lists.create("same").getOrThrow()
            fixture.lists.addMovieToLists(12, setOf(one, two)).getOrThrow()
            fixture.lists.addMovieToLists(12, setOf(one, two)).getOrThrow()
            assertEquals(setOf(one, two), fixture.lists.observeListIds(12).first())
            assertEquals(listOf(1, 1), fixture.lists.observeWatchlists().first().map { it.movieCount })
            fixture.lists.rename(one, "Renamed").getOrThrow()
            fixture.lists.removeMovie(two, 12).getOrThrow()
            fixture.lists.delete(one).getOrThrow()
            assertEquals(emptySet<Int>(), fixture.lists.observeMovieIds(two).first())
            assertEquals("same", fixture.lists.observeWatchlists().first().single().name)
            assertTrue(db.bookmarkDao().observeMovieBookmark(12).first())
            assertEquals(125, db.movieDao().getMovieById(12)?.runtimeMinutes)
        }
    }

    @Test
    fun failedSecondDestinationRollsBackTheWholeMultiListWrite() = runBlocking {
        MoversUiFixture().use { fixture ->
            val db = fixture.database
            db.movieDao().insertMovie(DbMovie(12, "Movie", "Synopsis", "2026-01-01"))
            val one = fixture.lists.create("One").getOrThrow()
            val two = fixture.lists.create("Two").getOrThrow()
            db.useWriterConnection { connection ->
                connection.immediateTransaction {
                    connection.usePrepared("CREATE TRIGGER reject_second BEFORE INSERT ON watchlist_movies WHEN NEW.watchlistId = '$two' BEGIN SELECT RAISE(ABORT, 'test failure'); END") { it.step() }
                }
            }
            assertTrue(fixture.lists.addMovieToLists(12, linkedSetOf(one, two)).isFailure)
            assertFalse(12 in fixture.lists.observeMovieIds(one).first())
            assertFalse(12 in fixture.lists.observeMovieIds(two).first())
            assertTrue(fixture.lists.addMovieToLists(12, setOf(one, "missing")).isFailure)
            assertEquals(emptySet<String>(), fixture.lists.observeListIds(12).first())
        }
    }
}
