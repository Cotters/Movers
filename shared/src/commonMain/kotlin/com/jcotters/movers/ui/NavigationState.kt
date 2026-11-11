package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.jcotters.catalogue.navigation.CatalogueKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.details.navigation.MovieImageKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.navigation.Navigator
import com.jcotters.profile.navigation.ProfileKey
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.styleguide.navigation.StyleGuideKey
import com.jcotters.watchlists.navigation.AddToListsKey
import com.jcotters.watchlists.navigation.ListTransferKey
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import com.jcotters.watchlists.navigation.WatchlistsKey
import kotlinx.serialization.modules.polymorphic

internal enum class TopLevelDestination { Home, Watchlists, Profile }

internal const val NavigationTabMetadata = "movers.navigation.tab"

@Composable
internal fun rememberNavigationState(): NavigationState {
    val selected = rememberSaveable { mutableStateOf(TopLevelDestination.Home.name) }
    val home = rememberNavBackStack(navigationSavedState, CatalogueKey)
    val watchlists = rememberNavBackStack(navigationSavedState, WatchlistsKey)
    val profile = rememberNavBackStack(navigationSavedState, ProfileKey)
    return remember {
        NavigationState(
            selected,
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
    internal val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) {
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
            key(tab) {
                val provider: (NavKey) -> NavEntry<NavKey> = remember(installers, navigator, tab) {
                    val featureProvider = entryProvider<NavKey> {
                        installers.forEach { installer -> installer(navigator, tab.name) }
                    }
                    val taggedProvider: (NavKey) -> NavEntry<NavKey> = { key ->
                        val entry = featureProvider(key)
                        NavEntry(
                            key = key,
                            contentKey = entry.contentKey,
                            metadata = entry.metadata + (NavigationTabMetadata to tab),
                        ) { entry.Content() }
                    }
                    taggedProvider
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

}

internal class AppNavigator(private val state: NavigationState) : Navigator {
    override fun navigate(key: NavKey) {
        val current = state.currentStack.lastOrNull()
        if (current is MovieDetailsKey && key is MovieDetailsKey && current.movieId == key.movieId) return
        if (current is AddToListsKey && key is AddToListsKey && current.movieId == key.movieId) return
        if (current is MovieSearchKey && key is MovieSearchKey && current.listId == key.listId) return
        if (current == key) return
        state.currentStack.add(key)
    }

    override fun goBack() = state.goBack()
}

internal val navigationSavedState = androidx.savedstate.serialization.SavedStateConfiguration {
    serializersModule = kotlinx.serialization.modules.SerializersModule {
        polymorphic(NavKey::class) {
            subclass(CatalogueKey::class, CatalogueKey.serializer())
            subclass(WatchlistsKey::class, WatchlistsKey.serializer())
            subclass(com.jcotters.watchlists.navigation.JoinWatchlistKey::class, com.jcotters.watchlists.navigation.JoinWatchlistKey.serializer())
            subclass(com.jcotters.watchlists.navigation.SharedListKey::class, com.jcotters.watchlists.navigation.SharedListKey.serializer())
            subclass(ProfileKey::class, ProfileKey.serializer())
            subclass(StyleGuideKey::class, StyleGuideKey.serializer())
            subclass(MovieDetailsKey::class, MovieDetailsKey.serializer())
            subclass(MovieImageKey::class, MovieImageKey.serializer())
            subclass(WatchlistDetailKey::class, WatchlistDetailKey.serializer())
            subclass(com.jcotters.watchlists.navigation.FilmImportKey::class, com.jcotters.watchlists.navigation.FilmImportKey.serializer())
            subclass(ListTransferKey::class, ListTransferKey.serializer())
            subclass(AddToListsKey::class, AddToListsKey.serializer())
            subclass(MovieSearchKey::class, MovieSearchKey.serializer())
        }
    }
}
