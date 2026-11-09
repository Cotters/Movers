package com.jcotters.movers.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jcotters.movers.SharedApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val restorableState = savedInstanceState?.takeIf { it.getInt(STATE_SCHEMA_KEY) == STATE_SCHEMA_VERSION }
        super.onCreate(restorableState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        if (savedInstanceState == null) intent?.dataString?.let({ com.jcotters.movers.receiveInvitation(it) })
        setContent { SharedApp() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { com.jcotters.movers.receiveInvitation(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SCHEMA_KEY, STATE_SCHEMA_VERSION)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_SCHEMA_KEY = "movers.navigation.schema"
        const val STATE_SCHEMA_VERSION = 1
    }
}
