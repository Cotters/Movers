package com.jcotters.movers.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.printToString
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.jcotters.catalogue.MovieCatalogueScreen
import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.designsystem.MoversDesignTheme
import com.jcotters.movers.SharedApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CataloguePagingUiTest {
    @get:Rule(order = 0)
    val drainUiDispatcher = object : TestWatcher() {
        override fun finished(description: Description) {
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fixture: MoversUiFixture
    private lateinit var pagingItems: LazyPagingItems<Movie>

    @After
    fun tearDown() {
        compose.activityRule.scenario.close()
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun consecutiveRoomInvalidationsKeepAbsoluteMoviePositionsAndVisibleOffset() {
        showCatalogue(PagedCatalogueFixture())
        scrollToFilm(180)
        val before = posterBounds(180)
        repeat(2) { revision ->
            val previousSnapshot = pagingItems.itemSnapshotList
            val title = PagedCatalogueFixture.title(180, revision + 1)
            runBlocking {
                val movie = checkNotNull(fixture.database.movieDao().getMovieById(180))
                fixture.database.movieDao().saveMovieDetails(movie.copy(title = title))
            }
            compose.waitUntil("Room's invalidation presents a new paging generation", 10_000) {
                compose.waitForIdle()
                pagingItems.itemSnapshotList !== previousSnapshot && sourceIsIdle()
            }
            compose.runOnIdle {
                val snapshot = pagingItems.itemSnapshotList
                assertEquals("The database result must retain its absolute positions", 300, snapshot.size)
                assertEquals("Film 180 must stay at database index 179", 180, snapshot[179]?.id)
                assertEquals("The refreshed movie contains the database update", title, snapshot[179]?.title)
                val loadedIds = snapshot.items.map { it.id }
                assertEquals("Loaded films remain ordered and unique", loadedIds.distinct().sorted(), loadedIds)
            }
            assertPosterPosition(180, before, revision + 1)
        }
    }

    @Test
    fun listKeepsVisibleMovieAndOffsetAcrossConsecutiveNetworkAppends() = exerciseAppends(grid = false)

    @Test
    fun gridKeepsVisibleMovieAndOffsetAcrossConsecutiveNetworkAppends() = exerciseAppends(grid = true)

    @Test
    fun listReturnsToSamePosterAfterEnrichingDifferentDeepMovies() = exerciseDetails(grid = false)

    @Test
    fun gridReturnsToSamePosterAfterEnrichingDifferentDeepMovies() = exerciseDetails(grid = true)

    @Test
    fun listLoadingSlotsHaveRealisticHeightAndDoNotMoveTheFollowingFilmWhenLoaded() =
        exerciseLoadingSlots(grid = false)

    @Test
    fun gridLoadingSlotsHaveRealisticHeightAndDoNotMoveTheFollowingFilmWhenLoaded() =
        exerciseLoadingSlots(grid = true)

    @Test
    fun restoredListPositionSurvivesAnEmptyNotLoadingSnapshot() = exerciseEmptySnapshotRestoration(grid = false)

    @Test
    fun restoredGridPositionSurvivesAnEmptyNotLoadingSnapshot() = exerciseEmptySnapshotRestoration(grid = true)

    private fun exerciseEmptySnapshotRestoration(grid: Boolean) {
        val movies = (1..300).map { Movie(it, PagedCatalogueFixture.title(it), "Catalogue synopsis", "2026-01-01") }
        val idle = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))
        val flow = MutableStateFlow(PagingData.from(movies, sourceLoadStates = idle))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val items = flow.collectAsLazyPagingItems()
            SideEffect { pagingItems = items }
            MoversDesignTheme {
                MovieCatalogueScreen(items, posterModifier = { Modifier }, onMovieTapped = {})
            }
        }
        awaitFilm(1)
        if (grid) compose.onNodeWithContentDescription("Show grid").performClick()
        scrollToFilm(180)
        val before = posterBounds(180)
        flow.value = PagingData.from(emptyList(), sourceLoadStates = idle)
        compose.waitUntil("The empty NotLoading snapshot is presented", 10_000) {
            compose.waitForIdle()
            pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Popular Movies").assertIsDisplayed()
        flow.value = PagingData.from(movies, sourceLoadStates = idle)
        compose.waitUntil("Data returns after restoring the empty snapshot", 10_000) {
            compose.waitForIdle()
            pagingItems.itemCount == 300
        }
        assertPosterPosition(180, before)
    }

    @Test
    fun loadingSlotsDoNotFetchTheEntireRemoteCatalogueWithoutFurtherScrolling() {
        val catalogue = PagedCatalogueFixture(cachedPages = 5, totalPages = 12, pausedPages = setOf(6))
        showCatalogue(catalogue)
        scrollToFilm(95)
        compose.waitUntil("The first network append is held", 10_000) {
            compose.waitForIdle()
            catalogue.requested(6)
        }
        catalogue.release(6)
        compose.waitUntil("Paging settles after filling its initial database window", 10_000) {
            compose.waitForIdle()
            pagingItems.itemCount >= 120 && sourceIsIdle() && pagingItems.loadState.append !is LoadState.Loading
        }
        assertEquals(
            "Unloaded slots must not cause loading through all twelve pages",
            setOf(6, 7),
            catalogue.requestedPages
        )
    }

    private fun exerciseLoadingSlots(grid: Boolean) {
        val movies = (1..12).map { Movie(it, PagedCatalogueFixture.title(it), "Catalogue synopsis", "2026-01-01") }
        val flow = MutableStateFlow(PagingData.from(movies.drop(1), placeholdersBefore = 1, placeholdersAfter = 1000))
        compose.setContent {
            MoversDesignTheme {
                MovieCatalogueScreen(flow.collectAsLazyPagingItems(), posterModifier = { Modifier }, onMovieTapped = {})
            }
        }
        awaitFilm(2)
        if (grid) compose.onNodeWithContentDescription("Show grid").performClick()
        val placeholder = compose.onNodeWithContentDescription("Loading movie")
            .assertIsDisplayed().assertHasNoClickAction().fetchSemanticsNode().boundsInRoot
        assertTrue("A loading movie must reserve real item height", placeholder.height > 150f)
        val before = posterBounds(2)
        flow.value = PagingData.from(movies, placeholdersAfter = 1000)
        awaitFilm(1)
        compose.onNodeWithContentDescription("Loading movie").assertDoesNotExist()
        assertPosterPosition(2, before)
    }

    private fun exerciseAppends(grid: Boolean) {
        val catalogue = PagedCatalogueFixture(cachedPages = 5, totalPages = 12, pausedPages = (6..12).toSet())
        showCatalogue(catalogue, grid = grid)
        for (page in 6..8) {
            val anchor = (page - 1) * PagedCatalogueFixture.PAGE_SIZE - 5
            scrollToFilm(anchor)
            compose.waitUntil("Network page $page is requested and held", 10_000) {
                compose.waitForIdle()
                catalogue.requested(page)
            }
            val before = posterBounds(anchor)
            catalogue.release(page)
            val lastId = page * PagedCatalogueFixture.PAGE_SIZE
            try {
                compose.waitUntil("The appended page is presented by Paging (page $page)", 10_000) {
                    compose.waitForIdle()
                    val snapshot = pagingItems.itemSnapshotList
                    if (pagingItems.loadState.append is LoadState.Error) {
                        throw AssertionError("Append failed: ${pagingItems.loadState}")
                    }
                    snapshot.size == lastId && sourceIsIdle()
                }
            } catch (failure: ComposeTimeoutException) {
                val snapshot = pagingItems.itemSnapshotList
                throw AssertionError(
                    "Append $page: count=${snapshot.size}, loaded=${snapshot.items.firstOrNull()?.id}.." +
                        "${snapshot.items.lastOrNull()?.id}, requests=${catalogue.requestedPages}, " +
                        "states=${pagingItems.loadState}",
                    failure,
                )
            }
            assertPosterPosition(anchor, before)
            assertTrue(
                "Only completed pages and at most one pending prefetch request are allowed: " +
                    catalogue.requestedPages,
                catalogue.requestedPages.containsAll((6..page).toSet()) &&
                    catalogue.requestedPages.all { it in 6..page + 1 },
            )
        }
    }

    private fun exerciseDetails(grid: Boolean) {
        fixture = MoversUiFixture(pagedCatalogue = PagedCatalogueFixture())
        compose.setContent { SharedApp() }
        awaitFilm(1)
        if (compose.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip").performClick()
        }
        if (grid) compose.onNodeWithContentDescription("Show grid").performClick()
        for (id in listOf(180, 184)) {
            scrollToFilm(id)
            val before = posterBounds(id)
            compose.onNodeWithContentDescription(posterDescription(id)).performClick()
            compose.waitUntil("Detail enrichment is persisted before returning", 10_000) {
                compose.waitForIdle()
                runBlocking { fixture.database.movieDao().getMovieById(id)?.detailsFetchedAt != null }
            }
            compose.onNodeWithContentDescription("Back").performClick()
            assertPosterPosition(id, before)
        }
    }

    private fun showCatalogue(catalogue: PagedCatalogueFixture, grid: Boolean = false) {
        fixture = MoversUiFixture(pagedCatalogue = catalogue)
        val repository = GlobalContext.get().get<IMovieCatalogueRepository>()
        compose.setContent {
            val scope = rememberCoroutineScope()
            val flow = remember(repository, scope) { repository.getPopularMoviesPaging().cachedIn(scope) }
            val movies = flow.collectAsLazyPagingItems()
            SideEffect { pagingItems = movies }
            MoversDesignTheme {
                MovieCatalogueScreen(movies, posterModifier = { Modifier }, onMovieTapped = {})
            }
        }
        awaitFilm(1)
        if (grid) compose.onNodeWithContentDescription("Show grid").performClick()
    }

    private fun scrollToFilm(id: Int) {
        // Visit cached pages in order; a single jump would depend on placeholder support itself.
        for (index in (20 until id step 20) + id) {
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            awaitFilm(index)
            if (::pagingItems.isInitialized) {
                compose.waitUntil("Cached page loads have settled", 10_000) {
                    compose.waitForIdle()
                    sourceIsIdle()
                }
            }
        }
    }

    private fun sourceIsIdle(): Boolean = pagingItems.loadState.source.run {
        refresh !is LoadState.Loading && append !is LoadState.Loading && prepend !is LoadState.Loading
    }

    private fun awaitFilm(id: Int) {
        compose.waitUntil("Film $id is composed", 10_000) {
            compose.onAllNodesWithText(PagedCatalogueFixture.title(id)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun posterDescription(id: Int, revision: Int = 0): String =
        "Movie poster for ${PagedCatalogueFixture.title(id, revision)}"

    private fun posterBounds(id: Int, revision: Int = 0): Rect =
        compose.onNodeWithContentDescription(posterDescription(id, revision), useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun assertPosterPosition(id: Int, before: Rect, revision: Int = 0) {
        compose.waitForIdle()
        val matches = compose.onAllNodesWithContentDescription(posterDescription(id, revision), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(
            "The original poster must remain composed without corrective scrolling. " +
                if (matches.isEmpty()) compose.onRoot(useUnmergedTree = true).printToString(maxDepth = 8) else "",
            1,
            matches.size,
        )
        val after = posterBounds(id, revision)
        assertTrue("Film $id moved vertically: $before -> $after", kotlin.math.abs(before.top - after.top) <= 1f)
        assertEquals("Poster height should be preserved", before.height, after.height, 1f)
    }
}
