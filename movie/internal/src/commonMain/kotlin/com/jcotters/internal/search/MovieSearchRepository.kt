package com.jcotters.internal.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.search.domain.IMovieSearchRepository
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.MovieMapper
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

internal class MovieSearchRepository(
    private val api: MovieApi,
    private val dao: MovieDao,
    private val mapper: MovieMapper
) : IMovieSearchRepository {
    override fun searchMovies(query: String) = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        MovieSearchPagingSource(query.trim(), api, dao, mapper)
    }.flow
}

internal class MovieSearchPagingSource(
    private val query: String,
    private val api: MovieApi,
    private val dao: MovieDao,
    private val mapper: MovieMapper,
) : PagingSource<Int, Movie>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> =
        recoverableResult<LoadResult<Int, Movie>> {
            if (query.isBlank()) {
                LoadResult.Page(emptyList(), null, null)
            } else {
                val page = params.key ?: 1
                val response = api.searchMovies(query, page)
                require(response.page == page) { "Search returned an unexpected page." }
                val movies = mapper.toDatabaseModel(response.results.orEmpty())
                // Search summaries must be saved before quick-add; preserve enriched movie fields.
                dao.upsertCatalogueMovies(movies)
                val endReached = response.results.isNullOrEmpty() || response.totalPages?.let { page >= it } == true
                LoadResult.Page(
                    movies.map(mapper::toDomainModel),
                    if (page > 1) page - 1 else null,
                    if (endReached) null else page + 1
                )
            }
        }.getOrElse { error ->
            val message =
                if (error is ClientRequestException && error.response.status == HttpStatusCode.TooManyRequests) {
                    "Search is temporarily rate-limited. Please try again shortly."
                } else "Unable to search for movies. Check your connection and retry."
            LoadResult.Error(IllegalStateException(message, error))
        }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = state.anchorPosition?.let { anchor ->
        state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
    }
}
