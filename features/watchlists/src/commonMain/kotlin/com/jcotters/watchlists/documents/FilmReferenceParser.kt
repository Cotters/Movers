package com.jcotters.watchlists.documents

import com.jcotters.contract.watchlists.data.FilmReference

internal const val YEAR_DIGITS = 4
internal val VALID_FILM_YEARS = 1000..9999

internal object FilmReferenceParser {
    fun parse(value: String): ParsedFilm {
        val text = value.trim()
        val tmdb = movieId(text)
        val imdb = imdbId(text)
        val yearMatch = Regex("^(.*?)\\s*\\((\\d{4})\\)\\s*$").matchEntire(text)
        val title = if (tmdb != null || imdb != null) "" else yearMatch?.groupValues?.get(1)?.trim() ?: text
        val issue =
            if (title.startsWith("http://") || title.startsWith("https://")) "Use a film title or movie ID." else null
        return ParsedFilm(FilmReference(title, yearMatch?.groupValues?.get(2)?.toIntOrNull(), tmdb, imdb), text, issue)
    }

    fun movieId(value: String): Int? =
        Regex("(?:https?://(?:www\\.)?themoviedb\\.org/movie/|tmdb:)(\\d+)(?:[-/?#].*)?", RegexOption.IGNORE_CASE)
            .matchEntire(value.trim())?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }

    fun imdbId(value: String): String? =
        Regex("(?:https?://(?:www\\.)?imdb\\.com/title/)?(tt\\d+)(?:[/\\?#].*)?", RegexOption.IGNORE_CASE)
            .matchEntire(value.trim())?.groupValues?.get(1)?.lowercase()

}
