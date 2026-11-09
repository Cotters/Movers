package com.jcotters.watchlists.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.BottomSheetSceneStrategy
import com.jcotters.watchlists.ui.AddToListsRoute
import com.jcotters.watchlists.ui.WatchlistDetailRoute
import com.jcotters.watchlists.ui.WatchlistsRoute

object WatchlistsNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<WatchlistsKey> { WatchlistsRoute(navigator) }
        entry<WatchlistDetailKey> { key -> WatchlistDetailRoute(key.listId, navigator, stackId) }
        entry<AddToListsKey>(metadata = BottomSheetSceneStrategy.metadata) { key ->
            AddToListsRoute(key.movieId, key.initialListId, navigator)
        }
    }
}
