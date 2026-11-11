package com.jcotters.movers

import com.jcotters.catalogue.catalogueDependencies
import com.jcotters.details.detailsDependencies
import com.jcotters.internal.movieDependencies
import com.jcotters.movers.ui.AppViewModel
import com.jcotters.styleguide.styleGuideDependencies
import com.jcotters.profile.profileDependencies
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import com.jcotters.watchlists.watchlistsDependencies
import com.jcotters.search.searchDependencies
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val sharedDependencies = module {
    viewModelOf(::AppViewModel)
}

internal fun initializeMovers(platformDependencies: Module) {
    startKoin {
        modules(platformDependencies, movieDependencies, catalogueDependencies, detailsDependencies, profileDependencies, watchlistsDependencies, searchDependencies, styleGuideDependencies, sharedDependencies)
    }
}
