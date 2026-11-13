package com.jcotters.watchlists.preferences

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidWatchlistPreferenceStorage(context: Context) : WatchlistPreferenceStorage {
    private val applicationContext = context.applicationContext
    private val preferences by lazy {
        applicationContext.getSharedPreferences(
            "movers.watchlist-view",
            Context.MODE_PRIVATE
        )
    }

    override suspend fun read(): String? = withContext(Dispatchers.IO) { preferences.getString("state", null) }

    override suspend fun write(value: String): Unit = withContext(Dispatchers.IO) {
        check(preferences.edit().putString("state", value).commit()) { "Couldn't save watchlist preferences." }
    }
}
