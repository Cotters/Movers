package com.jcotters.movers.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jcotters.movers.SharedApp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CatalogueNavigationUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fixture: MoversUiFixture

    @Before
    fun setUp() { fixture = MoversUiFixture() }

    @After
    fun tearDown() {
        compose.activityRule.scenario.close()
        fixture.close()
    }

    @Test
    @Config(qualifiers = "w840dp-h820dp")
    fun openMovieAndReturnOnFoldSize() = exerciseMovieJourney(useSystemBack = true)

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun openMovieAndReturnOnPhoneSize() = exerciseMovieJourney()

    @Test
    fun restoreMovieDetailsAndReturnToCatalogue() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { SharedApp() }
        openMovie()
        restoration.emulateSavedInstanceStateRestore()
        awaitText("Loaded movie details")
        goBack()
        compose.onNodeWithText("Popular Movies").assertIsDisplayed()
    }

    private fun exerciseMovieJourney(useSystemBack: Boolean = false) {
        compose.setContent { SharedApp() }
        repeat(3) {
            openMovie()
            goBack(useSystemBack)
            compose.onNodeWithText("Popular Movies").assertIsDisplayed()
        }
        openMovie()
        compose.onNodeWithContentDescription("Profile").performClick()
        awaitText("Josh")
        compose.onNodeWithText("Edit name").assertIsDisplayed()
        compose.onNodeWithText("Bookmarks").assertDoesNotExist()
        compose.onNodeWithContentDescription("Home").performClick()
        awaitText("Loaded movie details")
        goBack(useSystemBack)
        compose.onNodeWithText("Popular Movies").assertIsDisplayed()
    }

    private fun openMovie() {
        awaitText("Test Movie")
        compose.onNodeWithText("Test Movie").performClick()
        awaitText("Loaded movie details")
        compose.onNodeWithContentDescription("Add to list").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithContentDescription("Bookmark movie button").assertDoesNotExist()
    }

    private fun goBack(useSystemBack: Boolean = false) {
        if (useSystemBack) {
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        } else {
            compose.onNodeWithContentDescription("Back").performClick()
        }
        compose.waitForIdle()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

}
