package com.jcotters.designsystem

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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class MoversSwipeAction(
    val label: String,
    val accessibilityLabel: String,
    val onClick: () -> Unit,
    val icon: @Composable (Color) -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

private enum class SwipeReveal { Closed, Start, End }

/** Reveals explicit actions; a swipe never performs a destructive operation. */
@Composable
fun MoversSwipeActions(
    onOpen: () -> Unit,
    startAction: MoversSwipeAction? = null,
    endAction: MoversSwipeAction? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, () -> Unit) -> Unit,
) {
    if (startAction == null && endAction == null) {
        content(modifier, onOpen)
        return
    }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val actionWidth = 88.dp
    val widthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val hasStart = startAction != null
    val hasEnd = endAction != null
    val state = remember(widthPx, direction, hasStart, hasEnd) {
        AnchoredDraggableState(SwipeReveal.Closed, DraggableAnchors {
            SwipeReveal.Closed at 0f
            if (hasStart) SwipeReveal.Start at widthPx * direction
            if (hasEnd) SwipeReveal.End at -widthPx * direction
        })
    }
    val scope = rememberCoroutineScope()
    fun perform(action: () -> Unit) {
        scope.launch {
            state.animateTo(SwipeReveal.Closed)
            action()
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) state.animateTo(SwipeReveal.Closed)
    }
    Box(modifier.fillMaxWidth().clipToBounds()) {
        // These buttons are represented by custom actions on the foreground's semantics.
        val colors = MoversDesign.tokens.colors
        listOf(startAction to Alignment.CenterStart, endAction to Alignment.CenterEnd).forEach { (action, alignment) ->
            if (action != null) SwipeAction(
                action = action,
                background = if (action.destructive) colors.dangerSurface else colors.raised,
                color = if (!action.enabled) colors.muted else if (action.destructive) colors.danger else colors.accent,
                onClick = { perform(action.onClick) },
                enabled = enabled && action.enabled,
                modifier = Modifier.matchParentSize(),
                alignment = alignment,
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
            .background(colors.background)
            .semantics(mergeDescendants = true) {
                customActions = if (!enabled) emptyList() else listOfNotNull(startAction, endAction)
                    .filter { it.enabled }
                    .map { action -> CustomAccessibilityAction(action.accessibilityLabel) { perform(action.onClick); true } }
            }
        content(foreground) {
            if (abs(state.requireOffset()) > 0.5f) perform {} else onOpen()
        }
    }
}

@Composable
private fun SwipeAction(
    action: MoversSwipeAction,
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
            action.icon(color)
            MoversText(action.label, color = color, style = MoversDesign.tokens.type.caption)
        }
    }
}
