package com.jcotters.watchlists.documents

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.watchlists.data.FilmReference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal const val MaxImportBytes = 5 * 1024 * 1024
internal const val MaxImportEntries = 10_000

internal enum class InputFormat(val label: String, val separator: Char? = null) {
    Auto("Auto"), Lines("Newlines"), Comma("Commas", ','), Semicolon("Semicolons", ';'), Tab("Tabs", '\t'), Pipe("Pipes", '|'), Json("Movers JSON")
}
internal enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    Csv("CSV", "csv", "text/csv"), Text("Text", "txt", "text/plain"), Json("JSON", "json", "application/json")
}
internal data class ColumnMapping(val title: Int? = 0, val year: Int? = null, val movieId: Int? = null, val imdbId: Int? = null)
internal data class ParsedFilm(val reference: FilmReference, val original: String, val issue: String? = null, val unsupported: Boolean = false)
internal data class ParsedInput(val films: List<ParsedFilm>, val format: InputFormat, val headings: List<String> = emptyList(), val hasHeader: Boolean = false, val mapping: ColumnMapping = ColumnMapping())
internal data class ExportDocument(val name: String, val mimeType: String, val bytes: ByteArray)

internal object ListDocuments {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val titleHeaders = setOf("title", "name", "filmtitle", "movietitle", "filmname", "moviename", "film", "movie", "originaltitle")
    private val urlHeaders = setOf("url", "uri", "letterboxduri")
    private val yearHeaders = setOf("year", "releaseyear")
    private val movieHeaders = setOf("tmdbid", "themoviedbid", "movieid", "tmdburl")
    private val imdbHeaders = setOf("imdbid", "const", "tconst", "imdburl")
    private fun header(value: String) = value.lowercase().filter(Char::isLetterOrDigit)

    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MaxImportBytes) { "Choose a file up to 5 MB." }
        val first = bytes.getOrNull(0)?.toInt()?.and(255)
        val second = bytes.getOrNull(1)?.toInt()?.and(255)
        return if ((first == 255 && second == 254) || (first == 254 && second == 255)) {
            require(bytes.size % 2 == 0) { "Save the file as UTF-8 and retry." }
            buildString {
                for (i in 2 until bytes.size step 2) {
                    val a = bytes[i].toInt() and 255; val b = bytes[i + 1].toInt() and 255
                    append((if (first == 255) a or (b shl 8) else (a shl 8) or b).toChar())
                }
            }
        } else try { bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF") }
        catch (_: CharacterCodingException) { throw IllegalArgumentException("Save the file as UTF-8 and retry.") }
    }

    fun parse(source: String, requested: InputFormat = InputFormat.Auto, headerOverride: Boolean? = null, mappingOverride: ColumnMapping? = null): ParsedInput {
        require(source.encodeToByteArray().size <= MaxImportBytes) { "Use up to 5 MB of text." }
        val text = source.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return ParsedInput(emptyList(), InputFormat.Lines)
        val format = if (requested == InputFormat.Auto) detect(text) else requested
        if (format == InputFormat.Json) return parseJson(text)
        if (format == InputFormat.Lines) {
            return ParsedInput(limit(text.lines().map(String::trim).filter(String::isNotBlank).map(::plain)), format)
        }
        val table = readTable(text, checkNotNull(format.separator))
        val first = table.rows.firstOrNull().orEmpty()
        val knownHeader = first.any { header(it) in titleHeaders + yearHeaders + movieHeaders + imdbHeaders + urlHeaders }
        val hasHeader = headerOverride ?: knownHeader
        val headings = if (hasHeader) first else List(table.rows.maxOfOrNull { it.size } ?: 0) { "Column ${it + 1}" }
        fun index(names: Set<String>): Int? = if (hasHeader) first.indexOfFirst { header(it) in names }.takeIf { it >= 0 } else null
        val yearColumn = index(yearHeaders) ?: if (!hasHeader && table.rows.all { it.size == 2 && it[1].trim().toIntOrNull() in 1000..9999 }) 1 else null
        val mapping = mappingOverride ?: ColumnMapping(index(titleHeaders) ?: index(urlHeaders) ?: if (hasHeader && (index(movieHeaders) != null || index(imdbHeaders) != null)) null else 0, yearColumn, index(movieHeaders), index(imdbHeaders))
        val records = if (hasHeader) table.rows.drop(1) else table.rows
        val filmRows = if (!hasHeader && mappingOverride == null && yearColumn == null) {
            records.flatMap { cells -> cells.filter(String::isNotBlank).map(::plain) }
        } else records.filter { it.any(String::isNotBlank) }.map { cells ->
            fun cell(column: Int?) = column?.let { cells.getOrNull(it)?.trim() }.orEmpty()
            val title = cell(mapping.title)
            val yearText = cell(mapping.year)
            val idText = cell(mapping.movieId)
            val imdbText = cell(mapping.imdbId)
            val base = plain(title)
            val id = idText.toIntOrNull()?.takeIf { it > 0 } ?: movieId(idText)
            val imdb = imdbId(imdbText)
            val typeIndex = if (hasHeader) first.indexOfFirst { header(it) in setOf("titletype", "type") }.takeIf { it >= 0 } else null
            val unsupported = header(cell(typeIndex)) in setOf("tvseries", "tvminiseries", "tvepisode", "series", "episode")
            val year = yearText.toIntOrNull()?.takeIf { it in 1000..9999 }
            val reference = base.reference.copy(year = year ?: base.reference.year, movieId = id ?: base.reference.movieId, imdbId = imdb ?: base.reference.imdbId)
            val issue = when {
                unsupported -> "Not a film."
                yearText.isNotBlank() && year == null -> "Invalid year."
                reference.title.isBlank() && reference.movieId == null && reference.imdbId == null -> "Missing title or movie ID."
                else -> if (base.issue != null && id == null && imdb == null) base.issue else null
            }
            ParsedFilm(reference, title.ifBlank { idText.ifBlank { imdbText } }, issue, unsupported)
        }
        val unreadable = table.problem?.let { listOf(ParsedFilm(FilmReference(), it, "Unreadable CSV section. Choose a format or fix the quoted field.")) }.orEmpty()
        return ParsedInput(limit(filmRows + unreadable), format, headings, hasHeader, mapping)
    }

    private fun detect(text: String): InputFormat {
        if (text.startsWith('{')) return InputFormat.Json
        val singleColumn = readTable(text, ',')
        if (singleColumn.rows.firstOrNull()?.singleOrNull()?.let { header(it) in titleHeaders + yearHeaders + movieHeaders + imdbHeaders + urlHeaders } == true) return InputFormat.Comma
        for (format in listOf(InputFormat.Tab, InputFormat.Semicolon, InputFormat.Pipe, InputFormat.Comma)) {
            val table = readTable(text, checkNotNull(format.separator))
            val first = table.rows.firstOrNull().orEmpty()
            if (first.size > 1 && first.any { header(it) in titleHeaders + movieHeaders + imdbHeaders + urlHeaders }) return format
            if (table.rows.isNotEmpty() && table.rows.all { it.size == 2 && it[1].trim().toIntOrNull() in 1000..9999 }) return format
            if (!text.contains('\n') && !text.contains('\r') && first.size > 1) return format
        }
        return InputFormat.Lines
    }

    private data class Table(val rows: List<List<String>>, val problem: String? = null)
    private fun readTable(text: String, separator: Char): Table {
        val rows = mutableListOf<List<String>>()
        val cells = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false; var closed = false; var i = 0; var recordStart = 0
        fun field() {
            require(cells.size <= MaxImportEntries) { "Import up to 10,000 films at a time." }
            cells += field.toString(); field.clear(); closed = false
        }
        fun record() { field(); rows += cells.toList(); cells.clear(); require(rows.size <= MaxImportEntries + 1) { "Import up to 10,000 films at a time." } }
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (text.getOrNull(i + 1) == '"') { field.append('"'); i++ } else { quoted = false; closed = true }
                } else field.append(c)
            } else when {
                c == separator -> field()
                c == '\n' || c == '\r' -> { record(); if (c == '\r' && text.getOrNull(i + 1) == '\n') i++; recordStart = i + 1 }
                c == '"' && field.isEmpty() && !closed -> quoted = true
                closed && !c.isWhitespace() -> return Table(rows, text.substring(recordStart))
                c == '"' -> return Table(rows, text.substring(recordStart))
                !closed -> field.append(c)
            }
            i++
        }
        if (quoted) return Table(rows, text.substring(recordStart))
        if (field.isNotEmpty() || cells.isNotEmpty() || closed) record()
        return Table(rows)
    }

    private fun plain(value: String): ParsedFilm {
        val text = value.trim()
        val tmdb = movieId(text)
        val imdb = imdbId(text)
        val yearMatch = Regex("^(.*?)\\s*\\((\\d{4})\\)\\s*$").matchEntire(text)
        val title = if (tmdb != null || imdb != null) "" else yearMatch?.groupValues?.get(1)?.trim() ?: text
        val issue = if (title.startsWith("http://") || title.startsWith("https://")) "Use a film title or movie ID." else null
        return ParsedFilm(FilmReference(title, yearMatch?.groupValues?.get(2)?.toIntOrNull(), tmdb, imdb), text, issue)
    }
    private fun movieId(value: String): Int? = Regex("(?:https?://(?:www\\.)?themoviedb\\.org/movie/|tmdb:)(\\d+)(?:[-/?#].*)?", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim())?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    private fun imdbId(value: String): String? = Regex("(?:https?://(?:www\\.)?imdb\\.com/title/)?(tt\\d+)(?:[/\\?#].*)?", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim())?.groupValues?.get(1)?.lowercase()
    private fun limit(films: List<ParsedFilm>) = films.also { require(it.size <= MaxImportEntries) { "Import up to 10,000 films at a time." } }

    private fun parseJson(text: String): ParsedInput {
        val root = try { json.parseToJsonElement(text).jsonObject } catch (_: Exception) { throw IllegalArgumentException("Choose a Movers JSON export.") }
        require((root["format"] as? JsonPrimitive)?.contentOrNull == "movers.watchlist" && (root["version"] as? JsonPrimitive)?.intOrNull == 1) { "Choose a supported Movers JSON export." }
        val films = root["films"] as? JsonArray ?: throw IllegalArgumentException("This export has no film array.")
        require(films.size <= MaxImportEntries) { "Import up to 10,000 films at a time." }
        return ParsedInput(films.map { value ->
            try {
                val film = value.jsonObject
                val title = film["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val id = film["tmdbId"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
                val year = film["year"]?.jsonPrimitive?.intOrNull
                val issue = when {
                    year != null && year !in 1000..9999 -> "Invalid year."
                    title.isBlank() && id == null -> "Missing title or movie ID."
                    else -> null
                }
                ParsedFilm(FilmReference(title, year, id), title.ifBlank { id?.toString().orEmpty() }, issue)
            } catch (_: Exception) { ParsedFilm(FilmReference(), "Unreadable entry", "Unreadable JSON entry.") }
        }, InputFormat.Json)
    }

    fun export(name: String, movies: List<Movie>, format: ExportFormat): ExportDocument {
        val content = when (format) {
            ExportFormat.Csv -> csv(listOf("Title", "Year", "tmdbID"), movies.map { listOf(it.title, year(it)?.toString().orEmpty(), it.id.toString()) })
            ExportFormat.Text -> movies.joinToString("\n") { it.title.replace('\n', ' ').replace('\r', ' ') + (year(it)?.let { y -> " ($y)" } ?: "") }
            ExportFormat.Json -> buildJsonObject {
                put("format", "movers.watchlist"); put("version", 1); put("name", name)
                put("films", JsonArray(movies.map { movie -> buildJsonObject {
                    put("tmdbId", movie.id); put("title", movie.title); put("year", year(movie)?.let(::JsonPrimitive) ?: JsonNull)
                } }))
            }.let { json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), it) }
        }
        return document(name, format, content)
    }
    fun remaining(rows: List<Pair<ParsedFilm, String>>): ExportDocument = document("remaining-films", ExportFormat.Csv,
        csv(listOf("Title", "Year", "tmdbID", "imdbID", "Issue"), rows.map { (row, issue) ->
            listOf(row.reference.title.ifBlank { row.original }, row.reference.year?.toString().orEmpty(), row.reference.movieId?.toString().orEmpty(), row.reference.imdbId.orEmpty(), issue)
        }))
    private fun year(movie: Movie) = movie.releaseDate.take(4).toIntOrNull()?.takeIf { it in 1000..9999 }
    private fun csv(headers: List<String>, rows: List<List<String>>) = (listOf(headers) + rows).joinToString("\r\n") { row ->
        row.joinToString(",") { cell ->
            val safe = if (cell.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$cell" else cell
            "\"${safe.replace("\"", "\"\"")}\""
        }
    }
    private fun document(name: String, format: ExportFormat, content: String): ExportDocument {
        val base = name.map { if (it.isLetterOrDigit() || it in "-_ ") it else '-' }.joinToString("").trim().take(80).ifBlank { "watchlist" }
        return ExportDocument("$base.${format.extension}", format.mimeType, content.encodeToByteArray())
    }
}
