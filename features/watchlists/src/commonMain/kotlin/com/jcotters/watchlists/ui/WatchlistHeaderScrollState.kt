package com.jcotters.watchlists.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberWatchlistHeaderScrollState(
    listState: LazyListState,
    gridState: LazyGridState,
    showGrid: Boolean,
): WatchlistHeaderScrollState {
    val density = LocalDensity.current
    return remember(listState, gridState, showGrid, density) {
        val minimumReveal = with(density) { 64.dp.toPx() }
        val fallbackRowHeight = with(density) { 180.dp.toPx() }
        WatchlistHeaderScrollState(
            hideDistance = with(density) { 24.dp.toPx() },
            revealDistance = {
                val measuredRowHeight = if (showGrid) {
                    gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.size.height }
                } else {
                    listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.size }
                }
                val rowHeight = measuredRowHeight?.toFloat() ?: fallbackRowHeight
                val viewport = if (showGrid) {
                    gridState.layoutInfo.viewportSize.height
                } else {
                    listState.layoutInfo.viewportSize.height
                }
                // About one rendered row, bounded for small/landscape viewports.
                val limit = viewport.coerceAtLeast(1) / 2f
                rowHeight.coerceIn(minimumReveal.coerceAtMost(limit), limit)
            },
            atTop = { if (showGrid) !gridState.canScrollBackward else !listState.canScrollBackward },
            hasItems = { if (showGrid) gridState.layoutInfo.totalItemsCount > 0 else listState.layoutInfo.totalItemsCount > 0 },
        )
    }
}

internal class WatchlistHeaderScrollState(
    private val hideDistance: Float,
    private val revealDistance: () -> Float,
    private val atTop: () -> Boolean,
    private val hasItems: () -> Boolean,
) : NestedScrollConnection {
    var visible by mutableStateOf(true)
        private set
    private var upwardTravel = 0f
    private var downwardTravel = 0f

    fun show() {
        visible = true
        upwardTravel = 0f
        downwardTravel = 0f
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!hasItems()) {
            show()
            return Offset.Zero
        }
        // Check the top only during upward movement, never because header/IME resizing
        // clamped a short list to the top. This prevents collapse/reopen feedback loops.
        if (consumed.y > 0f && atTop()) {
            show()
            return Offset.Zero
        }
        // An unconsumed drag can dismiss the header even when a short list cannot scroll.
        val delta =
            if (consumed.y != 0f) consumed.y else if (source == NestedScrollSource.UserInput) available.y else 0f
        if (delta < 0f) {
            upwardTravel = 0f
            downwardTravel = (downwardTravel - delta).coerceAtMost(hideDistance)
            if (downwardTravel >= hideDistance) visible = false
        } else if (delta > 0f) {
            downwardTravel = 0f
            val threshold = revealDistance()
            upwardTravel = (upwardTravel + delta).coerceAtMost(threshold)
            if (upwardTravel >= threshold) visible = true
        }
        return Offset.Zero
    }
}
