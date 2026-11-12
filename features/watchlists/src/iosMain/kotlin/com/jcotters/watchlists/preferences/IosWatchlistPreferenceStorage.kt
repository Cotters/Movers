@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jcotters.watchlists.preferences

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

class IosWatchlistPreferenceStorage : WatchlistPreferenceStorage {
    private val key = "movers.watchlist-view.state"

    override suspend fun read(): String? = withContext(Dispatchers.Default) { NSUserDefaults.standardUserDefaults.stringForKey(key) }

    override suspend fun write(value: String): Unit = withContext(Dispatchers.Default) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }
}
