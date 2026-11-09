package com.jcotters.movers

import android.app.Application
import java.io.File

class MoversApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Retired account credentials and sessions are not imported into the device profile.
        listOf("auth_prefs.preferences_pb", "session_prefs.preferences_pb").forEach { name ->
            File(filesDir, "datastore/$name").delete()
        }
        initializeAndroidMovers(this, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    }
}
