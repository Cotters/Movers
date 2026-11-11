package com.jcotters.styleguide.navigation

import androidx.compose.runtime.LaunchedEffect
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.styleguide.ui.StyleGuideRoute

object StyleGuideNavigationModule {
    fun provideEntries(enabled: Boolean): EntryProviderInstaller = { navigator, _ ->
        // Keep the key readable after a Debug → Release update, without exposing the gallery.
        entry<StyleGuideKey> {
            if (enabled) StyleGuideRoute(navigator)
            else LaunchedEffect(Unit) { navigator.goBack() }
        }
    }
}
