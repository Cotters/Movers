package com.jcotters.details

import com.jcotters.details.domain.BookmarkMovieUseCase
import com.jcotters.details.domain.ObserveMovieBookmarkUseCase
import com.jcotters.details.ui.MovieDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsDependencies = module {
    factory { BookmarkMovieUseCase(get()) }
    factory { ObserveMovieBookmarkUseCase(get()) }
    viewModelOf(::MovieDetailViewModel)
}
