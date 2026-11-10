package com.jcotters.details

import com.jcotters.details.ui.MovieDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsDependencies = module {
    viewModelOf(::MovieDetailViewModel)
}
