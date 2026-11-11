package com.jcotters.movers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jcotters.catalogue.navigation.CatalogueNavigationModule
import com.jcotters.details.navigation.MovieDetailsNavigationModule
import com.jcotters.movers.ui.MoversApp
import com.jcotters.movers.ui.theme.MoversTheme
import com.jcotters.profile.navigation.ProfileNavigationModule
import com.jcotters.navigation.Navigator
import com.jcotters.styleguide.navigation.StyleGuideKey
import com.jcotters.styleguide.navigation.StyleGuideNavigationModule
import com.jcotters.watchlists.navigation.WatchlistsNavigationModule
import com.jcotters.search.navigation.SearchNavigationModule

@Composable
fun SharedApp() {
    val developmentTools = developmentToolsEnabled()
    val openStyleGuide: ((Navigator) -> Unit)? = if (developmentTools) {
        { navigator -> navigator.navigate(StyleGuideKey) }
    } else null
    val entries = remember(developmentTools) {
        setOf(
            CatalogueNavigationModule.provideEntries(), MovieDetailsNavigationModule.provideEntries(),
            ProfileNavigationModule.provideEntries(onOpenStyleGuide = openStyleGuide), WatchlistsNavigationModule.provideEntries(),
            SearchNavigationModule.provideEntries(),
            StyleGuideNavigationModule.provideEntries(enabled = developmentTools),
        )
    }
    MoversTheme { MoversApp(entries) }
}
