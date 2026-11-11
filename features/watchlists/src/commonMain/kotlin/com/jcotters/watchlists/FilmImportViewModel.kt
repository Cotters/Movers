package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.watchlists.data.FilmReference
import com.jcotters.contract.watchlists.data.FilmResolution
import com.jcotters.contract.watchlists.data.ImportDisposition
import com.jcotters.contract.watchlists.data.MovieImportResolver
import com.jcotters.contract.watchlists.data.MovieLookupException
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.watchlists.documents.ColumnMapping
import com.jcotters.watchlists.documents.DocumentResult
import com.jcotters.watchlists.documents.ExportDocument
import com.jcotters.watchlists.documents.InputFormat
import com.jcotters.watchlists.documents.ListDocuments
import com.jcotters.watchlists.documents.ParsedFilm
import com.jcotters.watchlists.documents.ParsedInput
import com.jcotters.watchlists.documents.message
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class ImportRowState { Ready, LookingUp, Resolved, Pending, Imported, Skipped, Review, Failed }
internal data class ImportRow(
    val id: Int,
    val input: ParsedFilm,
    val status: ImportRowState,
    val movie: Movie? = null,
    val candidates: List<Movie> = emptyList(),
    val issue: String? = input.issue,
) {
    val remaining get() = status !in setOf(ImportRowState.Imported, ImportRowState.Skipped)
    val displayTitle get() = (movie?.title ?: input.reference.title.ifBlank { input.original }).take(200)
    val statusLabel get() = issue ?: when (status) {
        ImportRowState.Ready -> "Not processed"
        ImportRowState.LookingUp -> "Finding film…"
        ImportRowState.Resolved -> "Ready to import"
        ImportRowState.Pending -> "Waiting for confirmation"
        ImportRowState.Imported -> "Imported"
        ImportRowState.Skipped -> "Skipped"
        ImportRowState.Review -> "Needs attention"
        ImportRowState.Failed -> "Couldn't add film"
    }
}
internal data class FilmImportState(
    val listName: String = "",
    val shared: Boolean = false,
    val unavailable: Boolean = false,
    val text: String = "",
    val filename: String? = null,
    val format: InputFormat = InputFormat.Auto,
    val headerOverride: Boolean? = null,
    val mappingOverride: ColumnMapping? = null,
    val parsed: ParsedInput? = null,
    val parsing: Boolean = false,
    val started: Boolean = false,
    val running: Boolean = false,
    val paused: Boolean = false,
    val rows: List<ImportRow> = emptyList(),
    val error: String? = null,
    val resolving: Int? = null,
    val query: String = "",
    val candidates: List<Movie> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    val showAllRows: Boolean = false,
    val remainingDocument: ExportDocument? = null,
    val savingRemaining: Boolean = false,
    val documentMessage: String? = null,
) {
    val importedCount get() = rows.count { it.status == ImportRowState.Imported }
    val skippedCount get() = rows.count { it.status == ImportRowState.Skipped }
    val remainingCount get() = rows.count { it.remaining }
    val visibleRows get() = if (showAllRows) rows else rows.filter { it.remaining }
    val canResume get() = !unavailable && rows.any {
        it.status in setOf(ImportRowState.Ready, ImportRowState.LookingUp, ImportRowState.Resolved, ImportRowState.Pending, ImportRowState.Failed)
    }
    val canResolve get() = !running && !unavailable && rows.none { it.status == ImportRowState.Pending }
}

internal class FilmImportViewModel(private val lists: IWatchlistsRepository, private val resolver: MovieImportResolver) : ViewModel() {
    private val state = MutableStateFlow(FilmImportState())
    val uiState = state.asStateFlow()
    private var listId: String? = null
    private var source = ""
    private var parser: Job? = null
    private var worker: Job? = null
    private var search: Job? = null
    private var generation = 0
    private val matches = mutableMapOf<FilmReference, FilmResolution>()
    private data class Batch(val requestId: String, val films: List<SharedFilm>, val rows: List<Int>)
    private var pending: Batch? = null

    fun showAllRows(show: Boolean) { state.update { it.copy(showAllRows = show) } }
    fun remainingDocumentDispatched() { state.update { it.copy(remainingDocument = null) } }
    fun documentResult(result: DocumentResult) {
        state.update { it.copy(savingRemaining = false, documentMessage = result.message) }
    }

