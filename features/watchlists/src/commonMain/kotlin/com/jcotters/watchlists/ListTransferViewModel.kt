package com.jcotters.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.watchlists.documents.DocumentResult
import com.jcotters.watchlists.documents.ExportDocument
import com.jcotters.watchlists.documents.ExportFormat
import com.jcotters.watchlists.documents.ListDocuments
import com.jcotters.watchlists.documents.message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ListTransferState(
    val loading: Boolean = true,
    val content: WatchlistContent? = null,
    val error: String? = null,
    val format: ExportFormat = ExportFormat.Csv,
    val exporting: Boolean = false,
    val document: ExportDocument? = null,
    val shareDocument: Boolean = false,
    val exportMessage: String? = null,
)

internal class ListTransferViewModel(
    private val repository: IWatchlistsRepository,
    private val removals: WatchlistRemovalCoordinator,
) : ViewModel() {
    private val state = MutableStateFlow(ListTransferState())
    val uiState = state.asStateFlow()
    private var listId: String? = null
    private var observation: Job? = null

    fun changeFormat(format: ExportFormat) {
        if (!state.value.exporting) state.update { it.copy(format = format) }
    }

    fun export(share: Boolean) {
        val current = state.value
        val content = current.content ?: return
        if (current.exporting) return
        state.update { it.copy(exporting = true, exportMessage = null) }
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.Default) {
                    ListDocuments.export(content.watchlist.name, content.movies, current.format)
                }
                state.update { it.copy(document = document, shareDocument = share) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(exporting = false, exportMessage = "Couldn't export this list.") }
            }
        }
    }

    fun documentDispatched() { state.update { it.copy(document = null) } }
    fun documentResult(result: DocumentResult) {
        state.update { it.copy(exporting = false, document = null, exportMessage = result.message) }
    }

    fun load(id: String) {
        if (id == listId && observation?.isActive == true) return
        observation?.cancel()
        listId = id
        state.value = ListTransferState()
        observation = viewModelScope.launch {
            combine(repository.observeWatchlist(id), removals.removals) { content, pending ->
                val hidden = pending.hiddenMovieIds(id)
                content?.copy(movies = content.movies.filterNot { it.id in hidden })
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                state.update { it.copy(loading = false, error = "Unable to read this list.") }
            }.collect { content -> state.update { it.copy(loading = false, content = content) } }
        }
    }
}
