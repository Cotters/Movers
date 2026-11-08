package com.jcotters.movers.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.jcotters.auth.navigation.AuthKey
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateShould {
    @Test
    fun `retain each tab history when switching`() {
        val state = state(1)
        val navigator = AppNavigator(state)
        val homeDetails = MovieDetailsKey(7)
        navigator.navigate(homeDetails)
        state.selectTab(TopLevelDestination.Profile)
        val profileDetails = MovieDetailsKey(9)
        navigator.navigate(profileDetails)

        state.selectTab(TopLevelDestination.Home)
        assertEquals(listOf(CatalogueKey, homeDetails), state.currentStack.toList())
        state.selectTab(TopLevelDestination.Profile)
        assertEquals(listOf(ProfileKey, profileDetails), state.currentStack.toList())
    }

    @Test
    fun `reselect pops only that tab to its root`() {
        val state = state(1)
        val navigator = AppNavigator(state)
        val homeDetails = MovieDetailsKey(7)
        navigator.navigate(homeDetails)
        state.selectTab(TopLevelDestination.Profile)
        navigator.navigate(MovieDetailsKey(9))

        state.selectTab(TopLevelDestination.Profile)

        assertEquals(listOf(ProfileKey), state.currentStack.toList())
        state.selectTab(TopLevelDestination.Home)
        assertEquals(homeDetails, state.currentStack.last())
    }

    @Test
    fun `back pops details then returns to the retained home stack`() {
        val state = state(1)
        val navigator = AppNavigator(state)
        val homeDetails = MovieDetailsKey(7)
        navigator.navigate(homeDetails)
        state.selectTab(TopLevelDestination.Profile)
        navigator.navigate(MovieDetailsKey(9))

        navigator.goBack()
        assertEquals(ProfileKey, state.currentStack.last())
        navigator.goBack()
        assertEquals(TopLevelDestination.Home, state.selectedTab)
        assertEquals(homeDetails, state.currentStack.last())
        navigator.goBack()
        navigator.goBack()
        assertEquals(listOf(CatalogueKey), state.currentStack.toList())
    }

    @Test
    fun `watchlists root returns home on back`() {
        val state = state()
        state.selectTab(TopLevelDestination.Watchlists)

        state.goBack()

        assertEquals(TopLevelDestination.Home, state.selectedTab)
    }

    @Test
    fun `login replaces auth in place without discarding the home feed entry`() {
        val state = state()
        val homeDetails = MovieDetailsKey(7)
        AppNavigator(state).navigate(homeDetails)
        state.selectTab(TopLevelDestination.Profile)

        state.reconcileSession(42)

        assertEquals(TopLevelDestination.Profile, state.selectedTab)
        assertEquals(listOf(ProfileKey), state.currentStack.toList())
        assertEquals(homeDetails, state.backStacks.getValue(TopLevelDestination.Home).last())
        assertEquals(0, state.generation)
        assertTrue(state.matchesSession(42))
    }

    @Test
    fun `login from an inactive profile does not redirect the current tab`() {
        val state = state()
        state.selectTab(TopLevelDestination.Watchlists)

        state.reconcileSession(42)

        assertEquals(TopLevelDestination.Watchlists, state.selectedTab)
        assertEquals(ProfileKey, state.backStacks.getValue(TopLevelDestination.Profile).first())
    }

    @Test
    fun `logout and expiry clear all retained entries and return home`() {
        val state = state(42)
        AppNavigator(state).navigate(MovieDetailsKey(7))
        state.selectTab(TopLevelDestination.Profile)
        AppNavigator(state).navigate(MovieDetailsKey(9))

        state.reconcileSession(null)

        assertEquals(TopLevelDestination.Home, state.selectedTab)
        assertEquals(listOf(CatalogueKey), state.currentStack.toList())
        assertEquals(listOf(AuthKey), state.backStacks.getValue(TopLevelDestination.Profile).toList())
        assertEquals(listOf(WatchlistsKey), state.backStacks.getValue(TopLevelDestination.Watchlists).toList())
        assertEquals(1, state.generation)
        assertTrue(state.matchesSession(null))
    }

    @Test
    fun `account replacement invalidates stores even when profile root key is unchanged`() {
        val state = state(42)
        state.selectTab(TopLevelDestination.Profile)

        state.reconcileSession(84)

        assertEquals(TopLevelDestination.Home, state.selectedTab)
        assertEquals(ProfileKey, state.backStacks.getValue(TopLevelDestination.Profile).single())
        assertEquals(1, state.generation)
        assertEquals(84, state.ownerUserId)
    }

    @Test
    fun `restored owner mismatch is rejected before private content can be shown`() {
        val state = state(42)
        state.selectTab(TopLevelDestination.Profile)
        AppNavigator(state).navigate(MovieDetailsKey(9))

        assertFalse(state.matchesSession(null))
        state.reconcileSession(null)
        assertTrue(state.matchesSession(null))
        assertEquals(TopLevelDestination.Home, state.selectedTab)
    }

    @Test
    fun `valid restored session keeps tab and screen history`() {
        val state = state(42)
        state.selectTab(TopLevelDestination.Profile)
        val details = MovieDetailsKey(9)
        AppNavigator(state).navigate(details)

        state.reconcileSession(42)

        assertEquals(TopLevelDestination.Profile, state.selectedTab)
        assertEquals(details, state.currentStack.last())
        assertEquals(0, state.generation)
    }

    @Test
    fun `repair a protected root restored for a guest`() {
        val state = state()
        state.selectTab(TopLevelDestination.Profile)
        state.currentStack.clear()
        state.currentStack.add(ProfileKey)

        assertFalse(state.matchesSession(null))
        state.reconcileSession(null)

        assertEquals(AuthKey, state.backStacks.getValue(TopLevelDestination.Profile).single())
        assertEquals(TopLevelDestination.Home, state.selectedTab)
    }

    @Test
    fun `same film on different stacks has independent entry identity`() {
        val state = state(42)
        val homeDetails = MovieDetailsKey(7)
        AppNavigator(state).navigate(homeDetails)
        state.selectTab(TopLevelDestination.Profile)
        val profileDetails = MovieDetailsKey(7)
        AppNavigator(state).navigate(profileDetails)

        assertNotEquals(homeDetails, profileDetails)
        assertEquals(homeDetails, state.backStacks.getValue(TopLevelDestination.Home).last())
        assertEquals(profileDetails, state.currentStack.last())
    }

    @Test
    fun `ignore rapid duplicate pushes for the visible film`() {
        val state = state()
        val navigator = AppNavigator(state)
        navigator.navigate(MovieDetailsKey(7))
        navigator.navigate(MovieDetailsKey(7))

        assertEquals(2, state.currentStack.size)
    }

    @Test
    fun `all keys serialize and details retain their instance identity`() {
        val keys: List<NavKey> = listOf(CatalogueKey, WatchlistsKey, ProfileKey, AuthKey, MovieDetailsKey(7))
        val serializer = ListSerializer(NavKeySerializer<NavKey>())

        val restored = Json.decodeFromString(serializer, Json.encodeToString(serializer, keys))

        assertEquals(keys, restored)
    }

    private fun state(userId: Int? = null): NavigationState = NavigationState(
        mutableStateOf(TopLevelDestination.Home.name),
        mutableStateOf(userId),
        mutableIntStateOf(0),
        mapOf(
            TopLevelDestination.Home to NavBackStack<NavKey>(CatalogueKey),
            TopLevelDestination.Watchlists to NavBackStack<NavKey>(WatchlistsKey),
            TopLevelDestination.Profile to NavBackStack<NavKey>(if (userId == null) AuthKey else ProfileKey),
        ),
    )
}
