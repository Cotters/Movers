package com.jcotters.internal.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.jcotters.contract.time.AppClock
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueRemoteKey
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.MovieMapper
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
internal class PopularMoviesRemoteMediator(
    private val movieApi: MovieApi,
    private val database: MoversDatabase,
    private val movieMapper: MovieMapper,
    private val clock: AppClock,
) : RemoteMediator<Int, DbMovie>() {

    private val movieDao = database.movieDao()
    private val movieCatalogueDao = database.movieCatalogueDao()
    private val remoteKeysDao = database.movieCatalogueRemoteKeysDao()

    private companion object {
        const val START_PAGE = 1
        val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
        val catalogue = MovieCatalogue.Popular.key
    }

    override suspend fun initialize(): InitializeAction {
        val lastRefreshedAt = remoteKeysDao.getRemoteKey(catalogue)?.lastRefreshedAt
            ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        val now = clock.millis()
        return if (lastRefreshedAt <= now && now - lastRefreshedAt < CACHE_TTL_MILLIS) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, DbMovie>
    ): MediatorResult {
        return try {
            val page: Int = when (loadType) {
                LoadType.REFRESH -> START_PAGE
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    remoteKeysDao.getRemoteKey(catalogue)?.nextPage
                        ?: return MediatorResult.Success(
                            endOfPaginationReached = true
                        )
                }
            }

            val response = movieApi.getPopularMovies(page = page)
            val movies = response.results.orEmpty().filterNotNull()
            val responsePage = requireNotNull(response.page) {
                "Popular movies response omitted page"
            }
            require(responsePage == page) {
                "Requested page $page but received page $responsePage"
            }
            val endOfPaginationReached = movies.isEmpty() || response.totalPages?.let { responsePage >= it } == true

            database.useWriterConnection { connection -> connection.immediateTransaction {
                val lastRefreshedAt = if (loadType == LoadType.REFRESH) {
                    clock.millis()
                } else {
                    remoteKeysDao.getRemoteKey(catalogue)?.lastRefreshedAt
                }
                if (loadType == LoadType.REFRESH) {
                    removeCatalogueData()
                }
                val dbMovies = movieMapper.toDatabaseModel(movies = movies)
                movieDao.upsertCatalogueMovies(dbMovies)
                val catalogueEntries = movieMapper.toCatalogueEntry(catalogue, dbMovies, responsePage)
                movieCatalogueDao.insertCatalogueEntries(catalogueEntries)
                val nextPage = if (endOfPaginationReached) null else responsePage + 1
                remoteKeysDao.insertKey(DbMovieCatalogueRemoteKey(catalogue, nextPage, lastRefreshedAt))
            } }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun removeCatalogueData() {
        movieCatalogueDao.removeAllForCatalogue(catalogue)
        remoteKeysDao.removeCatalogueKeys(catalogue)
    }
}
