package com.jcotters.watchlists

import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.data.ImportDisposition
import com.jcotters.contract.watchlists.data.MAX_IMPORT_BATCH_SIZE
import com.jcotters.contract.watchlists.data.MovieImportResolver
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns one import's pause/resume lifecycle and stable pending receipt. */
internal class FilmImportSession(
    private val lists: IWatchlistsRepository,
    resolver: MovieImportResolver,
    private val state: MutableStateFlow<FilmImportState>,
    private val scope: CoroutineScope,
) {
    private var listId: String? = null
    private var worker: Job? = null
    private var generation = 0
    private val lookup = FilmImportLookup(resolver, state)

    private data class Batch(val requestId: String, val films: List<SharedFilm>, val rows: List<Int>)

    private var pending: Batch? = null
    val hasPendingBatch: Boolean get() = pending != null

    fun importResolved() = runImport(resolveOnly = true)

    fun load(id: String) {
        if (listId != null) return
        listId = id
        scope.launch {
            try {
                lists.observeWatchlist(id).collect { content ->
                    state.update {
                        it.copy(
                            listName = content?.watchlist?.name.orEmpty(),
                            shared = content?.watchlist?.shared != null,
                            unavailable = content == null
                        )
                    }
                    if (content == null) {
                        pause()
                        state.update { it.copy(error = "This list is unavailable.") }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                pause()
                state.update { it.copy(error = "Unable to read this list.", unavailable = true) }
            }
        }
    }

    fun start() {
        if (state.value.unavailable || state.value.parsing || worker?.isActive == true) return
        if (!state.value.started) {
            val films = state.value.parsed?.films.orEmpty()
            if (films.isEmpty()) return
            state.update {
                it.copy(started = true, rows = films.mapIndexed { index, film ->
                    ImportRow(
                        index, film, when {
                            film.unsupported -> ImportRowState.Skipped
                            film.issue != null -> ImportRowState.Review
                            else -> ImportRowState.Ready
                        }
                    )
                })
            }
        } else state.update {
            it.copy(rows = it.rows.map { row ->
                when (row.status) {
                    ImportRowState.Failed -> row.copy(
                        status = if (row.movie == null) ImportRowState.Ready else ImportRowState.Resolved,
                        issue = null
                    )

                    ImportRowState.LookingUp -> row.copy(status = ImportRowState.Ready)
                    else -> row
                }
            })
        }
        runImport()
    }

    fun pause() {
        generation++
        worker?.cancel()
        worker = null
        state.update { it.copy(running = false, paused = it.started && it.rows.any(ImportRow::remaining)) }
    }

    fun active(active: Boolean) {
        if (!active && state.value.running) pause()
    }

    private fun runImport(resolveOnly: Boolean = false) {
        val id = listId ?: return
        val version = ++generation
        state.update { it.copy(running = true, paused = false, error = null) }
        worker = scope.launch {
            try {
                recoverableResult {
                    processBatches(id, resolveOnly)
                }.onFailure { e ->
                    state.update { it.copy(paused = true, error = e.message ?: "Import paused. Retry when connected.") }
                }
            } finally {
                if (generation == version) state.update { it.copy(running = false) }
            }
        }
    }

    private suspend fun processBatches(id: String, resolveOnly: Boolean) {
        if (pending != null && !commit(id)) return
        while (true) {
            val lookupFailed = if (resolveOnly) false else lookup.resolveReady()
            prepareBatch(id)
            if (pending != null && !commit(id)) return
            if (lookupFailed) {
                state.update {
                    it.copy(
                        paused = true,
                        error = "Lookup paused. Retry when connected."
                    )
                }
                return
            }
            val hasWork = state.value.rows.any {
                it.status == ImportRowState.Ready || it.status == ImportRowState.Resolved
            }
            if (resolveOnly || !hasWork) break
        }
    }

    private suspend fun prepareBatch(id: String) {
        if (pending != null) return
        // Shared membership is decided by the server, not a possibly stale Room snapshot.
        val existing = if (state.value.shared) emptySet() else lists.observeMovieIds(id).first()
        val handled =
            state.value.rows.filter { it.status == ImportRowState.Imported || it.status == ImportRowState.Skipped }
                .mapNotNull { it.movie?.id }.toMutableSet()
        val selected = mutableListOf<ImportRow>()
        state.value.rows.filter { it.status == ImportRowState.Resolved }.take(MAX_IMPORT_BATCH_SIZE).forEach { row ->
            val movie = checkNotNull(row.movie)
            when {
                !handled.add(movie.id) -> state.updateRow(row.id) {
                    it.copy(
                        status = ImportRowState.Skipped,
                        issue = "Duplicate in import."
                    )
                }

                movie.id in existing -> state.updateRow(row.id) {
                    it.copy(
                        status = ImportRowState.Skipped,
                        issue = "Already in this list."
                    )
                }

                else -> selected += row
            }
        }
        if (selected.isNotEmpty()) {
            pending = Batch(
                Uuid.random().toString(),
                selected.map { row ->
                    checkNotNull(row.movie).let {
                        SharedFilm(
                            it.id,
                            it.title,
                            it.synopsis,
                            it.releaseDate,
                            it.posterUrl
                        )
                    }
                },
                selected.map { it.id })
            selected.forEach { row ->
                state.updateRow(row.id) {
                    it.copy(
                        status = ImportRowState.Pending,
                        issue = "Waiting for confirmation."
                    )
                }
            }
        }
    }

    private suspend fun commit(id: String): Boolean {
        val batch = pending ?: return true
        val outcome = lists.importMovies(batch.requestId, id, batch.films)
        val result = outcome.getOrElse { error ->
            state.update { it.copy(paused = true, error = error.message ?: "Import paused. Retry when connected.") }
            return false
        }
        val byId = result.outcomes.associateBy { it.movieId }
        batch.rows.forEach { rowId ->
            state.updateRow(rowId) { row ->
                when (byId[row.movie?.id]?.disposition) {
                    ImportDisposition.Imported -> row.copy(status = ImportRowState.Imported, issue = null)
                    ImportDisposition.AlreadyPresent -> row.copy(
                        status = ImportRowState.Skipped,
                        issue = "Already in this list."
                    )

                    ImportDisposition.Rejected -> row.copy(
                        status = ImportRowState.Failed,
                        issue = "This film couldn't be added."
                    )

                    null -> row.copy(status = ImportRowState.Failed, issue = "No confirmation received.")
                }
            }
        }
        pending = null
        return true
    }
}
