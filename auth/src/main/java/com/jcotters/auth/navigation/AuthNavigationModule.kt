package com.jcotters.auth.navigation

import com.jcotters.auth.ui.AuthRoute
import com.jcotters.navigation.EntryProviderInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AuthNavigationModule {
    @Provides
    @IntoSet
    fun provideEntries(): EntryProviderInstaller = { _, _ ->
        entry<AuthKey> { AuthRoute() }
    }
}
