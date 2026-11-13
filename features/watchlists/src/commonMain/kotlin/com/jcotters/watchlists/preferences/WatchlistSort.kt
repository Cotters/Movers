package com.jcotters.watchlists.preferences

import com.jcotters.contract.detail.domain.models.Movie

private const val MAX_MOVIE_RATING = 10.0
private const val RELEASE_YEAR_LENGTH = 4

private val releaseDatePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

internal enum class WatchlistSortField(val label: String) {
    Added("Recently added"), ReleaseDate("Release date"), Title("Alphabetical"), Rating("Rating"),
}

internal data class WatchlistSort(
    val field: WatchlistSortField = WatchlistSortField.Added,
    val ascending: Boolean = false
) {
    fun apply(movies: List<Movie>): List<Movie> {
        if (field == WatchlistSortField.Added) return if (ascending) movies.asReversed() else movies
        val titles = movies.associate { it.id to it.title.trim().lowercase().ifBlank { null } }
        val dates =
            if (field == WatchlistSortField.ReleaseDate) movies.associate { it.id to releaseDate(it) } else emptyMap()
        val ratings = if (field == WatchlistSortField.Rating) movies.associate { it.id to rating(it) } else emptyMap()
        return movies.sortedWith { first, second ->
            val comparison = when (field) {
                WatchlistSortField.ReleaseDate -> compareValues(dates[first.id], dates[second.id])
                WatchlistSortField.Title -> compareValues(titles[first.id], titles[second.id])
                WatchlistSortField.Rating -> compareValues(ratings[first.id], ratings[second.id])
                WatchlistSortField.Added -> 0
            }
            // Equal values use a deterministic title/id tie-breaker in either direction.
            comparison.takeIf { it != 0 } ?: compareValuesBy(first, second, { it.title.lowercase() }, { it.id })
        }
    }

    private fun <T : Comparable<T>> compareValues(first: T?, second: T?): Int = when {
        first == null -> if (second == null) 0 else 1
        second == null -> -1
        ascending -> first.compareTo(second)
        else -> second.compareTo(first)
    }

    private fun rating(movie: Movie): Double? = movie.voteAverage?.takeIf {
        it.isFinite() && it in 0.0..MAX_MOVIE_RATING && movie.voteCount != 0
    }

    private fun releaseDate(movie: Movie): String? {
        val date = movie.releaseDate
        if (!date.matches(releaseDatePattern)) return null
        val year = date.take(RELEASE_YEAR_LENGTH).toInt()
        val month = date.substring(5, 7).toInt()
        val day = date.takeLast(2).toInt()
        if (year == 0 || month !in 1..12) return null
        val leapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val days = when (month) {
            2 -> if (leapYear) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31
        }
        return date.takeIf { day in 1..days }
    }
}
