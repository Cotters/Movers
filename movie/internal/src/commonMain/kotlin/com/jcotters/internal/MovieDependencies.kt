package com.jcotters.internal

import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.time.AppClock
import com.jcotters.database.MoversDatabase
import com.jcotters.internal.catalogue.data.MovieCatalogueRepository
import com.jcotters.internal.catalogue.data.PopularMoviesRemoteMediator
import com.jcotters.internal.detail.data.BookmarksRepository
import com.jcotters.internal.detail.data.MovieDetailsRepository
import com.jcotters.internal.detail.data.MovieMapper
import org.koin.dsl.module

val movieDependencies = module {
    single<AppClock> { AppClock.System }
    single<MovieApi> { KtorMovieApi(get()) }
    single { MovieMapper() }
    single { PopularMoviesRemoteMediator(get(), get(), get(), get()) }
    single<IMovieCatalogueRepository> { MovieCatalogueRepository(get(), get<MoversDatabase>().movieDao(), get()) }
    single<IMovieDetailsRepository> { MovieDetailsRepository(get(), get<MoversDatabase>().movieDao(), get(), get()) }
    single<IBookmarksRepository> { BookmarksRepository(get<MoversDatabase>().bookmarkDao(), get()) }
}
