package com.jcotters.watchlists.documents

import com.jcotters.contract.watchlists.data.FilmReference

internal const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
internal const val MAX_IMPORT_ENTRIES = 10_000

internal enum class InputFormat(val label: String, val separator: Char? = null) {
    Auto("Auto"), Lines("Newlines"), Comma("Commas", ','), Semicolon("Semicolons", ';'), Tab(
        "Tabs",
        '\t'
    ),
    Pipe("Pipes", '|'), Json("Movers JSON")
}

internal enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    Csv("CSV", "csv", "text/csv"), Text("Text", "txt", "text/plain"), Json("JSON", "json", "application/json")
}

internal data class ColumnMapping(
    val title: Int? = 0,
    val year: Int? = null,
    val movieId: Int? = null,
    val imdbId: Int? = null
)

internal data class ParsedFilm(
    val reference: FilmReference,
    val original: String,
    val issue: String? = null,
    val unsupported: Boolean = false
)

internal data class ParsedInput(
    val films: List<ParsedFilm>,
    val format: InputFormat,
    val headings: List<String> = emptyList(),
    val hasHeader: Boolean = false,
    val mapping: ColumnMapping = ColumnMapping()
)

internal data class ExportDocument(val name: String, val mimeType: String, val bytes: ByteArray)
