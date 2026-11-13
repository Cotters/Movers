package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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

/** Short swipes reveal buttons; opted-in, non-destructive actions also support full swipes. */
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
    val density = LocalDensity.current
    val widthPx = with(density) { actionWidth.toPx() }
    val velocityThreshold = with(density) { 125.dp.toPx() }
    val state = remember(widthPx, direction) { SwipeActionsState(widthPx, direction) }
    val scope = rememberCoroutineScope()
    val currentStart = rememberUpdatedState(startAction)
    val currentEnd = rememberUpdatedState(endAction)
    val revealingStart by remember(state, direction) { derivedStateOf { state.offset * direction >= 0 } }
    val armedAction by remember(state, startAction, endAction) {
        derivedStateOf { state.fullSwipeAction(startAction, endAction) }
    }
    LaunchedEffect(enabled, state) {
        if (!enabled) state.close(scope)
    }
    val dragGesture = Modifier.pointerInput(state, enabled, velocityThreshold) {
        if (!enabled) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = { state.startDrag() },
            onDragEnd = { state.finishDrag(scope, currentStart.value, currentEnd.value, velocityThreshold) },
            onDragCancel = { state.close(scope) },
            onHorizontalDrag = state::dragBy,
        )
    }
    Box(modifier.fillMaxWidth().clipToBounds().onSizeChanged {
        state.updateAnchors(it.width, startAction, endAction)
    }.then(dragGesture)) {
        // These buttons are represented by custom actions on the foreground's semantics.
        val colors = MoversDesign.tokens.colors
        val action = if (revealingStart) startAction else endAction
        if (action != null) {
            SwipeAction(
                action = action,
                background = when {
                    action === armedAction -> colors.accent
                    action.destructive -> colors.dangerSurface
                    else -> colors.raised
                },
                color = when {
                    !action.enabled -> colors.muted
                    action === armedAction -> colors.onAccent
                    action.destructive -> colors.danger
                    else -> colors.accent
                },
                onClick = { state.close(scope, action.onClick) },
                enabled = enabled && action.enabled,
                modifier = Modifier.matchParentSize(),
                alignment = if (revealingStart) Alignment.CenterStart else Alignment.CenterEnd,
                actionWidth = actionWidth,
            )
        }
        val foreground = Modifier.offset { IntOffset(state.offset.roundToInt(), 0) }
            .background(colors.background)
            .semantics(mergeDescendants = true) {
                customActions = if (!enabled) emptyList() else listOfNotNull(startAction, endAction)
                    .filter { it.enabled }
                    .map { action ->
                        CustomAccessibilityAction(action.accessibilityLabel) {
                            state.close(scope, action.onClick)
                            true
                        }
                    }
            }
        content(foreground) {
            if (abs(state.offset) > 0.5f) state.close(scope) else onOpen()
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
    Box(modifier.background(background), contentAlignment = alignment) {
        Column(
            Modifier.width(actionWidth).fillMaxHeight()
                .clickable(enabled = enabled, onClick = onClick).clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            action.icon(color)
            MoversText(action.label, color = color, style = MoversDesign.tokens.type.caption)
        }
    }
}
