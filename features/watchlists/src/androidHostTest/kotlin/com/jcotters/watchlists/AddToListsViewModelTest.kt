package com.jcotters.watchlists

import androidx.lifecycle.ViewModelStore
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddToListsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val lists = mockk<IWatchlistsRepository>()
    private val movies = mockk<IMovieDetailsRepository>()
    private val available = MutableStateFlow(listOf(Watchlist("one", "One", 1, 0), Watchlist("two", "Two", 2, 1)))
    private val memberships = MutableStateFlow(setOf("two"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { lists.observeWatchlists() } returns available
        every { lists.observeListIds(12) } returns memberships
        every { movies.observeMovieWithId(12) } returns flowOf(Movie(12, "Movie", "", ""))
    }

    @After
    fun tearDown() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test
    fun failedWritesKeepThePreviousCheckboxStateAndCanBeRetried() = runTest {
        coEvery { lists.addMovieToLists(12, setOf("one")) } returns Result.failure(IllegalStateException("add failed"))
        coEvery { lists.removeMovie("two", 12) } returns Result.failure(IllegalStateException("remove failed"))
        val model = create()
        model.load(12)
        runCurrent()
        model.setMembership("one", true)
        model.setMembership("one", true)
        runCurrent()
        assertEquals("add failed", model.uiState.value.error)
        assertEquals(setOf("two"), model.uiState.value.existing)
        coVerify(exactly = 1) { lists.addMovieToLists(any(), any()) }
        model.setMembership("two", false)
        runCurrent()
        assertEquals("remove failed", model.uiState.value.error)
        assertEquals(setOf("two"), model.uiState.value.existing)
        assertFalse(model.uiState.value.busy)
        coEvery { lists.removeMovie("two", 12) } coAnswers { memberships.value = emptySet(); Result.success(Unit) }
        model.setMembership("two", false)
        runCurrent()
        assertTrue(model.uiState.value.existing.isEmpty())
    }

    @Test
    fun inlineCreationAutomaticallyAddsAndFailedAddLeavesTheListAvailableForRetry() = runTest {
        coEvery { lists.create("New") } coAnswers {
            available.value += Watchlist("new", "New", 3, 0)
            Result.success("new")
        }
        coEvery { lists.addMovieToLists(12, setOf("new")) } returns Result.failure(IllegalStateException("add failed"))
        val model = create()
        model.load(12)
        runCurrent()
        model.create("New")
        runCurrent()
        assertEquals("new", model.uiState.value.createdId)
        assertTrue(model.uiState.value.lists.any { it.id == "new" })
        assertFalse("new" in model.uiState.value.existing)
        assertFalse(model.uiState.value.busy)
        coEvery { lists.addMovieToLists(12, setOf("new")) } coAnswers { memberships.value += "new"; Result.success(Unit) }
        model.setMembership("new", true)
        runCurrent()
        assertTrue("new" in model.uiState.value.existing)
        coVerify(exactly = 1) { lists.create("New") }
    }

    private fun create() = AddToListsViewModel(lists, movies).also { store.put("picker", it) }
}
