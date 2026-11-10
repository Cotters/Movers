package com.jcotters.internal.detail.data

import com.jcotters.database.movies.DbMovieGenre
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.detail.data.models.Genre
import com.jcotters.internal.detail.data.models.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieMapperShould {
    private val mapper = MovieMapper()

    @Test
    fun `map all core detail fields and filter unusable genres`() {
        val db = mapper.toDatabaseModel(MovieDto(
            id = 7, title = "Movie", overview = "Overview", releaseDate = "2026-10-07",
            posterPath = "/poster.jpg", backdropPath = "/backdrop.jpg", tagline = "Tagline",
            runtime = 125, genres = listOf(Genre(18, "Drama"), null, Genre(18, "Drama"), Genre(null, "Missing")),
            voteAverage = 7.8, voteCount = 1234, status = "Released", originalLanguage = "en",
        ), 123)!!
        val movie = mapper.toDomainModel(db)
        assertEquals(listOf(DbMovieGenre(18, "Drama")), db.genres)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", movie.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/original/poster.jpg", movie.fullSizePosterUrl)
        assertEquals("https://image.tmdb.org/t/p/original/backdrop.jpg", movie.fullSizeBackdropUrl)
        assertEquals("Tagline", movie.tagline)
        assertEquals(125, movie.runtimeMinutes)
        assertEquals("Drama", movie.genres.single().name)
        assertEquals(7.8, movie.voteAverage!!, 0.001)
        assertEquals(1234, movie.voteCount)
        assertEquals("Released", movie.status)
        assertEquals("en", movie.originalLanguage)
        assertEquals(123L, movie.detailsFetchedAt)
    }

    @Test
    fun `accept missing overview and normalize unavailable optional values`() {
        val db = mapper.toDatabaseModel(MovieDto(id = 7, title = "Movie", runtime = 0,
            tagline = " ", genres = listOf(Genre(12, "")), voteAverage = Double.NaN, voteCount = -1), 123)!!
        assertEquals("", db.synopsis)
        assertNull(db.tagline)
        assertNull(db.runtimeMinutes)
        assertTrue(db.genres!!.isEmpty())
        assertNull(db.voteAverage)
        assertNull(db.voteCount)
        assertEquals(123L, db.detailsFetchedAt)
    }

    @Test
    fun `map known catalogue rating and language without marking it enriched`() {
        val movie = mapper.toDomainModel(listOf(CatalogueMovieDto(id = 7, title = "Movie", overview = "Overview",
            originalLanguage = "fr", voteAverage = 8.1, voteCount = 50))).single()
        assertEquals("fr", movie.originalLanguage)
        assertEquals(8.1, movie.voteAverage!!, 0.001)
        assertEquals(50, movie.voteCount)
        assertNull(movie.detailsFetchedAt)
        assertNull(movie.fullSizePosterUrl)
        assertNull(movie.fullSizeBackdropUrl)
    }
}
