@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberFeatureTourPreference(): FeatureTourPreference = remember {
    object : FeatureTourPreference {
        override val seen: Boolean get() = NSUserDefaults.standardUserDefaults.boolForKey("movers.feature-tour.seen")
        override fun finish() { NSUserDefaults.standardUserDefaults.setBool(true, "movers.feature-tour.seen") }
    }
}
