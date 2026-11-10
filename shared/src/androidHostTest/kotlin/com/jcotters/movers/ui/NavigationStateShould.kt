package com.jcotters.movers.ui

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.details.navigation.MovieImageKey
import com.jcotters.details.navigation.MovieImageType
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.watchlists.navigation.AddToListsKey
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStateShould {
    @Test fun `retain independent histories across tab changes`() {
        val state = state()
        state.currentStack.add(MovieDetailsKey(1, "home"))
        state.selectTab(TopLevelDestination.Profile)
        state.currentStack.add(MovieDetailsKey(2, "profile"))
        state.selectTab(TopLevelDestination.Home)
        assertEquals(MovieDetailsKey(1, "home"), state.currentStack.last())
        state.selectTab(TopLevelDestination.Profile)
        assertEquals(MovieDetailsKey(2, "profile"), state.currentStack.last())
    }
    @Test fun `reselection pops only the active history to its root`() {
        val state = state()
        state.currentStack.add(MovieDetailsKey(1, "detail"))
        state.selectTab(TopLevelDestination.Home)
        assertEquals(listOf(CatalogueKey), state.currentStack.toList())
    }
    @Test fun `back at profile root returns to retained home history`() {
        val state = state()
        state.currentStack.add(MovieDetailsKey(1, "detail"))
        state.selectTab(TopLevelDestination.Profile)
        state.goBack()
        assertEquals(TopLevelDestination.Home, state.selectedTab)
        assertEquals(MovieDetailsKey(1, "detail"), state.currentStack.last())
    }
    @Test fun `duplicate detail taps do not create duplicate entries`() {
        val state = state()
        val navigator = AppNavigator(state)
        navigator.navigate(MovieDetailsKey(1, "one"))
        navigator.navigate(MovieDetailsKey(1, "two"))
        assertEquals(2, state.currentStack.size)
    }
    @Test fun `every key round trips without platform reflection`() {
        val json = Json { serializersModule = navigationSavedState.serializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        listOf(CatalogueKey, ProfileKey, WatchlistsKey, MovieDetailsKey(14, "entry", "list"),
            MovieImageKey(14, MovieImageType.Poster, "entry"), MovieImageKey(14, MovieImageType.Backdrop, "entry"),
            WatchlistDetailKey("list"), AddToListsKey(14, "list", "picker"), MovieSearchKey("list", "search")).forEach { key ->
            assertEquals(key, json.decodeFromString(serializer, json.encodeToString(serializer, key)))
        }
    }
    private fun state() = NavigationState(mutableStateOf(TopLevelDestination.Home.name), mapOf(
        TopLevelDestination.Home to NavBackStack<NavKey>(CatalogueKey),
        TopLevelDestination.Profile to NavBackStack<NavKey>(ProfileKey),
        TopLevelDestination.Watchlists to NavBackStack<NavKey>(WatchlistsKey),
    ))
}
