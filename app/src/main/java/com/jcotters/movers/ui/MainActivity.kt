package com.jcotters.movers.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jcotters.movers.ui.theme.MoversTheme
import com.jcotters.navigation.EntryProviderInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var entryInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    setContent {
      MoversTheme {
        MoversApp(entryInstallers)
      }
    }
  }
}
