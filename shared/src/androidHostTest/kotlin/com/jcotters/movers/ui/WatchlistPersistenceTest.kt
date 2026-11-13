package com.jcotters.movers.ui

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.data.Participant
import com.jcotters.contract.watchlists.data.SharedCommand
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.data.SharedListInfo
import com.jcotters.contract.watchlists.data.SharedSnapshot
import com.jcotters.database.MoversDatabase
import com.jcotters.database.RemoveBookmarksMigration9To10
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
    fun migrationDropsBookmarksAndPreservesProfileListsAndEnrichedCatalogueAcrossReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (sourceVersion in listOf(7, 8, 9)) {
            val name = "watchlist-migration-$sourceVersion-test.db"
            context.deleteDatabase(name)
            try {
                createLegacyDatabase(context, name, sourceVersion)
                MoversUiFixture(openMigratedDatabase(context, name)).use { fixture ->
                    assertMigratedData(fixture, sourceVersion)
                }
                MoversUiFixture(openMigratedDatabase(context, name)).use { fixture ->
                    assertReopenedData(fixture, sourceVersion)
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }

    private val actor = Participant("participant", "Josh", 3)
    private val snapshot = Json.encodeToString(
        SharedSnapshot(
            "shared-list", "Shared list", 1,
            SharedListInfo(actor, listOf(actor), 2, 4), emptyList()
        )
    )
    private val operation = Json.encodeToString<SharedCommand>(SharedCommand.Rename("shared-list", "Renamed"))
    private val activity = Json.encodeToString(
        FilmActivity(
            "event", 4, actor,
            SharedFilm(12, "Saved movie", "Details", "2026-01-01"), true, 2
        )
    )

    private fun createLegacyDatabase(context: Context, name: String, sourceVersion: Int) {
        val schema = Json.parseToJsonElement(
            checkNotNull(
                javaClass.getResourceAsStream(
                    "/com.jcotters.database.MoversDatabase/$sourceVersion.json",
                )
            ).bufferedReader().use { it.readText() }).jsonObject.getValue("database").jsonObject
        SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(name).also { it.parentFile?.mkdirs() }, null
        ).use { old ->
            schema.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                fun execute(sql: String) = old.execSQL(sql.replace("\${TABLE_NAME}", table))
                execute(entity.getValue("createSql").jsonPrimitive.content)
                entity["indices"]?.jsonArray?.forEach {
                    execute(it.jsonObject.getValue("createSql").jsonPrimitive.content)
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            seedLegacyDatabase(old, sourceVersion)
            old.version = sourceVersion
        }
    }

    private fun seedLegacyDatabase(old: SQLiteDatabase, sourceVersion: Int) {
        if (sourceVersion == 9) {
            old.execSQL("INSERT INTO device_profile VALUES(1, 'stable-installation', 'Josh', 3, 1)")
            old.execSQL("INSERT INTO watchlists VALUES('shared-list','Shared list',1)")
            old.execSQL(
                "INSERT INTO shared_lists VALUES('shared-list','participant',4,?)",
                arrayOf(snapshot)
            )
            old.execSQL(
                "INSERT INTO shared_operations VALUES('operation','shared-list','participant',?)",
                arrayOf(operation)
            )
            old.execSQL("INSERT INTO shared_activity VALUES('shared-list','event',4,?)", arrayOf(activity))
            old.execSQL("INSERT INTO shared_activity_cursors VALUES('shared-list',3)")
        } else {
            old.execSQL("INSERT INTO device_profile VALUES(1, 'stable-installation', 'Josh')")
        }
        old.execSQL(
            "INSERT INTO movies(id,title,synopsis,releaseDate," +
                "runtimeMinutes,detailsFetchedAt) " +
                "VALUES(12,'Saved movie','Details','2026-01-01',125,1234)"
        )
        old.execSQL("INSERT INTO bookmarks VALUES(12)")
        old.execSQL("INSERT INTO movie_catalogue_entries VALUES('popular',12,1,0)")
        old.execSQL("INSERT INTO movie_catalogue_remote_keys VALUES('popular',2,999)")
        if (sourceVersion >= 8) {
            old.execSQL("INSERT INTO watchlists VALUES('existing-list','Existing list',1)")
            old.execSQL("INSERT INTO watchlist_movies VALUES('existing-list',12,2)")
        }
    }

    private fun openMigratedDatabase(context: Context, name: String): MoversDatabase =
        Room.databaseBuilder<MoversDatabase>(context, name)
            .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                WatchlistMigration7To8,
                com.jcotters.database.watchlists.SharedMigration8To9,
                RemoveBookmarksMigration9To10,
            ).build()

    private suspend fun assertMigratedData(fixture: MoversUiFixture, sourceVersion: Int) {
        val db = fixture.database
        assertEquals("stable-installation", db.profileDao().get()?.installationId)
        assertEquals(sourceVersion == 9, checkNotNull(db.profileDao().get()).nameConfirmed)
        assertTrue(checkNotNull(db.profileDao().get()).color in 0..7)
        if (sourceVersion >= 8) assertEquals(
            listOf(12),
            fixture.lists.observeWatchlist("existing-list").first()?.movies?.map { it.id })
        assertEquals(125, db.movieDao().getMovieById(12)?.runtimeMinutes)
        assertEquals(1234L, db.movieDao().getMovieById(12)?.detailsFetchedAt)
        db.useReaderConnection { connection ->
            connection.usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bookmarks'") {
                assertFalse(it.step())
            }
        }
        assertEquals(2, db.movieCatalogueRemoteKeysDao().getRemoteKey("popular")?.nextPage)
        val id = fixture.lists.create("Survives restart").getOrThrow()
        fixture.lists.addMovieToLists(12, setOf(id)).getOrThrow()
    }

    private suspend fun assertReopenedData(fixture: MoversUiFixture, sourceVersion: Int) {
        val list = fixture.lists.observeWatchlists().first().single { it.name == "Survives restart" }
        assertEquals("Survives restart", list.name)
        assertEquals(1, list.movieCount)
        assertEquals(listOf(12), fixture.lists.observeWatchlist(list.id).first()?.movies?.map { it.id })
        assertEquals("stable-installation", fixture.database.profileDao().get()?.installationId)
        if (sourceVersion == 9) {
            assertEquals(3, fixture.database.profileDao().get()?.color)
            val cache = fixture.database.sharedCacheDao()
            assertEquals(snapshot, cache.get("shared-list")?.payload)
            assertEquals("participant", cache.get("shared-list")?.participantId)
            assertEquals(4L, cache.get("shared-list")?.revision)
            assertEquals(operation, fixture.database.sharedOperationDao().operations().single().payload)
            assertEquals(
                activity,
                fixture.database.sharedActivityDao().observeActivity("shared-list").first().single().payload
            )
            assertEquals(3L, fixture.database.sharedActivityDao().cursor("shared-list")?.nextBefore)
        }
    }

    @Test
    fun duplicateListsIdempotentMembershipAndDeletionPreserveMovies() = runBlocking {
        MoversUiFixture().use { fixture ->
            val db = fixture.database
            db.movieDao().insertMovie(DbMovie(12, "Movie", "Synopsis", "2026-01-01", runtimeMinutes = 125))
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
                    connection.usePrepared(
                        "CREATE TRIGGER reject_second BEFORE INSERT ON watchlist_movies " +
                            "WHEN NEW.watchlistId = '$two' BEGIN SELECT RAISE(ABORT, 'test " +
                            "failure'); END"
                    ) { it.step() }
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
