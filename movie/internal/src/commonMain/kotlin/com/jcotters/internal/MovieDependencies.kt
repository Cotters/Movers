package com.jcotters.internal

import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.contract.time.AppClock
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.database.MoversDatabase
import com.jcotters.internal.catalogue.data.MovieCatalogueRepository
import com.jcotters.internal.catalogue.data.PopularMoviesRemoteMediator
import com.jcotters.internal.detail.data.MovieDetailsRepository
import com.jcotters.internal.detail.data.MovieMapper
import com.jcotters.internal.search.MovieSearchRepository
import com.jcotters.internal.watchlists.RoomWatchlistDatabase
import com.jcotters.internal.watchlists.WatchlistsRepository
import org.koin.dsl.module

val movieDependencies = module {
    single<AppClock> { AppClock.System }
    single<MovieApi> { KtorMovieApi(get()) }
    single { MovieMapper() }
    single { RoomWatchlistDatabase(get(), get()) }
    single { com.jcotters.internal.watchlists.SharedWatchlistsRepository(get(), getOrNull<com.jcotters.contract.watchlists.data.SharedWatchlistRemote>() ?: com.jcotters.internal.watchlists.UnavailableSharing,
        getOrNull<com.jcotters.profile.domain.ParticipantIdentity>() ?: com.jcotters.internal.watchlists.UnavailableSharing, get(), get()) }
    single<com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository> { get<com.jcotters.internal.watchlists.SharedWatchlistsRepository>() }
    single<WatchlistDatabase> { com.jcotters.internal.watchlists.CollaborativeWatchlistDatabase(get(), get(), get()) }
    single<IWatchlistsRepository> { WatchlistsRepository(get(), get()) }
    single<IMovieSearchRepository> { MovieSearchRepository(get(), get<MoversDatabase>().movieDao(), get()) }
    single { PopularMoviesRemoteMediator(get(), get(), get(), get()) }
    single<IMovieCatalogueRepository> { MovieCatalogueRepository(get(), get<MoversDatabase>().movieDao(), get()) }
    single<IMovieDetailsRepository> { MovieDetailsRepository(get(), get<MoversDatabase>().movieDao(), get(), get()) }
}
