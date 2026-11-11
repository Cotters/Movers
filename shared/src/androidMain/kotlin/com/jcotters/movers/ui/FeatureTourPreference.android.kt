package com.jcotters.movers.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberFeatureTourPreference(): FeatureTourPreference {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("movers.feature-tour", Context.MODE_PRIVATE)
        object : FeatureTourPreference {
            override val seen: Boolean get() = preferences.getBoolean("seen", false)
            override fun finish() { preferences.edit().putBoolean("seen", true).apply() }
        }
    }
}
