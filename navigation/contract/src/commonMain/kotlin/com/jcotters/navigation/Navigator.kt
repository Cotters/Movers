package com.jcotters.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun navigate(key: NavKey)
    fun goBack()
}

/** Installs feature destinations; stackId isolates shared-element identity between tabs. */
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.(navigator: Navigator, stackId: String) -> Unit
