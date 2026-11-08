package com.jcotters.watchlists.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.watchlists.ui.WatchlistsRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object WatchlistsNavigationModule {
    @Provides
    @IntoSet
    fun provideEntries(): EntryProviderInstaller = { _, _ ->
        entry<WatchlistsKey> { WatchlistsRoute() }
    }
}
