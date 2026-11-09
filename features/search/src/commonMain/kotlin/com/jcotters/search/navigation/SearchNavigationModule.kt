package com.jcotters.search.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.search.MovieSearchRoute

object SearchNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<MovieSearchKey> { key -> MovieSearchRoute(key.listId, navigator, stackId) }
    }
}
