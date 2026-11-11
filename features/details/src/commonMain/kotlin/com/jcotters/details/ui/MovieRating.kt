package com.jcotters.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

@Composable
internal fun MovieRating(score: Double, voteCount: Int, modifier: Modifier = Modifier) {
    val value = round(score * 10) / 10
    val votes = if (voteCount == 1) "1 vote" else "$voteCount votes"
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "TMDB rating: $value out of 10, $votes"
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            RatingStars(score = score)
            Text("$value/10", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "TMDB · $votes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RatingStars(score: Double) {
    val filledColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    Spacer(
        Modifier.size(width = 78.dp, height = 14.dp).drawWithCache {
            val diameter = size.height
            val radius = diameter / 2
            val path = Path().apply {
                repeat(10) { point ->
                    val angle = -PI / 2 + point * PI / 5
                    val pointRadius = if (point % 2 == 0) radius else radius * 0.45f
                    val x = radius + (cos(angle) * pointRadius).toFloat()
                    val y = radius + (sin(angle) * pointRadius).toFloat()
                    if (point == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            val step = (size.width - diameter) / 4
            val rtl = layoutDirection == LayoutDirection.Rtl
            onDrawBehind {
                repeat(5) { index ->
                    // TMDB scores are out of ten; each star represents two points.
                    val fraction = (score / 2 - index).coerceIn(0.0, 1.0).toFloat()
                    val position = if (rtl) 4 - index else index
                    translate(left = position * step) {
                        drawPath(path, emptyColor)
                        if (fraction > 0f) {
                            clipRect(
                                left = if (rtl) diameter * (1 - fraction) else 0f,
                                right = if (rtl) diameter else diameter * fraction,
                            ) {
                                drawPath(path, filledColor)
                            }
                        }
                    }
                }
            }
        },
    )
}
