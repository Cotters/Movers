package com.jcotters.search

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchDependencies = module { viewModelOf(::MovieSearchViewModel) }
