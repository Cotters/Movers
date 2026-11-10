package com.jcotters.movers.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import com.jcotters.database.movies.DbMovie
import com.jcotters.movers.SharedApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WatchlistsUiTest {
    @get:Rule(order = 0)
    val drainUiDispatcher = object : TestWatcher() {
        override fun finished(description: Description) {
            // Paging's process-wide dispatcher must drain after the Compose rule disposes content.
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: MoversUiFixture

    @Before fun setUp() { fixture = MoversUiFixture() }
    @After fun tearDown() {
        compose.activityRule.scenario.close()
        fixture.close()
    }

    @Test
    fun createDuplicateNamesRenameAndDeleteOnlyOneList() {
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithContentDescription("Watchlists").performClick()
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
        compose.waitUntil("Rename dialog closes and list controls become editable", 10_000) {
            compose.onAllNodesWithText("List name").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodes(hasContentDescription("List options") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
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
        awaitMembership("One", true)
        awaitMembership("Two", true)
        restore.emulateSavedInstanceStateRestore()
        awaitMembership("One", true)
        awaitMembership("Two", true)
        compose.onAllNodesWithText("Add to 2 lists").assertCountEquals(0)
        compose.onNodeWithText("Close").performClick()
        awaitSheetClosed()
        compose.onNodeWithText("Popular Movies").assertIsDisplayed()
        compose.onNodeWithText("Test Movie").performClick()
        awaitText("Loaded movie details")
        compose.onNodeWithContentDescription("Add to list").performClick()
        awaitMembership("One", true)
        awaitMembership("Two", true)
        compose.onNodeWithText("One").performClick()
        awaitMembership("One", false)
        compose.onNodeWithText("Close").performClick()
        awaitSheetClosed()
        compose.onNodeWithText("Loaded movie details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Watchlists").performClick()
        awaitText("One")
        compose.onAllNodesWithText("1 movie").assertCountEquals(1)
        compose.onAllNodesWithText("0 movies").assertCountEquals(1)
    }

    @Test
    fun targetedSearchStaysOpenForConsecutiveAdds() {
        runBlocking { fixture.lists.create("Tonight").getOrThrow() }
        compose.setContent { SharedApp() }
        awaitText("Test Movie")
        compose.onNodeWithContentDescription("Watchlists").performClick()
        awaitText("Tonight")
        compose.onNodeWithText("Tonight").performClick()
        compose.onNodeWithContentDescription("Add movies").performClick()
        compose.onNodeWithText("Search films").performTextInput("test")
        awaitText("Search Movie")
        compose.onNodeWithContentDescription("Add Search Movie to list").performClick()
        awaitAction("Remove Search Movie from list")
        compose.onNodeWithContentDescription("Remove Search Movie from list").performClick()
        awaitAction("Add Search Movie to list")
        compose.onNodeWithContentDescription("Add Search Movie to list").performClick()
        awaitAction("Remove Search Movie from list")
        compose.onNodeWithContentDescription("Add Another Movie to list").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasContentDescription("Remove ", substring = true) and isEnabled()).fetchSemanticsNodes().size == 2 }
        compose.onNodeWithText("Search films").assertTextContains("test")
        compose.onNodeWithContentDescription("Back").performClick()
        awaitText("2 movies")
        compose.onNodeWithText("Search Movie").assertIsDisplayed()
        compose.onAllNodes(hasContentDescription("Remove ", substring = true)).assertCountEquals(0)
        compose.onNodeWithContentDescription("List options").performClick()
        compose.onNodeWithText("Edit list").performClick()
        compose.onAllNodes(hasContentDescription("Remove ", substring = true))[0].performClick()
        awaitText("1 movie")
        compose.onNodeWithText("Done").performClick()
        compose.onAllNodes(hasContentDescription("Remove ", substring = true)).assertCountEquals(0)
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
        awaitMembership("Search picks", true)
        compose.onAllNodesWithText("Add to 1 list").assertCountEquals(0)
        compose.onNodeWithText("Close").performClick()
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
        try {
            compose.waitUntil("Text '$text' appears", 10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: ComposeTimeoutException) {
            val tree = runCatching { compose.onRoot(useUnmergedTree = true).printToString() }.getOrDefault("No single root available")
            throw AssertionError("Expected '$text'.\n$tree", error)
        }
    }

    private fun awaitAction(description: String) {
        compose.waitUntil("Action '$description' is ready", 10_000) {
            compose.onAllNodes(hasContentDescription(description) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitSheetClosed() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Close").fetchSemanticsNodes().isEmpty() }
    }

    private fun awaitMembership(name: String, added: Boolean) {
        compose.waitUntil("Membership '$name' becomes $added and is editable", 10_000) {
            compose.onAllNodes(hasText(name) and isEnabled() and (if (added) isOn() else isOff()))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

}
