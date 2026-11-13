package com.jcotters.watchlists.documents

import com.jcotters.contract.detail.domain.models.Movie

internal object ListDocuments {
    fun decode(bytes: ByteArray): String = ListDocumentEncoding.decode(bytes)

    fun parse(
        source: String,
        requested: InputFormat = InputFormat.Auto,
        headerOverride: Boolean? = null,
        mappingOverride: ColumnMapping? = null,
    ): ParsedInput {
        require(source.encodeToByteArray().size <= MAX_IMPORT_BYTES) { "Use up to 5 MB of text." }
        val text = source.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return ParsedInput(emptyList(), InputFormat.Lines)
        val format = if (requested == InputFormat.Auto) DelimitedFilmParser.detect(text) else requested
        val parsed = when (format) {
            InputFormat.Json -> MoversJsonDocument.parse(text)
            InputFormat.Lines -> ParsedInput(
                text.lines().map(String::trim).filter(String::isNotBlank).map(FilmReferenceParser::parse), format
            )

            else -> DelimitedFilmParser.parse(text, format, headerOverride, mappingOverride)
        }
        require(parsed.films.size <= MAX_IMPORT_ENTRIES) { "Import up to 10,000 films at a time." }
        return parsed
    }

    fun export(name: String, movies: List<Movie>, format: ExportFormat): ExportDocument =
        ListDocumentExporter.export(name, movies, format)

    fun remaining(rows: List<Pair<ParsedFilm, String>>): ExportDocument = ListDocumentExporter.remaining(rows)
}
