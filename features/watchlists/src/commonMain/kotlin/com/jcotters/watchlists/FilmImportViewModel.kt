package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.data.MovieImportResolver
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.watchlists.documents.DocumentResult
import com.jcotters.watchlists.documents.ListDocuments
import com.jcotters.watchlists.documents.message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_INITIAL_SEARCH_LENGTH = 100
private const val SEARCH_DEBOUNCE_MILLIS = 300L

internal class FilmImportViewModel(lists: IWatchlistsRepository, private val resolver: MovieImportResolver) :
    ViewModel() {
    private val state = MutableStateFlow(FilmImportState())
    val uiState = state.asStateFlow()
    val input = FilmImportInput(state, viewModelScope)
    val session = FilmImportSession(lists, resolver, state, viewModelScope)
    private var search: Job? = null

    fun showAllRows(show: Boolean) {
        state.update { it.copy(showAllRows = show) }
    }

    fun remainingDocumentDispatched() {
        state.update { it.copy(remainingDocument = null) }
    }

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

    fun openResolution(id: Int) {
        if (state.value.running || session.hasPendingBatch) return
        val row = state.value.rows.firstOrNull { it.id == id } ?: return
        state.update {
            it.copy(
                resolving = id,
                query = row.input.reference.title.ifBlank { row.input.original.take(MAX_INITIAL_SEARCH_LENGTH) },
                candidates = row.candidates,
                searchError = null
            )
        }
    }

    fun closeResolution() {
        search?.cancel()
        state.update { it.copy(resolving = null, searching = false) }
    }

    fun queryChanged(query: String) {
        state.update { it.copy(query = query) }
        searchQuery(query, SEARCH_DEBOUNCE_MILLIS)
    }

    fun search() {
        searchQuery(state.value.query, 0)
    }

    private fun searchQuery(query: String, debounceMillis: Long) {
        search?.cancel()
        if (query.isBlank()) {
            state.update { it.copy(searching = false, candidates = emptyList(), searchError = null) }
            return
        }
        state.update { it.copy(searching = true, candidates = emptyList(), searchError = null) }
        search = viewModelScope.launch {
            recoverableResult {
                delay(debounceMillis)
                val results = resolver.search(query)
                state.update {
                    if (it.query == query && it.resolving != null) it.copy(
                        candidates = results,
                        searching = false
                    ) else it
                }
            }.onFailure { e ->
                state.update { it.copy(searching = false, searchError = e.message ?: "Unable to search. Retry.") }
            }
        }
    }

    fun select(movie: Movie) {
        val rowId = state.value.resolving ?: return
        if (state.value.running || session.hasPendingBatch) return
        state.updateRow(rowId) { it.copy(status = ImportRowState.Resolved, movie = movie, issue = null) }
        closeResolution()
        session.importResolved()
    }
}
