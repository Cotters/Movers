package com.jcotters.internal.search

import androidx.paging.PagingSource
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class MovieSearchPagingSourceTest {
    private val api = mockk<MovieApi>()
    private val dao = mockk<MovieDao>(relaxed = true)

    @Test
    fun emptyQueryDoesNotCallServiceOrWriteCache() = runTest {
        val result = source("").load(PagingSource.LoadParams.Refresh(null, 20, false))
        assertTrue(result is PagingSource.LoadResult.Page && result.data.isEmpty())
        coVerify(exactly = 0) { api.searchMovies(any(), any()) }
        coVerify(exactly = 0) { dao.upsertCatalogueMovies(any()) }
    }

    @Test
    fun pagesSaveSummariesAndStopAtLastPage() = runTest {
        val movie = CatalogueMovieDto(id = 12, title = "Movie", overview = "Synopsis", releaseDate = "2026-01-01")
        coEvery { api.searchMovies("movie", 1) } returns CataloguePageResponse(1, listOf(movie), 2)
        coEvery { api.searchMovies("movie", 2) } returns CataloguePageResponse(2, listOf(movie.copy(id = 13)), 2)
        val source = source("movie")
        val first = source.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
        val second = source.load(PagingSource.LoadParams.Append(2, 20, false)) as PagingSource.LoadResult.Page
        assertEquals(listOf(12), first.data.map { it.id })
        assertEquals(2, first.nextKey)
        assertNull(first.prevKey)
        assertEquals(1, second.prevKey)
        assertNull(second.nextKey)
        coVerify(exactly = 2) { dao.upsertCatalogueMovies(any()) }
    }

    @Test
    fun failureIsRetryableAndCancellationPropagates() = runTest {
        coEvery { api.searchMovies("movie", 1) } throws IllegalStateException("offline")
        val source = source("movie")
        assertTrue(source.load(PagingSource.LoadParams.Refresh(null, 20, false)) is PagingSource.LoadResult.Error)
        coVerify(exactly = 0) { dao.upsertCatalogueMovies(any()) }
        coEvery { api.searchMovies("movie", 1) } throws CancellationException("cancelled")
        assertFailsWith<CancellationException> { source.load(PagingSource.LoadParams.Refresh(null, 20, false)) }
    }

    private fun source(query: String) = MovieSearchPagingSource(query, api, dao, MovieMapper())
}
