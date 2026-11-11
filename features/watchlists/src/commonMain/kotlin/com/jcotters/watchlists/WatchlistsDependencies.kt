package com.jcotters.watchlists

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.onClose

val watchlistsDependencies = module {
    single { WatchlistRemovalCoordinator(get()) } onClose { it?.close() }
    viewModelOf(::WatchlistsViewModel)
    viewModelOf(::WatchlistDetailViewModel)
    viewModelOf(::AddToListsViewModel)
    viewModelOf(::SharingViewModel)
    viewModelOf(::FilmImportViewModel)
    viewModelOf(::ListTransferViewModel)
}
