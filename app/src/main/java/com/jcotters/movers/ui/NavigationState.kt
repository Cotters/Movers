package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.jcotters.auth.navigation.AuthKey
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.navigation.Navigator
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.watchlists.navigation.WatchlistsKey

internal enum class TopLevelDestination { Home, Watchlists, Profile }

@Composable
internal fun rememberNavigationState(userId: Int?): NavigationState {
    val selected = rememberSaveable { mutableStateOf(TopLevelDestination.Home.name) }
    val owner = rememberSaveable { mutableStateOf(userId) }
    val generation = rememberSaveable { mutableIntStateOf(0) }
    val home = rememberNavBackStack(CatalogueKey)
    val watchlists = rememberNavBackStack(WatchlistsKey)
    val profile = rememberNavBackStack(if (userId == null) AuthKey else ProfileKey)
    return remember {
        NavigationState(
            selected, owner, generation,
            mapOf(
                TopLevelDestination.Home to home,
                TopLevelDestination.Watchlists to watchlists,
                TopLevelDestination.Profile to profile,
            ),
        )
    }
}

internal class NavigationState(
    private val selected: MutableState<String>,
    owner: MutableState<Int?>,
    generation: MutableIntState,
    internal val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) {
    var ownerUserId: Int? by owner
        private set
    var generation: Int by generation
        private set
    val selectedTab: TopLevelDestination get() = TopLevelDestination.valueOf(selected.value)
    val currentStack: NavBackStack<NavKey> get() = backStacks.getValue(selectedTab)

    fun selectTab(tab: TopLevelDestination) {
        if (tab == selectedTab) {
            val stack = backStacks.getValue(tab)
            while (stack.size > 1) stack.removeLastOrNull()
        } else {
            selected.value = tab.name
        }
    }

    fun matchesSession(userId: Int?): Boolean = ownerUserId == userId &&
        backStacks.getValue(TopLevelDestination.Profile).firstOrNull() ==
        rootFor(TopLevelDestination.Profile, userId)

    fun reconcileSession(userId: Int?) {
        if (ownerUserId != userId && ownerUserId != null) {
            // A new generation drops every entry store, even when its root key is unchanged.
            generation++
            backStacks.forEach { (tab, stack) ->
                stack.clear()
                stack.add(rootFor(tab, userId))
            }
            selected.value = TopLevelDestination.Home.name
        } else {
            val profile = backStacks.getValue(TopLevelDestination.Profile)
            val root = rootFor(TopLevelDestination.Profile, userId)
            if (profile.firstOrNull() != root) {
                profile.clear()
                profile.add(root)
                if (userId == null) selected.value = TopLevelDestination.Home.name
            }
        }
        ownerUserId = userId
    }

    fun goBack() {
        if (currentStack.size > 1) {
            currentStack.removeLastOrNull()
        } else if (selectedTab != TopLevelDestination.Home) {
            selected.value = TopLevelDestination.Home.name
        }
    }

    @Composable
    fun decoratedEntries(
        installers: Set<EntryProviderInstaller>,
        navigator: Navigator,
    ): List<NavEntry<NavKey>> {
        val entriesByTab = TopLevelDestination.entries.associateWith { tab ->
            key(tab, generation) {
                val provider = remember(installers, navigator, tab) {
                    entryProvider<NavKey> {
                        installers.forEach { installer -> installer(navigator, tab.name) }
                    }
                }
                rememberDecoratedNavEntries(
                    backStack = backStacks.getValue(tab),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = provider,
                )
            }
        }
        val activeTabs = if (selectedTab == TopLevelDestination.Home) {
            listOf(TopLevelDestination.Home)
        } else {
            listOf(TopLevelDestination.Home, selectedTab)
        }
        return activeTabs.flatMap { entriesByTab.getValue(it) }
    }

    private fun rootFor(tab: TopLevelDestination, userId: Int?): NavKey = when (tab) {
        TopLevelDestination.Home -> CatalogueKey
        TopLevelDestination.Watchlists -> WatchlistsKey
        TopLevelDestination.Profile -> if (userId == null) AuthKey else ProfileKey
    }
}

internal class AppNavigator(private val state: NavigationState) : Navigator {
    override fun navigate(key: NavKey) {
        val current = state.currentStack.lastOrNull()
        if (current is MovieDetailsKey && key is MovieDetailsKey && current.movieId == key.movieId) return
        if (current == key) return
        state.currentStack.add(key)
    }

    override fun goBack() = state.goBack()
}
