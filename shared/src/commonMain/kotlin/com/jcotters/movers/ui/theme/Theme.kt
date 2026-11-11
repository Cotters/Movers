package com.jcotters.movers.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.jcotters.designsystem.MoversDesignTheme
import com.jcotters.designsystem.MoversStyleSettings
import com.jcotters.designsystem.ThemeMode

/** One shared visual identity on Android and iOS. Gallery overrides stay local. */
@Composable
fun MoversTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MoversDesignTheme(
        settings = MoversStyleSettings(theme = if (darkTheme) ThemeMode.Dark else ThemeMode.Light),
        content = content,
    )
}
