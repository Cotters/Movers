package com.jcotters.database.movies

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieGenreConvertersShould {
    private val converter = MovieGenreConverters()

    @Test
    fun `round trip names containing quotes unicode and commas`() {
        val genres = listOf(DbMovieGenre(18, "Drama, \"Mystery\""), DbMovieGenre(12, "冒険"))
        assertEquals(genres, converter.decode(converter.encode(genres)))
    }

    @Test
    fun `preserve empty and unknown genres separately`() {
        assertNull(converter.decode(converter.encode(null)))
        assertEquals(emptyList<DbMovieGenre>(), converter.decode(converter.encode(emptyList())))
    }
}
