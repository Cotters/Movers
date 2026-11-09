package com.jcotters.movers.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
        compose.onNodeWithContentDescription("Bookmark movie button").performClick()
        compose.onNodeWithText("Profile").performClick()
        awaitText("Josh")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription("Movie poster for Test Movie").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Movie poster for Test Movie").assertIsDisplayed().performClick()
        awaitText("Loaded movie details")
        awaitBookmark()
        compose.onNodeWithContentDescription("Bookmark movie button").assertIsEnabled().performClick()
        goBack(useSystemBack)
        awaitText("No Saved Movies")
    }

    private fun openMovie() {
        awaitText("Test Movie")
        compose.onNodeWithText("Test Movie").performClick()
        awaitText("Loaded movie details")
        awaitBookmark()
        compose.onNodeWithContentDescription("Bookmark movie button").assertIsDisplayed().assertIsEnabled()
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

    private fun awaitBookmark() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasContentDescription("Bookmark movie button") and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
