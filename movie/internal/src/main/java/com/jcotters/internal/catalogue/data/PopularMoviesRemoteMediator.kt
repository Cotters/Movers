package com.jcotters.internal.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueRemoteKey
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.internal.MovieApi
import com.jcotters.internal.detail.data.MovieMapper
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
internal class PopularMoviesRemoteMediator @Inject constructor(
    private val movieApi: MovieApi,
    private val database: MoversDatabase,
    private val movieMapper: MovieMapper,
    @param:CatalogueClock private val clock: Clock,
) : RemoteMediator<Int, DbMovie>() {

    private val movieDao = database.movieDao()
    private val movieCatalogueDao = database.movieCatalogueDao()
    private val remoteKeysDao = database.movieCatalogueRemoteKeysDao()

    private companion object {
        const val START_PAGE = 1
        val CACHE_TTL_MILLIS = Duration.ofHours(6).toMillis()
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

            database.withTransaction {
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
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun removeCatalogueData() {
        movieCatalogueDao.removeAllForCatalogue(catalogue)
        remoteKeysDao.removeCatalogueKeys(catalogue)
    }
}
