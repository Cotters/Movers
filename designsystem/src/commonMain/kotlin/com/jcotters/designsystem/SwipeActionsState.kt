package com.jcotters.designsystem

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class SwipePosition { Closed, Start, End, FullStart, FullEnd }

/** UI-local drag geometry and settling; feature callbacks retain ownership of their actions. */
internal class SwipeActionsState(private val actionWidth: Float, private val direction: Float) {
    private val drag = AnchoredDraggableState(SwipePosition.Closed, DraggableAnchors {
        SwipePosition.Closed at 0f
    })
    private val velocityTracker = VelocityTracker()
    private var animation: Job? = null
    private var rowWidth = 0f

    val offset: Float get() = drag.requireOffset()

    fun updateAnchors(width: Int, start: MoversSwipeAction?, end: MoversSwipeAction?) {
        rowWidth = width.toFloat()
        drag.updateAnchors(DraggableAnchors {
            SwipePosition.Closed at 0f
            if (start != null) SwipePosition.Start at actionWidth * direction
            if (end != null) SwipePosition.End at -actionWidth * direction
            if (start?.canFullSwipe == true && rowWidth > actionWidth * 2) {
                SwipePosition.FullStart at rowWidth * direction
            }
            if (end?.canFullSwipe == true && rowWidth > actionWidth * 2) {
                SwipePosition.FullEnd at -rowWidth * direction
            }
        })
    }

    fun startDrag() {
        animation?.cancel()
        velocityTracker.resetTracking()
    }

    fun dragBy(change: PointerInputChange, amount: Float) {
        velocityTracker.addPointerInputChange(change)
        drag.dispatchRawDelta(amount)
    }

    fun fullSwipeAction(start: MoversSwipeAction?, end: MoversSwipeAction?): MoversSwipeAction? {
        // Keep activation well beyond the normal reveal, including on narrow rows.
        val threshold = maxOf(actionWidth * 2, rowWidth * 0.65f)
        if (rowWidth <= actionWidth * 2 || abs(offset) < threshold) return null
        val action = if (offset * direction > 0) start else end
        return action?.takeIf { it.canFullSwipe }
    }

    fun finishDrag(
        scope: CoroutineScope,
        start: MoversSwipeAction?,
        end: MoversSwipeAction?,
        velocityThreshold: Float,
    ) {
        val action = fullSwipeAction(start, end)
        val velocity = velocityTracker.calculateVelocity().x
        val reveal = if (abs(velocity) >= velocityThreshold) offset * velocity > 0 else abs(offset) >= actionWidth / 2
        val target = when {
            action != null || !reveal -> SwipePosition.Closed
            offset * direction > 0 -> SwipePosition.Start
            else -> SwipePosition.End
        }
        animation?.cancel()
        animation = scope.launch {
            drag.animateTo(target)
            action?.onClick()
        }
    }

    fun close(scope: CoroutineScope, action: () -> Unit = {}) {
        animation?.cancel()
        animation = scope.launch {
            drag.animateTo(SwipePosition.Closed)
            action()
        }
    }
}
