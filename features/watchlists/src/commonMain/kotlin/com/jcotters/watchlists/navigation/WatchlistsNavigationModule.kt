package com.jcotters.watchlists.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.watchlists.ui.WatchlistsRoute

object WatchlistsNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { _, _ ->
        entry<WatchlistsKey> { WatchlistsRoute() }
    }
}
