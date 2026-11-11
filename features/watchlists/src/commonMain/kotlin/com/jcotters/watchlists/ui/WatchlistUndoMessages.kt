package com.jcotters.watchlists.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.jcotters.presentation.LocalSnackbarHostState
import com.jcotters.watchlists.WatchlistRemovalCoordinator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** Hosted by the app shell so Undo survives tab changes and leaving the source list. */
@Composable
fun WatchlistUndoMessages() {
    val coordinator: WatchlistRemovalCoordinator = koinInject()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(coordinator, snackbar) {
        coordinator.removals.map { it.pending }.distinctUntilChanged().collectLatest { pending ->
            if (pending == null) return@collectLatest
            val message = if (pending.targets.size == 1) "Removed “${pending.targets.single().title}”."
                else "Removed ${pending.targets.size} films."
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) coordinator.undo(pending.id) else coordinator.commit(pending.id)
        }
    }
    LaunchedEffect(coordinator, snackbar) {
        coordinator.errors.collect { snackbar.showSnackbar(it) }
    }
}
