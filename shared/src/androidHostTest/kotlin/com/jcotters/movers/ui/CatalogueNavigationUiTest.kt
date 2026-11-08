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
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.database.MoversDatabase
import com.jcotters.internal.configureMovieApi
import com.jcotters.movers.SharedApp
import com.jcotters.movers.initializeMovers
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CatalogueNavigationUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: MoversDatabase
    private lateinit var client: HttpClient

    @Before
    fun setUp() {
        // Use Android's SQLite shadow on the host; production uses bundled SQLite.
        database = Room.inMemoryDatabaseBuilder<MoversDatabase>(ApplicationProvider.getApplicationContext())
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        client = HttpClient(MockEngine { request ->
            val body = when (request.url.encodedPath) {
                "/3/discover/movie" -> """
                    {"page":1,"total_pages":1,"results":[
                        {"id":12,"title":"Test Movie","overview":"Catalogue synopsis","release_date":"2026-01-01"}
                    ]}
                """.trimIndent()
                "/3/movie/12" -> """
                    {"id":12,"title":"Test Movie","overview":"Loaded movie details","runtime":125,
                     "release_date":"2026-01-01","original_language":"en","vote_average":8,"vote_count":10}
                """.trimIndent()
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { configureMovieApi("test-token") }
        initializeMovers(module {
            single { database }
            single { client }
            single<NetworkMonitor> { object : NetworkMonitor {
                override val networkStatus = flowOf(NetworkStatus.Online)
            } }
        })
    }

    @After
    fun tearDown() {
        compose.activityRule.scenario.close()
        stopKoin()
        if (::client.isInitialized) client.close()
        if (::database.isInitialized) database.close()
    }

    @Test
    @Config(qualifiers = "w840dp-h820dp")
    fun openMovieAndReturnOnFoldSize() = exerciseMovieJourney()

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

    private fun exerciseMovieJourney() {
        compose.setContent { SharedApp() }
        repeat(3) {
            openMovie()
            goBack()
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
        goBack()
        awaitText("No Saved Movies")
    }

    private fun openMovie() {
        awaitText("Test Movie")
        compose.onNodeWithText("Test Movie").performClick()
        awaitText("Loaded movie details")
        awaitBookmark()
        compose.onNodeWithContentDescription("Bookmark movie button").assertIsDisplayed().assertIsEnabled()
    }

    private fun goBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
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
