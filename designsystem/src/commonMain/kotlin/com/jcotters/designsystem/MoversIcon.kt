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

enum class MoversSymbol { Add, Check, Close, Search, Trash, More, Back, Grid }

/** Small original stroke icons; no icon-font or external asset dependency. */
@Composable
fun MoversIcon(symbol: MoversSymbol, modifier: Modifier = Modifier, tint: Color = MoversDesign.tokens.colors.content) {
    Canvas(modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        val stroke = 1.8f * scale
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale), stroke, StrokeCap.Round)
        }
        when (symbol) {
            MoversSymbol.Add -> { line(5f, 12f, 19f, 12f); line(12f, 5f, 12f, 19f) }
            MoversSymbol.Check -> { line(5f, 12f, 10f, 17f); line(10f, 17f, 19f, 7f) }
            MoversSymbol.Close -> { line(6f, 6f, 18f, 18f); line(6f, 18f, 18f, 6f) }
            MoversSymbol.Back -> { line(18f, 12f, 5f, 12f); line(5f, 12f, 11f, 6f); line(5f, 12f, 11f, 18f) }
            MoversSymbol.More -> listOf(5f, 12f, 19f).forEach { drawCircle(tint, 1.6f * scale, Offset(it * scale, 12f * scale)) }
            MoversSymbol.Search -> {
                drawCircle(tint, 6f * scale, Offset(10f * scale, 10f * scale), style = Stroke(stroke))
                line(15f, 15f, 21f, 21f)
            }
            MoversSymbol.Trash -> {
                line(4f, 6f, 20f, 6f); line(9f, 3f, 15f, 3f)
                line(6f, 6f, 7f, 21f); line(7f, 21f, 17f, 21f); line(17f, 21f, 18f, 6f)
                line(10f, 10f, 10f, 17f); line(14f, 10f, 14f, 17f)
            }
            MoversSymbol.Grid -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                drawRect(tint, Offset(x * scale, y * scale), Size(6f * scale, 6f * scale), style = Stroke(stroke))
            }
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
            .border(1.dp, if (focused) tokens.colors.accent else Color.Transparent, shape)
            .semantics { contentDescription = label },
    ) {
        MoversIcon(symbol, tint = when {
            !enabled -> tokens.colors.muted
            destructive -> tokens.colors.danger
            else -> tokens.colors.content
        })
    }
}
