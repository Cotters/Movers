package com.jcotters.watchlists.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.BottomSheetSceneStrategy
import com.jcotters.watchlists.ui.AddToListsRoute
import com.jcotters.watchlists.ui.ListTransferRoute
import com.jcotters.watchlists.ui.WatchlistDetailRoute
import com.jcotters.watchlists.ui.WatchlistsRoute

object WatchlistsNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<JoinWatchlistKey> { com.jcotters.watchlists.ui.JoinWatchlistRoute(navigator) }
        entry<SharedListKey> { key -> com.jcotters.watchlists.ui.SharedListRoute(key.listId, key.section, navigator) }
        entry<WatchlistsKey> { WatchlistsRoute(navigator) }
        entry<WatchlistDetailKey> { key ->
            WatchlistDetailRoute(key.listId, navigator, stackId, fromNotification = key.notificationEntryId != null)
        }
        entry<ListTransferKey> { key -> ListTransferRoute(key.listId, navigator) }
        entry<FilmImportKey> { key -> com.jcotters.watchlists.ui.FilmImportRoute(key.listId, navigator) }
        entry<AddToListsKey>(metadata = BottomSheetSceneStrategy.metadata) { key ->
            AddToListsRoute(key.movieId, key.initialListId, navigator)
        }
    }
}
