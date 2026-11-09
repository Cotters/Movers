package com.jcotters.movers.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.jcotters.database.movies.DbMovie
import com.jcotters.movers.SharedApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WatchlistsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: MoversUiFixture

    @Before fun setUp() { fixture = MoversUiFixture() }
    @After fun tearDown() { compose.activityRule.scenario.close(); fixture.close() }

    @Test
    fun createDuplicateNamesRenameAndDeleteOnlyOneList() {
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithText("Watchlists").performClick()
        awaitText("Create a list for your next movie night.")
        createList("Weekend")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Create list").performClick()
        compose.onNodeWithText("List name").performTextInput("weekend")
        compose.onNodeWithText("A list with this name already exists. You can still use this name.").assertIsDisplayed()
        compose.onNodeWithText("Create", useUnmergedTree = true).performClick()
        awaitText("weekend")
        compose.onNodeWithContentDescription("List options").performClick()
        compose.onNodeWithText("Rename list").performClick()
        compose.onNodeWithText("List name").performTextReplacement("Classics")
        compose.onNodeWithText("Save").performClick()
        awaitText("Classics")
        compose.onNodeWithContentDescription("List options").performClick()
        compose.onNodeWithText("Delete list").performClick()
        compose.onNodeWithText("Delete").performClick()
        awaitText("Weekend")
        compose.onAllNodesWithText("Classics").assertCountEquals(0)
    }

    @Test
    fun catalogueAndDetailPickerAddToMultipleListsWithoutLeavingTheirSource() {
        runBlocking { fixture.lists.create("One").getOrThrow(); fixture.lists.create("Two").getOrThrow() }
        val restore = StateRestorationTester(compose)
        restore.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithContentDescription("Add to list").performClick()
        awaitText("One")
        compose.onNodeWithText("One").performClick()
        compose.onNodeWithText("Two").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Add to 2 lists").performClick()
        awaitSheetClosed()
        compose.onNodeWithText("Popular Movies").assertIsDisplayed()
        compose.onNodeWithText("Test Movie").performClick()
        awaitText("Loaded movie details")
        compose.onNodeWithContentDescription("Add to list").performClick()
        awaitText("Already added")
        compose.onAllNodesWithText("Already added").assertCountEquals(2)
        compose.onNodeWithText("Close").performClick()
        awaitSheetClosed()
        compose.onNodeWithText("Loaded movie details").assertIsDisplayed()
        compose.onNodeWithText("Watchlists").performClick()
        awaitText("One")
        compose.onAllNodesWithText("1 movie").assertCountEquals(2)
    }

    @Test
    fun targetedSearchStaysOpenForConsecutiveAdds() {
        runBlocking { fixture.lists.create("Tonight").getOrThrow() }
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithText("Watchlists").performClick()
        awaitText("Tonight")
        compose.onNodeWithText("Tonight").performClick()
        compose.onNodeWithContentDescription("Add movies").performClick()
        compose.onNodeWithText("Search films").performTextInput("test")
        awaitText("Search Movie")
        compose.onAllNodesWithText("Add")[0].performClick()
        awaitText("Added")
        compose.onAllNodesWithText("Add")[0].performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Added").fetchSemanticsNodes().size == 2 }
        compose.onNodeWithText("Search films").assertTextContains("test")
        compose.onNodeWithContentDescription("Back").performClick()
        awaitText("2 movies")
        compose.onNodeWithText("Search Movie").assertIsDisplayed()
        compose.onAllNodesWithText("Remove")[0].performClick()
        awaitText("1 movie")
    }

    @Test
    fun globalSearchSupportsInlineCreationAndRetainsQueryAfterAdding() {
        runBlocking {
            fixture.database.movieDao().insertMovie(DbMovie(
                25, "Search Movie", "Cached details", "2026-02-01", runtimeMinutes = 120, detailsFetchedAt = 1234L,
            ))
        }
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithContentDescription("Search movies").performClick()
        compose.onNodeWithText("Search films").performTextInput("search")
        awaitText("Search Movie")
        runBlocking {
            val cached = fixture.database.movieDao().getMovieById(25)
            assertEquals(120, cached?.runtimeMinutes)
            assertEquals(1234L, cached?.detailsFetchedAt)
        }
        compose.onAllNodesWithContentDescription("Add to list")[0].performClick()
        awaitText("Create new list")
        compose.onNodeWithText("Create new list").performClick()
        compose.onNodeWithText("List name").performTextInput("Search picks")
        compose.onNodeWithText("Create").performClick()
        awaitText("Add to 1 list")
        compose.onNodeWithText("Add to 1 list").performClick()
        awaitSheetClosed()
        compose.onNodeWithText("Search films").assertTextContains("search")
        compose.onNodeWithText("Search Movie").assertIsDisplayed()
    }

    private fun createList(name: String) {
        compose.onNodeWithText("Create list").performClick()
        compose.onNodeWithText("List name").performTextInput(name)
        compose.onNodeWithText("Create", useUnmergedTree = true).performClick()
        awaitText(name)
    }

    @Test
    fun searchFailureRetriesAndEmptyQueryResultsStayUseful() {
        fixture.failSearch = true
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithContentDescription("Search movies").performClick()
        compose.onNodeWithText("Search films").performTextInput("search")
        awaitText("Unable to search for movies. Check your connection and retry.")
        fixture.failSearch = false
        compose.onNodeWithText("Retry").performClick()
        awaitText("Search Movie")
        compose.onNodeWithText("Search films").performTextReplacement("none")
        awaitText("No movies found.")
        compose.onNodeWithText("Clear").performClick()
        awaitText("Search for a movie to add to your lists.")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitSheetClosed() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Close").fetchSemanticsNodes().isEmpty() }
    }
}
