package com.jcotters.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcotters.designsystem.resources.Res
import com.jcotters.designsystem.resources.inter_medium
import com.jcotters.designsystem.resources.inter_regular
import com.jcotters.designsystem.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class MoversColors(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val border: Color,
    val controlOutline: Color,
    val danger: Color,
    val dangerSurface: Color,
    val success: Color,
    val successSurface: Color,
    val scrim: Color = Color.Black.copy(alpha = 0.55f),
)

@Immutable
data class MoversTypography(
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

@Immutable
data class MoversDesignTokens(
    val colors: MoversColors,
    val type: MoversTypography,
    val cornerRadius: Dp,
    val spacing: Dp,
    val controlPadding: Dp,
)

private val LocalMoversDesign = staticCompositionLocalOf<MoversDesignTokens> {
    error("Movers components require MoversDesignTheme.")
}

object MoversDesign {
    val tokens: MoversDesignTokens
        @Composable @ReadOnlyComposable get() = LocalMoversDesign.current
}

@Composable
fun MoversDesignTheme(settings: MoversStyleSettings = MoversStyleSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val family = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )
    val tokens = MoversDesignTokens(
        colors = if (dark) darkColors else lightColors,
        type = MoversTypography(
            headline = TextStyle(fontFamily = family, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
            title = TextStyle(fontFamily = family, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
            body = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 23.sp),
            label = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            caption = TextStyle(fontFamily = family, fontSize = 13.sp, lineHeight = 19.sp),
        ),
        cornerRadius = settings.cornerRadius,
        spacing = if (settings.density == ComponentDensity.Comfortable) 8.dp else 6.dp,
        controlPadding = if (settings.density == ComponentDensity.Comfortable) 16.dp else 12.dp,
    )
    CompositionLocalProvider(LocalMoversDesign provides tokens) {
        MoversMaterialBridge(tokens, dark, content)
    }
}

private val darkColors = MoversColors(
    background = Color(0xFF101619), surface = Color(0xFF192225), raised = Color(0xFF243034),
    content = Color(0xFFEAF1EF), muted = Color(0xFFABBDB7), accent = Color(0xFF93E5D8),
    onAccent = Color(0xFF10251F), border = Color(0xFF3C4D48), controlOutline = Color(0xFF647D73),
    danger = Color(0xFFF1A9AE), dangerSurface = Color(0xFF42282C),
    success = Color(0xFFADD8B8), successSurface = Color(0xFF24372A),
)

private val lightColors = MoversColors(
    background = Color(0xFFF4F7F6), surface = Color.White, raised = Color(0xFFE7EEEB),
    content = Color(0xFF172421), muted = Color(0xFF52645E), accent = Color(0xFF146C63),
    onAccent = Color.White, border = Color(0xFFA6B9B1), controlOutline = Color(0xFF788F86),
    danger = Color(0xFF9D303B), dangerSurface = Color(0xFFFCE9EA),
    success = Color(0xFF28653C), successSurface = Color(0xFFE9F4E9),
)
