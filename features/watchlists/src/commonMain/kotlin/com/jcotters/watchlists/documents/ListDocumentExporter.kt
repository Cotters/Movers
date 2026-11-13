package com.jcotters.watchlists.documents

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MAX_FILENAME_LENGTH = 80

internal object ListDocumentExporter {
    fun export(name: String, movies: List<Movie>, format: ExportFormat): ExportDocument {
        val content = when (format) {
            ExportFormat.Csv -> csv(
                listOf("Title", "Year", "tmdbID"),
                movies.map { listOf(it.title, year(it)?.toString().orEmpty(), it.id.toString()) })

            ExportFormat.Text -> movies.joinToString("\n") {
                it.title.replace('\n', ' ').replace('\r', ' ') + (year(it)?.let { y -> " ($y)" } ?: "")
            }

            ExportFormat.Json -> buildJsonObject {
                put("format", "movers.watchlist")
                put("version", 1)
                put("name", name)
                put("films", JsonArray(movies.map { movie ->
                    buildJsonObject {
                        put("tmdbId", movie.id)
                        put("title", movie.title)
                        put("year", year(movie)?.let(::JsonPrimitive) ?: JsonNull)
                    }
                }))
            }.let { documentJson.encodeToString(JsonObject.serializer(), it) }
        }
        return document(name, format, content)
    }

    fun remaining(rows: List<Pair<ParsedFilm, String>>): ExportDocument = document(
        "remaining-films", ExportFormat.Csv,
        csv(listOf("Title", "Year", "tmdbID", "imdbID", "Issue"), rows.map { (row, issue) ->
            listOf(
                row.reference.title.ifBlank { row.original },
                row.reference.year?.toString().orEmpty(),
                row.reference.movieId?.toString().orEmpty(),
                row.reference.imdbId.orEmpty(),
                issue
            )
        })
    )

    private fun year(movie: Movie) =
        movie.releaseDate.take(YEAR_DIGITS).toIntOrNull()?.takeIf { it in VALID_FILM_YEARS }

    private fun csv(headers: List<String>, rows: List<List<String>>) =
        (listOf(headers) + rows).joinToString("\r\n") { row ->
            row.joinToString(",") { cell ->
                val safe = if (cell.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$cell" else cell
                "\"${safe.replace("\"", "\"\"")}\""
            }
        }

    private fun document(name: String, format: ExportFormat, content: String): ExportDocument {
        val base = name.map { if (it.isLetterOrDigit() || it in "-_ ") it else '-' }.joinToString("").trim()
            .take(MAX_FILENAME_LENGTH)
            .ifBlank { "watchlist" }
        return ExportDocument("$base.${format.extension}", format.mimeType, content.encodeToByteArray())
    }
}
