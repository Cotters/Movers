package com.jcotters.watchlists

import com.jcotters.contract.detail.domain.models.Movie
import kotlin.random.Random

/** A linear rating bias; missing ratings get a neutral weight and every film remains eligible. */
internal fun pickWatchlistMovie(movies: List<Movie>, random: Random = Random.Default): Movie? {
    if (movies.isEmpty()) return null
    val weights = movies.map { movie ->
        movie.voteAverage?.takeIf { it.isFinite() && it > 0 && movie.voteCount != 0 }
            ?.coerceIn(1.0, 10.0) ?: 5.0
    }
    var remaining = random.nextDouble(weights.sum())
    movies.forEachIndexed { index, movie ->
        remaining -= weights[index]
        if (remaining < 0) return movie
    }
    return movies.last()
}
