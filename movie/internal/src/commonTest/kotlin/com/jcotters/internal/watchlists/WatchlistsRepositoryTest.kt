package com.jcotters.internal.watchlists

import com.jcotters.contract.time.AppClock
import com.jcotters.contract.watchlists.data.WatchlistContentRecord
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.contract.watchlists.data.WatchlistRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WatchlistsRepositoryTest {
    @Test
    fun trimsNamesButAllowsIndependentCaseInsensitiveDuplicates() = runTest {
        val database = TestDatabase()
        val repository = WatchlistsRepository(database, AppClock { 100L })
        val first = repository.create(" Weekend ").getOrThrow()
        val second = repository.create("weekend").getOrThrow()
        assertNotEquals(first, second)
        assertEquals(listOf("Weekend", "weekend"), repository.observeWatchlists().first().map { it.name })
        assertTrue(repository.create("   ").isFailure)
        assertEquals(2, database.rows.value.size)
    }

    @Test
    fun validatesSelectionAndUsesOneAtomicStorageOperation() = runTest {
        val database = TestDatabase()
        val repository = WatchlistsRepository(database, AppClock { 123L })
        assertTrue(repository.addMovieToLists(12, emptySet()).isFailure)
        repository.addMovieToLists(12, setOf("a", "b")).getOrThrow()
        assertEquals(listOf(Triple(12, setOf("a", "b"), 123L)), database.additions)
    }

    @Test
    fun propagatesCancellationAndFatalErrors() = runTest {
        val database = TestDatabase()
        val repository = WatchlistsRepository(database, AppClock { 0L })
        database.failure = CancellationException("Cancelled")
        assertFailsWith<CancellationException> { repository.create("One") }
        database.failure = AssertionError("Fatal")
        assertFailsWith<AssertionError> { repository.create("One") }
    }

    private class TestDatabase : WatchlistDatabase {
        val rows = MutableStateFlow<List<WatchlistRecord>>(emptyList())
        val additions = mutableListOf<Triple<Int, Set<String>, Long>>()
        var failure: Throwable? = null
        override fun observeLists() = rows
        override fun observeContent(id: String) = flowOf<WatchlistContentRecord?>(null)
        override fun observeMovieIds(listId: String) = flowOf(emptySet<Int>())
        override fun observeListIds(movieId: Int) = flowOf(emptySet<String>())
        override suspend fun create(id: String, name: String, createdAt: Long) {
            failure?.let { throw it }
            rows.value += WatchlistRecord(id, name, createdAt, 0)
        }
        override suspend fun rename(id: String, name: String) { rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it } }
        override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        override suspend fun addMovieToLists(movieId: Int, listIds: Set<String>, addedAt: Long) { additions += Triple(movieId, listIds, addedAt) }
        override suspend fun removeMovie(listId: String, movieId: Int) = Unit
        override suspend fun importMovies(requestId: String, listId: String, films: List<com.jcotters.contract.watchlists.data.SharedFilm>, addedAt: Long) =
            com.jcotters.contract.watchlists.data.ImportBatchResult(films.map {
                com.jcotters.contract.watchlists.data.FilmImportOutcome(it.id, com.jcotters.contract.watchlists.data.ImportDisposition.Imported)
            })
    }
}
