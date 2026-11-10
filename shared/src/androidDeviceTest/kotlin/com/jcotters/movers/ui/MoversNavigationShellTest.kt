package com.jcotters.movers.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cotters.data.network.NetworkStatus
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoversNavigationShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detailsInDifferentTabsHaveIndependentRetainedViewModels() {
        val probes = mutableMapOf<String, ProbeViewModel>()
        compose.setContent {
            MaterialTheme { MoversNavigationShell(NetworkStatus.Online, entries(probes)) }
        }
        compose.onNodeWithText("Open home details").performClick()
        compose.onNodeWithText("Home details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Josh").assertIsDisplayed()
        compose.onNodeWithText("Open profile details").performClick()
        compose.onNodeWithText("Profile details").assertIsDisplayed()
        compose.runOnIdle { assertNotSame(probes["Home"], probes["Profile"]) }
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNodeWithText("Home details").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(requireNotNull(probes["Home"]).cleared)
            assertFalse(requireNotNull(probes["Profile"]).cleared)
        }
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Josh").assertIsDisplayed()
        compose.waitUntil { probes["Profile"]?.cleared == true }
        compose.runOnIdle { assertFalse(requireNotNull(probes["Home"]).cleared) }
    }

    @Test fun savedStateRestoresTheSelectedTabAndItsDetails() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme { MoversNavigationShell(NetworkStatus.Online, entries(mutableMapOf())) }
        }
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Open profile details").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Profile details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNodeWithText("Open home details").assertIsDisplayed()
    }

    private fun entries(probes: MutableMap<String, ProbeViewModel>): Set<EntryProviderInstaller> = setOf({ navigator, tab ->
        entry<CatalogueKey> {
            Button(onClick = { navigator.navigate(MovieDetailsKey(1)) }) { Text("Open home details") }
        }
        entry<ProfileKey> {
            Column {
                Text("Josh")
                Button(onClick = { navigator.navigate(MovieDetailsKey(1)) }) { Text("Open profile details") }
            }
        }
        entry<WatchlistsKey> { Text("Coming soon") }
        entry<MovieDetailsKey> {
            val probe = viewModel { ProbeViewModel() }
            probes[tab] = probe
            Text("$tab details")
        }
    })

    private class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
