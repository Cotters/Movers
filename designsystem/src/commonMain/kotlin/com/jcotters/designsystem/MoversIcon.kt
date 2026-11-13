package com.jcotters.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton

/** Small original stroke icons; no icon-font or external asset dependency. */
@Composable
fun MoversIcon(symbol: MoversSymbol, modifier: Modifier = Modifier, tint: Color = MoversDesign.tokens.colors.content) {
    Canvas(modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        val stroke = 1.8f * scale
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale), stroke, StrokeCap.Round)
        }
        IconStroke.lines[symbol]?.forEach { line(it.x1, it.y1, it.x2, it.y2) }
        when (symbol) {
            MoversSymbol.More -> listOf(5f, 12f, 19f).forEach {
                drawCircle(tint, 1.6f * scale, Offset(it * scale, 12f * scale))
            }

            MoversSymbol.Search -> {
                drawCircle(tint, 6f * scale, Offset(10f * scale, 10f * scale), style = Stroke(stroke))
                line(15f, 15f, 21f, 21f)
            }

            MoversSymbol.Grid -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                drawRect(tint, Offset(x * scale, y * scale), Size(6f * scale, 6f * scale), style = Stroke(stroke))
            }

            else -> Unit
        }
    }
}

@Composable
fun MoversIconButton(
    symbol: MoversSymbol,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    MoversIconButton(
        onClick = onClick, modifier = modifier.semantics { contentDescription = label },
        enabled = enabled, contentColor = if (destructive) MoversDesign.tokens.colors.danger else Color.Unspecified
    ) {
        MoversIcon(symbol, tint = androidx.compose.material3.LocalContentColor.current)
    }
}

@Composable
fun MoversIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    UnstyledButton(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        contentPadding = PaddingValues(12.dp),
        modifier = modifier.size(48.dp).clip(shape)
            .background(if (pressed) tokens.colors.raised else Color.Transparent)
            .border(1.dp, if (focused) tokens.colors.accent else Color.Transparent, shape),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides when {
                !enabled -> tokens.colors.muted
                contentColor != Color.Unspecified -> contentColor
                else -> tokens.colors.content
            },
            content = content,
        )
    }
}
