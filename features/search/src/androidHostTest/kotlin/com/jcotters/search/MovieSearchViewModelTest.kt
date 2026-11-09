package com.jcotters.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val queries = mutableListOf<String>()
    private val cancelled = mutableListOf<String>()
    private val lists = mockk<IWatchlistsRepository>()
    private val search = object : IMovieSearchRepository {
        override fun searchMovies(query: String): Flow<PagingData<Movie>> = flow {
            queries += query
            try { emit(PagingData.empty()); awaitCancellation() } finally { cancelled += query }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { lists.observeWatchlists() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test
    fun waits600msAndSkipsBlankOrUnchangedQueries() = runTest {
        val viewModel = create()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.movies.collect {} }
        runCurrent()
        viewModel.changeQuery(" matrix ")
        runCurrent()
        advanceTimeBy(599)
        runCurrent()
        assertTrue(queries.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("matrix"), queries)
        viewModel.changeQuery("matrix")
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("matrix"), queries)
        viewModel.changeQuery(" ")
        runCurrent()
        assertEquals(listOf("matrix"), cancelled)
    }

    @Test
    fun immediateSubmitCancelsOldSearchAndDoesNotDuplicateDebounce() = runTest {
        val viewModel = create()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.movies.collect {} }
        runCurrent()
        viewModel.changeQuery("first")
        viewModel.submit()
        runCurrent()
        assertEquals(listOf("first"), queries)
        viewModel.changeQuery("second")
        viewModel.submit()
        runCurrent()
        assertEquals(listOf("first", "second"), queries)
        assertEquals(listOf("first"), cancelled)
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("first", "second"), queries)
    }

    private fun create() = MovieSearchViewModel(search, lists, SavedStateHandle()).also { store.put("search", it) }

    @Test
    fun membershipObservationCanRetryAfterReadFailure() = runTest {
        var attempts = 0
        every { lists.observeMovieIds("list") } returns flow {
            if (++attempts == 1) error("read failed")
            emit(setOf(12))
        }
        val model = create()
        model.setTarget("list")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.addedIds.collect {} }
        runCurrent()
        assertEquals("Unable to read list membership.", model.actionsState.value.error)
        model.clearError()
        model.retryLists()
        runCurrent()
        assertEquals(setOf(12), model.addedIds.value)
        assertEquals(2, attempts)
    }
}
