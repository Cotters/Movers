package com.jcotters.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Share one pulse across the shapes in a loading surface. Read its value only during drawing. */
@Composable
fun rememberMoversLoadingOpacity(): State<Float> {
    val transition = rememberInfiniteTransition(label = "Loading")
    return transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "Placeholder opacity",
    )
}

@Composable
fun MoversLoadingPlaceholder(opacity: State<Float>, modifier: Modifier = Modifier) {
    Spacer(
        modifier.clip(RoundedCornerShape(6.dp))
            .graphicsLayer { alpha = opacity.value }
            .background(MoversDesign.tokens.colors.content.copy(alpha = 0.2f)),
    )
}
