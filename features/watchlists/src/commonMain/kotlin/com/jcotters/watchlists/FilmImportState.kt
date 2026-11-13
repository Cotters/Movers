package com.jcotters.watchlists

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.watchlists.documents.ColumnMapping
import com.jcotters.watchlists.documents.ExportDocument
import com.jcotters.watchlists.documents.InputFormat
import com.jcotters.watchlists.documents.ParsedFilm
import com.jcotters.watchlists.documents.ParsedInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal const val MAX_IMPORT_TITLE_LENGTH = 200

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
    val displayTitle
        get() = (movie?.title ?: input.reference.title.ifBlank { input.original }).take(
            MAX_IMPORT_TITLE_LENGTH
        )
    val statusLabel
        get() = issue ?: when (status) {
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
    val progressLabel: String
        get() = "$importedCount imported · $skippedCount skipped · $remainingCount " +
            if (running) "remaining" else "need attention"
    val importedCount get() = rows.count { it.status == ImportRowState.Imported }
    val skippedCount get() = rows.count { it.status == ImportRowState.Skipped }
    val remainingCount get() = rows.count { it.remaining }
    val visibleRows get() = if (showAllRows) rows else rows.filter { it.remaining }
    val canResume
        get() = !unavailable && rows.any {
            it.status in setOf(
                ImportRowState.Ready,
                ImportRowState.LookingUp,
                ImportRowState.Resolved,
                ImportRowState.Pending,
                ImportRowState.Failed
            )
        }
    val canResolve get() = !running && !unavailable && rows.none { it.status == ImportRowState.Pending }
}

internal fun MutableStateFlow<FilmImportState>.updateRow(id: Int, block: (ImportRow) -> ImportRow) {
    update { it.copy(rows = it.rows.map { row -> if (row.id == id) block(row) else row }) }
}