    fun saveRemaining() {
        val current = state.value
        if (current.running || current.savingRemaining) return
        val remaining = current.rows.filter { it.remaining }.map { it.input to (it.issue ?: "Not processed") }
        state.update { it.copy(savingRemaining = true, documentMessage = null) }
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.Default) { ListDocuments.remaining(remaining) }
                state.update { it.copy(remainingDocument = document) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(savingRemaining = false, documentMessage = "Couldn't save remaining films.") }
            }
        }
    }

    fun load(id: String) {
        if (listId != null) return
        listId = id
        viewModelScope.launch {
            try {
                lists.observeWatchlist(id).collect { content ->
                    state.update { it.copy(listName = content?.watchlist?.name.orEmpty(), shared = content?.watchlist?.shared != null, unavailable = content == null) }
                    if (content == null) { pause(); state.update { it.copy(error = "This list is unavailable.") } }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { pause(); state.update { it.copy(error = "Unable to read this list.", unavailable = true) } }
        }
    }

    fun sourceChanged(text: String) {
        if (state.value.started) return
        source = text
        state.update { it.copy(text = text, filename = null) }
        parse()
    }
    fun pickedFile(name: String, bytes: ByteArray) {
        if (state.value.started) return
        parser?.cancel()
        state.update { it.copy(parsing = true, error = null) }
        parser = viewModelScope.launch {
            try {
                source = withContext(Dispatchers.Default) { ListDocuments.decode(bytes) }
                state.update { it.copy(filename = name, text = "", format = InputFormat.Auto, headerOverride = null, mappingOverride = null) }
                parseNow()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(parsing = false, parsed = null, error = e.message ?: "Unable to read file.") } }
        }
    }
    fun formatChanged(format: InputFormat) { state.update { it.copy(format = format, mappingOverride = null, headerOverride = null) }; parse() }
    fun headerChanged(header: Boolean) { state.update { it.copy(headerOverride = header, mappingOverride = null) }; parse() }
    fun mappingChanged(mapping: ColumnMapping) { state.update { it.copy(mappingOverride = mapping) }; parse() }
    fun message(message: String?) { state.update { it.copy(error = message) } }
    private fun parse() {
        parser?.cancel()
        state.update { it.copy(parsing = true, error = null) }
        parser = viewModelScope.launch { delay(250); parseNow() }
    }
    private suspend fun parseNow() {
        val current = state.value
        try {
            val result = withContext(Dispatchers.Default) { ListDocuments.parse(source, current.format, current.headerOverride, current.mappingOverride) }
            state.update { it.copy(parsing = false, parsed = result, error = null) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { state.update { it.copy(parsing = false, parsed = null, error = e.message ?: "Unable to read these films.") } }
    }

    fun start() {
        if (state.value.unavailable || state.value.parsing || worker?.isActive == true) return
        if (!state.value.started) {
            val films = state.value.parsed?.films.orEmpty()
            if (films.isEmpty()) return
            state.update { it.copy(started = true, rows = films.mapIndexed { index, film ->
                ImportRow(index, film, when { film.unsupported -> ImportRowState.Skipped; film.issue != null -> ImportRowState.Review; else -> ImportRowState.Ready })
            }) }
        } else state.update { it.copy(rows = it.rows.map { row ->
            when (row.status) {
                ImportRowState.Failed -> row.copy(status = if (row.movie == null) ImportRowState.Ready else ImportRowState.Resolved, issue = null)
                ImportRowState.LookingUp -> row.copy(status = ImportRowState.Ready)
                else -> row
            }
        }) }
        runImport()
    }
    fun pause() {
        generation++
        worker?.cancel(); worker = null
        state.update { it.copy(running = false, paused = it.started && it.rows.any(ImportRow::remaining)) }
    }
    fun active(active: Boolean) { if (!active && state.value.running) pause() }

    private fun runImport(resolveOnly: Boolean = false) {
        val id = listId ?: return
        val version = ++generation
        state.update { it.copy(running = true, paused = false, error = null) }
        worker = viewModelScope.launch {
            try {
                if (pending != null && !commit(id)) return@launch
                while (true) {
                    var lookupFailed = false
                    val next = if (resolveOnly) emptyList() else state.value.rows.filter { it.status == ImportRowState.Ready }.take(50)
                    for (pair in next.groupBy { it.input.reference }.entries.chunked(2)) {
                        val results = coroutineScope {
                            pair.map { (reference, rows) -> async {
                                rows.forEach { row -> update(row.id) { it.copy(status = ImportRowState.LookingUp) } }
                                try { Triple(rows, resolve(reference), null) }
                                catch (e: CancellationException) { throw e }
                                catch (e: Exception) { Triple(rows, null, e.message ?: "Unable to look up this film.") }
                            } }.map { it.await() }
                        }
                        results.forEach { (rows, result, error) ->
                            rows.forEach { row -> update(row.id) {
                                when (result) {
                                    is FilmResolution.Matched -> it.copy(status = ImportRowState.Resolved, movie = result.movie, issue = null)
                                    is FilmResolution.Review -> it.copy(status = ImportRowState.Review, candidates = result.candidates, issue = result.reason)
                                    null -> it.copy(status = ImportRowState.Failed, issue = error)
                                }
                            } }
                            if (error != null) lookupFailed = true
                        }
                        if (lookupFailed) break
                    }
                    prepareBatch(id)
                    if (pending != null && !commit(id)) return@launch
                    if (lookupFailed) {
                        state.update { it.copy(paused = true, error = "Lookup paused. Retry when connected.") }; return@launch
                    }
                    if (resolveOnly || state.value.rows.none { it.status == ImportRowState.Ready || it.status == ImportRowState.Resolved }) break
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(paused = true, error = e.message ?: "Import paused. Retry when connected.") } }
            finally { if (generation == version) state.update { it.copy(running = false) } }
        }
    }
    private suspend fun resolve(reference: FilmReference): FilmResolution {
        matches[reference]?.let { return it }
        repeat(2) { attempt ->
            try { return resolver.resolve(reference).also { matches[reference] = it } }
            catch (e: MovieLookupException) { if (attempt == 1) throw e; delay(e.retryAfterMillis ?: 1000) }
        }
        error("Lookup did not complete")
    }
    private suspend fun prepareBatch(id: String) {
        if (pending != null) return
        // Shared membership is decided by the server, not a possibly stale Room snapshot.
        val existing = if (state.value.shared) emptySet() else lists.observeMovieIds(id).first()
        val handled = state.value.rows.filter { it.status == ImportRowState.Imported || it.status == ImportRowState.Skipped }.mapNotNull { it.movie?.id }.toMutableSet()
        val selected = mutableListOf<ImportRow>()
        state.value.rows.filter { it.status == ImportRowState.Resolved }.take(50).forEach { row ->
            val movie = checkNotNull(row.movie)
            when {
                !handled.add(movie.id) -> update(row.id) { it.copy(status = ImportRowState.Skipped, issue = "Duplicate in import.") }
                movie.id in existing -> update(row.id) { it.copy(status = ImportRowState.Skipped, issue = "Already in this list.") }
                else -> selected += row
            }
        }
        if (selected.isNotEmpty()) {
            pending = Batch(Uuid.random().toString(), selected.map { row -> checkNotNull(row.movie).let { SharedFilm(it.id,it.title,it.synopsis,it.releaseDate,it.posterUrl) } }, selected.map { it.id })
            selected.forEach { row -> update(row.id) { it.copy(status = ImportRowState.Pending, issue = "Waiting for confirmation.") } }
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
        batch.rows.forEach { rowId -> update(rowId) { row ->
            when (byId[row.movie?.id]?.disposition) {
                ImportDisposition.Imported -> row.copy(status = ImportRowState.Imported, issue = null)
                ImportDisposition.AlreadyPresent -> row.copy(status = ImportRowState.Skipped, issue = "Already in this list.")
                ImportDisposition.Rejected -> row.copy(status = ImportRowState.Failed, issue = "This film couldn't be added.")
                null -> row.copy(status = ImportRowState.Failed, issue = "No confirmation received.")
            }
        } }
        pending = null
        return true
    }
    private fun update(id: Int, block: (ImportRow) -> ImportRow) { state.update { it.copy(rows = it.rows.map { row -> if (row.id == id) block(row) else row }) } }

    fun openResolution(id: Int) {
        if (state.value.running || pending != null) return
        val row = state.value.rows.firstOrNull { it.id == id } ?: return
        state.update { it.copy(resolving = id, query = row.input.reference.title.ifBlank { row.input.original.take(100) }, candidates = row.candidates, searchError = null) }
    }
    fun closeResolution() { search?.cancel(); state.update { it.copy(resolving = null, searching = false) } }
    fun queryChanged(query: String) {
        state.update { it.copy(query = query) }
        searchQuery(query, 300)
    }
    fun search() {
        searchQuery(state.value.query, 0)
    }
    private fun searchQuery(query: String, debounceMillis: Long) {
        search?.cancel()
        if (query.isBlank()) { state.update { it.copy(searching = false, candidates = emptyList(), searchError = null) }; return }
        state.update { it.copy(searching = true, candidates = emptyList(), searchError = null) }
        search = viewModelScope.launch {
            try {
                delay(debounceMillis)
                val results = resolver.search(query)
                state.update { if (it.query == query && it.resolving != null) it.copy(candidates = results, searching = false) else it }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(searching = false, searchError = e.message ?: "Unable to search. Retry.") } }
        }
    }
    fun select(movie: Movie) {
        val rowId = state.value.resolving ?: return
        if (state.value.running || pending != null) return
        update(rowId) { it.copy(status = ImportRowState.Resolved, movie = movie, issue = null) }
        closeResolution()
        runImport(resolveOnly = true)
    }
}
