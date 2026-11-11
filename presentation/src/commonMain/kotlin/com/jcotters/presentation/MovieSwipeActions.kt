package com.jcotters.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add_to_list
import com.jcotters.presentation.resources.trash
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class MovieReveal { Closed, Add, Remove }

/** Reveals explicit actions; a swipe never performs a destructive operation. */
@Composable
fun MovieSwipeActions(
    movieTitle: String,
    onOpen: () -> Unit,
    onAddToList: (() -> Unit)?,
    onRemove: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, () -> Unit) -> Unit,
) {
    if (onAddToList == null && onRemove == null) {
        content(modifier, onOpen)
        return
    }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val actionWidth = 88.dp
    val widthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val hasAdd = onAddToList != null
    val hasRemove = onRemove != null
    val state = remember(widthPx, direction, hasAdd, hasRemove) {
        AnchoredDraggableState(MovieReveal.Closed, DraggableAnchors {
            MovieReveal.Closed at 0f
            if (hasAdd) MovieReveal.Add at widthPx * direction
            if (hasRemove) MovieReveal.Remove at -widthPx * direction
        })
    }
    val scope = rememberCoroutineScope()
    fun perform(action: () -> Unit) {
        scope.launch {
            state.animateTo(MovieReveal.Closed)
            action()
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) state.animateTo(MovieReveal.Closed)
    }
    Box(modifier.fillMaxWidth().clipToBounds()) {
        // These buttons are represented by custom actions on the foreground's semantics.
        if (onAddToList != null) {
            SwipeAction(
                icon = Res.drawable.add_to_list,
                label = "Add to list",
                background = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { perform(onAddToList) },
                enabled = enabled,
                modifier = Modifier.matchParentSize(),
                alignment = Alignment.CenterStart,
                actionWidth = actionWidth,
            )
        }
        if (onRemove != null) {
            SwipeAction(
                icon = Res.drawable.trash,
                label = "Remove",
                background = MaterialTheme.colorScheme.errorContainer,
                color = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { perform(onRemove) },
                enabled = enabled,
                modifier = Modifier.matchParentSize(),
                alignment = Alignment.CenterEnd,
                actionWidth = actionWidth,
            )
        }
        val foreground = Modifier.offset { IntOffset(state.requireOffset().roundToInt(), 0) }
            .anchoredDraggable(
                state = state,
                reverseDirection = false,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
            )
            .background(MaterialTheme.colorScheme.surface)
            .semantics(mergeDescendants = true) {
                customActions = if (!enabled) emptyList() else buildList {
                    if (onAddToList != null) add(CustomAccessibilityAction("Add $movieTitle to lists") { perform(onAddToList); true })
                    if (onRemove != null) add(CustomAccessibilityAction("Remove $movieTitle from list") { perform(onRemove); true })
                }
            }
        content(foreground) {
            if (abs(state.requireOffset()) > 0.5f) perform {} else onOpen()
        }
    }
}

@Composable
private fun SwipeAction(
    icon: DrawableResource,
    label: String,
    background: Color,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    alignment: Alignment,
    actionWidth: Dp,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = alignment) {
        Column(
            Modifier.width(actionWidth).fillMaxHeight().background(background)
                .clickable(enabled = enabled, onClick = onClick).clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = color)
            Text(label, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}
