package com.jcotters.details

import com.jcotters.details.ui.MovieDetailViewModel
import com.jcotters.details.ui.MovieImageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsDependencies = module {
    viewModelOf(::MovieDetailViewModel)
    viewModel { parameters ->
        MovieImageViewModel(
            movieId = parameters.get(),
            imageType = parameters.get(),
            movieDetailsRepository = get(),
        )
    }
}
