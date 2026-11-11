package com.jcotters.styleguide

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val styleGuideDependencies = module { viewModelOf(::StyleGuideViewModel) }
