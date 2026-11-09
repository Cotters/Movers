package com.jcotters.movers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jcotters.catalogue.navigation.CatalogueNavigationModule
import com.jcotters.details.navigation.MovieDetailsNavigationModule
import com.jcotters.movers.ui.MoversApp
import com.jcotters.movers.ui.theme.MoversTheme
import com.jcotters.profile.navigation.ProfileNavigationModule
import com.jcotters.watchlists.navigation.WatchlistsNavigationModule
import com.jcotters.search.navigation.SearchNavigationModule

@Composable
fun SharedApp() {
    val entries = remember {
        setOf(
            CatalogueNavigationModule.provideEntries(), MovieDetailsNavigationModule.provideEntries(),
            ProfileNavigationModule.provideEntries(), WatchlistsNavigationModule.provideEntries(),
            SearchNavigationModule.provideEntries(),
        )
    }
    MoversTheme { MoversApp(entries) }
}
