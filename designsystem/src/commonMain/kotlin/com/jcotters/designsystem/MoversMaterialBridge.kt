package com.jcotters.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

/** Maps retained layout/text infrastructure to Movers tokens. Controls own their styling. */
@Composable
internal fun MoversMaterialBridge(tokens: MoversDesignTokens, dark: Boolean, content: @Composable () -> Unit) {
    val c = tokens.colors
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.raised,
        onPrimaryContainer = c.accent,
        inversePrimary = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.raised,
        onSecondaryContainer = c.content,
        tertiary = c.success,
        onTertiary = c.background,
        tertiaryContainer = c.successSurface,
        onTertiaryContainer = c.success,
        background = c.background,
        onBackground = c.content,
        surface = c.surface,
        onSurface = c.content,
        surfaceVariant = c.raised,
        onSurfaceVariant = c.muted,
        surfaceTint = c.accent,
        surfaceBright = c.raised,
        surfaceDim = c.background,
        surfaceContainer = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainerLowest = c.background,
        surfaceContainerHigh = c.raised,
        surfaceContainerHighest = c.raised,
        inverseSurface = c.raised,
        inverseOnSurface = c.content,
        error = c.danger,
        onError = c.background,
        errorContainer = c.dangerSurface,
        onErrorContainer = c.danger,
        outline = c.controlOutline,
        outlineVariant = c.border,
        scrim = c.scrim,
    )
    val type = tokens.type
    val typography = Typography(
        displayLarge = type.headline,
        displayMedium = type.headline,
        displaySmall = type.headline,
        headlineLarge = type.headline,
        headlineMedium = type.headline,
        headlineSmall = type.title,
        titleLarge = type.title,
        titleMedium = type.label.copy(fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = type.label,
        bodyLarge = type.body,
        bodyMedium = type.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = type.caption,
        labelLarge = type.label,
        labelMedium = type.caption,
        labelSmall = type.caption,
    )
    val shape = RoundedCornerShape(tokens.cornerRadius)
    MaterialTheme(
        colorScheme = colors, typography = typography,
        shapes = Shapes(shape, shape, shape, shape, shape), content = content
    )
}
