package com.jcotters.movers.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cotters.data.network.NetworkStatus
import com.jcotters.auth.domain.UserSession
import com.jcotters.auth.navigation.AuthKey
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/** Run by the user on a physical device; these tests never need a service or real credentials. */
class MoversNavigationShellTest {
    @get:Rule
    val compose = createComposeRule()
    private val registry = ProbeRegistry()
    private val session = mutableStateOf<UserSession>(UserSession.Authenticated(1))
    private lateinit var state: NavigationState

    @Test
    fun retainIndependentDetailStoresAndClearOnlyPoppedEntries() {
        showShell()
        compose.onNodeWithText("Open movie").performClick()
        lateinit var homeDetails: ProbeViewModel
        compose.runOnIdle { homeDetails = registry.latest("Home movie") }

        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Open movie").performClick()
        lateinit var profileDetails: ProbeViewModel
        compose.runOnIdle {
            profileDetails = registry.latest("Profile movie")
            assertNotSame(homeDetails, profileDetails)
        }

        compose.onNodeWithText("Profile").performClick()
        compose.waitUntil(5_000) { profileDetails.cleared }
        compose.onNodeWithText("Home").performClick()
        compose.runOnIdle { assertSame(homeDetails, registry.latest("Home movie")) }
        compose.onNodeWithText("Home").performClick()
        compose.waitUntil(5_000) { homeDetails.cleared }
        compose.onNodeWithText("Home content").assertIsDisplayed()
    }

    @Test
    fun loginReleasesAuthStoreAndPreservesTheHomeStore() {
        session.value = UserSession.NotAuthenticated
        showShell()
        lateinit var home: ProbeViewModel
        compose.runOnIdle { home = registry.latest("Home") }
        compose.onNodeWithText("Profile").performClick()
        lateinit var auth: ProbeViewModel
        compose.runOnIdle { auth = registry.latest("Auth") }

        compose.runOnIdle { session.value = UserSession.Authenticated(1) }

        compose.onNodeWithText("Profile content").assertIsDisplayed()
        compose.waitUntil(5_000) { auth.cleared }
        compose.runOnIdle {
            assertSame(home, registry.latest("Home"))
            assertEquals(1, registry.instances.getValue("Home").size)
        }
    }

    @Test
    fun expiryClearsInactiveStoresAndReturnsHome() {
        showShell()
        compose.onNodeWithText("Open movie").performClick()
        lateinit var details: ProbeViewModel
        compose.runOnIdle { details = registry.latest("Home movie") }
        compose.onNodeWithText("Profile").performClick()
        lateinit var profile: ProbeViewModel
        compose.runOnIdle { profile = registry.latest("Profile") }

        compose.runOnIdle { session.value = UserSession.NotAuthenticated }

        compose.onNodeWithText("Home content").assertIsDisplayed()
        compose.waitUntil(5_000) { details.cleared && profile.cleared }
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Auth content").assertIsDisplayed()
    }

    @Test
    fun restoreSelectedTabAndStableDetailIdentity() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Open movie").performClick()
        lateinit var details: MovieDetailsKey
        compose.runOnIdle { details = state.currentStack.last() as MovieDetailsKey }

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Profile movie content").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(TopLevelDestination.Profile, state.selectedTab)
            assertEquals(details, state.currentStack.last())
        }
    }

    private fun showShell() {
        compose.setContent { Shell() }
    }

    @Composable
    private fun Shell() {
        val navigationState = rememberNavigationState((session.value as? UserSession.Authenticated)?.userId)
        SideEffect { state = navigationState }
        MaterialTheme {
            MoversNavigationShell(session.value, NetworkStatus.Online, installers, navigationState)
        }
    }

    private val installers: Set<EntryProviderInstaller> = setOf({ navigator, stackId ->
        entry<CatalogueKey> { ProbeContent("Home") { navigator.navigate(MovieDetailsKey(7)) } }
        entry<ProfileKey> { ProbeContent("Profile") { navigator.navigate(MovieDetailsKey(7)) } }
        entry<MovieDetailsKey> { ProbeContent("$stackId movie") }
        entry<WatchlistsKey> { ProbeContent("Lists") }
        entry<AuthKey> { ProbeContent("Auth") }
    })

    @Composable
    private fun ProbeContent(label: String, openMovie: (() -> Unit)? = null) {
        viewModel<ProbeViewModel>(factory = viewModelFactory {
            initializer { ProbeViewModel(label, registry) }
        })
        Column {
            Text("$label content")
            if (openMovie != null) Button(onClick = openMovie) { Text("Open movie") }
        }
    }
}

private class ProbeRegistry {
    val instances = mutableMapOf<String, MutableList<ProbeViewModel>>()
    fun latest(label: String): ProbeViewModel = instances.getValue(label).last()
}

private class ProbeViewModel(label: String, registry: ProbeRegistry) : ViewModel() {
    @Volatile
    var cleared = false
        private set

    init {
        registry.instances.getOrPut(label) { mutableListOf() }.add(this)
    }

    override fun onCleared() {
        cleared = true
    }
}
