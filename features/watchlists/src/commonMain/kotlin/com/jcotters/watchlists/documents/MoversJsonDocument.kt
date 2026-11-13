package com.jcotters.watchlists.documents

import com.jcotters.contract.watchlists.data.FilmReference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val documentJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

internal object MoversJsonDocument {
    fun parse(text: String): ParsedInput {
        val root = try {
            documentJson.parseToJsonElement(text).jsonObject
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Choose a Movers JSON export.", error)
        }
        val format = (root["format"] as? JsonPrimitive)?.contentOrNull
        val version = (root["version"] as? JsonPrimitive)?.intOrNull
        require(format == "movers.watchlist" && version == 1) { "Choose a supported Movers JSON export." }
        val films = root["films"] as? JsonArray ?: throw IllegalArgumentException("This export has no film array.")
        require(films.size <= MAX_IMPORT_ENTRIES) { "Import up to 10,000 films at a time." }
        return ParsedInput(films.map { value ->
            try {
                val film = value.jsonObject
                val title = film["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val id = film["tmdbId"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
                val year = film["year"]?.jsonPrimitive?.intOrNull
                val issue = when {
                    year != null && year !in VALID_FILM_YEARS -> "Invalid year."
                    title.isBlank() && id == null -> "Missing title or movie ID."
                    else -> null
                }
                ParsedFilm(FilmReference(title, year, id), title.ifBlank { id?.toString().orEmpty() }, issue)
            } catch (_: IllegalArgumentException) {
                ParsedFilm(FilmReference(), "Unreadable entry", "Unreadable JSON entry.")
            }
        }, InputFormat.Json)
    }
}
