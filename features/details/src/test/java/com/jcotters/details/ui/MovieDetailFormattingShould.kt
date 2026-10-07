package com.jcotters.details.ui

import com.jcotters.contract.detail.domain.models.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDetailFormattingShould {
    @Test
    fun `format runtime without empty hours or minutes`() {
        assertEquals("45 min", formatRuntime(45))
        assertEquals("2h", formatRuntime(120))
        assertEquals("2h 5m", formatRuntime(125))
    }

    @Test
    fun `distinguish an unrated movie from an unknown rating`() {
        assertEquals("Not yet rated", formatRating(Movie(7, "Movie", "", "", voteAverage = 0.0, voteCount = 0)))
        assertNull(formatRating(Movie(7, "Movie", "", "")))
    }
}
