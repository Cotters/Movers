package com.jcotters.catalogue

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogueDependencies = module {
    viewModelOf(::MovieCatalogueViewModel)
}
