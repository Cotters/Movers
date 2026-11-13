package com.jcotters.watchlists.documents

import com.jcotters.contract.watchlists.data.FilmReference

internal object DelimitedFilmParser {
    private val titleHeaders =
        setOf("title", "name", "filmtitle", "movietitle", "filmname", "moviename", "film", "movie", "originaltitle")
    private val urlHeaders = setOf("url", "uri", "letterboxduri")
    private val yearHeaders = setOf("year", "releaseyear")
    private val movieHeaders = setOf("tmdbid", "themoviedbid", "movieid", "tmdburl")
    private val imdbHeaders = setOf("imdbid", "const", "tconst", "imdburl")
    private val referenceHeaders = titleHeaders + movieHeaders + imdbHeaders + urlHeaders
    private val allHeaders = referenceHeaders + yearHeaders
    private val nonFilmTypes = setOf("tvseries", "tvminiseries", "tvepisode", "series", "episode")

    fun detect(text: String): InputFormat {
        if (text.startsWith('{')) return InputFormat.Json
        val singleColumn = DelimitedTableParser(text, ',').read().rows.firstOrNull()?.singleOrNull()
        if (singleColumn != null && header(singleColumn) in allHeaders) return InputFormat.Comma
        return listOf(InputFormat.Tab, InputFormat.Semicolon, InputFormat.Pipe, InputFormat.Comma)
            .firstOrNull { format ->
                val table = DelimitedTableParser(text, checkNotNull(format.separator)).read()
                val first = table.rows.firstOrNull().orEmpty()
                val headerMatches = first.size > 1 && first.any { header(it) in referenceHeaders }
                val singleLine = !text.contains('\n') && !text.contains('\r') && first.size > 1
                headerMatches || (table.rows.isNotEmpty() && titleYearRows(table.rows)) || singleLine
            } ?: InputFormat.Lines
    }

    fun parse(
        text: String,
        format: InputFormat,
        headerOverride: Boolean?,
        mappingOverride: ColumnMapping?,
    ): ParsedInput {
        val table = DelimitedTableParser(text, checkNotNull(format.separator)).read()
        val first = table.rows.firstOrNull().orEmpty()
        val hasHeader = headerOverride ?: first.any { header(it) in allHeaders }
        val headings = if (hasHeader) first else List(table.rows.maxOfOrNull { it.size } ?: 0) { "Column ${it + 1}" }
        val yearColumn = column(first, yearHeaders, hasHeader)
            ?: if (!hasHeader && titleYearRows(table.rows)) 1 else null
        val mapping = mappingOverride ?: inferMapping(first, hasHeader, yearColumn)
        val records = if (hasHeader) table.rows.drop(1) else table.rows
        val films = if (!hasHeader && mappingOverride == null && yearColumn == null) {
            records.flatMap { cells -> cells.filter(String::isNotBlank).map(FilmReferenceParser::parse) }
        } else {
            val typeColumn = column(first, setOf("titletype", "type"), hasHeader)
            records.filter { it.any(String::isNotBlank) }.map { parseFilm(it, mapping, typeColumn) }
        }
        val unreadable = table.problem?.let {
            listOf(ParsedFilm(FilmReference(), it, "Unreadable CSV section. Choose a format or fix the quoted field."))
        }.orEmpty()
        return ParsedInput(films + unreadable, format, headings, hasHeader, mapping)
    }

    private fun inferMapping(first: List<String>, hasHeader: Boolean, yearColumn: Int?): ColumnMapping {
        val movie = column(first, movieHeaders, hasHeader)
        val imdb = column(first, imdbHeaders, hasHeader)
        val defaultTitle = if (hasHeader && (movie != null || imdb != null)) null else 0
        val title = column(first, titleHeaders, hasHeader) ?: column(first, urlHeaders, hasHeader) ?: defaultTitle
        return ColumnMapping(title, yearColumn, movie, imdb)
    }

    private fun parseFilm(cells: List<String>, mapping: ColumnMapping, typeColumn: Int?): ParsedFilm {
        fun cell(column: Int?) = column?.let { cells.getOrNull(it)?.trim() }.orEmpty()
        val title = cell(mapping.title)
        val yearText = cell(mapping.year)
        val idText = cell(mapping.movieId)
        val imdbText = cell(mapping.imdbId)
        val base = FilmReferenceParser.parse(title)
        val id = idText.toIntOrNull()?.takeIf { it > 0 } ?: FilmReferenceParser.movieId(idText)
        val imdb = FilmReferenceParser.imdbId(imdbText)
        val unsupported = header(cell(typeColumn)) in nonFilmTypes
        val year = yearText.toIntOrNull()?.takeIf { it in VALID_FILM_YEARS }
        val reference = base.reference.copy(
            year = year ?: base.reference.year,
            movieId = id ?: base.reference.movieId,
            imdbId = imdb ?: base.reference.imdbId,
        )
        val issue = when {
            unsupported -> "Not a film."
            yearText.isNotBlank() && year == null -> "Invalid year."
            missingReference(reference) -> "Missing title or movie ID."
            else -> if (id == null && imdb == null) base.issue else null
        }
        return ParsedFilm(reference, title.ifBlank { idText.ifBlank { imdbText } }, issue, unsupported)
    }

    private fun missingReference(reference: FilmReference): Boolean =
        reference.title.isBlank() && reference.movieId == null && reference.imdbId == null

    private fun titleYearRows(rows: List<List<String>>): Boolean = rows.all {
        it.size == 2 && it[1].trim().toIntOrNull() in VALID_FILM_YEARS
    }

    private fun column(first: List<String>, names: Set<String>, hasHeader: Boolean): Int? =
        if (hasHeader) first.indexOfFirst { header(it) in names }.takeIf { it >= 0 } else null

    private fun header(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)
}
