package com.jcotters.styleguide.navigation

import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.styleguide.ui.StyleGuideRoute

object StyleGuideNavigationModule {
    fun provideEntries(enabled: Boolean): EntryProviderInstaller = { navigator, _ ->
        // The app sanitizes restored development keys; retain a guard at the entry too.
        entry<StyleGuideKey> {
            if (enabled) StyleGuideRoute(navigator)
        }
    }
}
