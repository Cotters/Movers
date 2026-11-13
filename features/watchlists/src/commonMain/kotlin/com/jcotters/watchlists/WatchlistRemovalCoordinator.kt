package com.jcotters.watchlists

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal data class RemovalKey(val listId: String, val movieId: Int)
internal data class RemovalTarget(val key: RemovalKey, val title: String)
internal data class PendingRemoval(val id: String, val targets: List<RemovalTarget>)
internal data class WatchlistRemovals(
    val pending: PendingRemoval? = null,
    val committing: Set<RemovalKey> = emptySet(),
) {
    fun hiddenMovieIds(listId: String): Set<Int> = buildSet {
        pending?.targets?.filter { it.key.listId == listId }?.forEach { add(it.key.movieId) }
        committing.filter { it.listId == listId }.forEach { add(it.movieId) }
    }
}

/** App-owned undo window; no database write happens before the snackbar decision. */
internal class WatchlistRemovalCoordinator(private val repository: IWatchlistsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(WatchlistRemovals())
    val removals = state.asStateFlow()
    private val failures = Channel<String>(Channel.UNLIMITED)
    val errors = failures.receiveAsFlow()

    fun stage(listId: String, movie: Movie) {
        val key = RemovalKey(listId, movie.id)
        val id = Uuid.random().toString()
        state.update { current ->
            if (key in current.committing || current.pending?.targets?.any { it.key == key } == true) current
            else current.copy(
                pending = PendingRemoval(
                    id,
                    current.pending?.targets.orEmpty() + RemovalTarget(key, movie.title)
                )
            )
        }
    }

    fun undo(id: String) {
        state.update { if (it.pending?.id == id) it.copy(pending = null) else it }
    }

    fun commit(id: String) {
        while (true) {
            val current = state.value
            val batch = current.pending?.takeIf { it.id == id } ?: return
            val next = current.copy(pending = null, committing = current.committing + batch.targets.map { it.key })
            if (!state.compareAndSet(current, next)) continue
            scope.launch {
                for (target in batch.targets) {
                    try {
                        repository.removeMovie(target.key.listId, target.key.movieId).onFailure {
                            failures.send("Couldn't remove “${target.title}”.")
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        failures.send("Couldn't remove “${target.title}”.")
                    } finally {
                        state.update { it.copy(committing = it.committing - target.key) }
                    }
                }
            }
            return
        }
    }

    fun close() {
        scope.cancel()
        failures.close()
    }
}
