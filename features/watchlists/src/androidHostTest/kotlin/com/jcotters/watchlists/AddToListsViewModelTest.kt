package com.jcotters.watchlists

import androidx.lifecycle.SavedStateHandle
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

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { lists.observeWatchlists() } returns available
        every { lists.observeListIds(12) } returns flowOf(setOf("two"))
        every { movies.observeMovieWithId(12) } returns flowOf(Movie(12, "Movie", "", ""))
    }

    @After
    fun tearDown() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test
    fun failedAddKeepsSelectionForRetryAndAlreadyAddedCannotBeToggled() = runTest {
        coEvery { lists.addMovieToLists(12, setOf("one")) } returnsMany listOf(Result.failure(IllegalStateException("offline")), Result.success(Unit))
        val model = create()
        model.load(12, "one")
        runCurrent()
        model.toggle("two")
        assertEquals(setOf("one"), model.uiState.value.destinations)
        model.add()
        model.add()
        runCurrent()
        assertEquals("offline", model.uiState.value.error)
        assertEquals(setOf("one"), model.uiState.value.destinations)
        assertFalse(model.uiState.value.completed)
        coVerify(exactly = 1) { lists.addMovieToLists(any(), any()) }
        model.add()
        runCurrent()
        assertTrue(model.uiState.value.completed)
    }

    @Test
    fun inlineCreatedListIsSelectedAndSelectionRestores() = runTest {
        coEvery { lists.create("New") } coAnswers {
            available.value += Watchlist("new", "New", 3, 0)
            Result.success("new")
        }
        val handle = SavedStateHandle()
        val model = create(handle)
        model.load(12, null)
        runCurrent()
        model.create("New")
        runCurrent()
        assertEquals("new", model.uiState.value.createdId)
        assertEquals(setOf("new"), model.uiState.value.destinations)
        val restored = create(handle)
        restored.load(12, null)
        runCurrent()
        assertEquals(setOf("new"), restored.uiState.value.destinations)
        coVerify(exactly = 0) { lists.addMovieToLists(any(), any()) }
    }

    private fun create(handle: SavedStateHandle = SavedStateHandle()) =
        AddToListsViewModel(lists, movies, handle).also { store.put("picker", it) }
}
