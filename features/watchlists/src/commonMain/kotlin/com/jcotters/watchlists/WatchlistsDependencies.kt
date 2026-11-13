package com.jcotters.watchlists

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.onClose

val watchlistsDependencies = module {
    single<com.jcotters.watchlists.preferences.WatchlistPreferences> {
        com.jcotters.watchlists.preferences.LocalWatchlistPreferences(get())
    }
    single { WatchlistRemovalCoordinator(get()) } onClose { it?.close() }
    viewModelOf(::WatchlistsViewModel)
    viewModelOf(::WatchlistDetailViewModel)
    viewModelOf(::AddToListsViewModel)
    viewModelOf(::SharingViewModel)
    viewModelOf(::JoinWatchlistViewModel)
    viewModelOf(::FilmImportViewModel)
    viewModelOf(::ListTransferViewModel)
}
