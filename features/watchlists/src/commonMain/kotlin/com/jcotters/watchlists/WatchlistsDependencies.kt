package com.jcotters.watchlists

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val watchlistsDependencies = module {
    viewModelOf(::WatchlistsViewModel)
    viewModelOf(::WatchlistDetailViewModel)
    viewModelOf(::AddToListsViewModel)
}
