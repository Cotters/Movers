package com.jcotters.movers.ui

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.database.MoversDatabase
import com.jcotters.internal.configureMovieApi
import com.jcotters.movers.initializeMovers
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module

internal class MoversUiFixture(
    val database: MoversDatabase = Room.inMemoryDatabaseBuilder<MoversDatabase>(ApplicationProvider.getApplicationContext())
        .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build(),
) : AutoCloseable {
    @Volatile
    var failSearch = false
    private val client = HttpClient(MockEngine { request ->
        val body = when (request.url.encodedPath) {
            "/3/discover/movie" -> """{"page":1,"total_pages":1,"results":[$catalogueMovie]}"""
            "/3/search/movie" -> {
                if (failSearch) return@MockEngine respond("Unavailable", HttpStatusCode.ServiceUnavailable)
                val query = request.url.parameters["query"].orEmpty()
                val results = if (query == "none") "" else "$searchMovie,$anotherMovie"
                """{"page":1,"total_pages":1,"results":[$results]}"""
            }

            "/3/movie/12" -> """{"id":12,"title":"Test Movie","overview":"Loaded movie details","runtime":125,"release_date":"2026-01-01","original_language":"en","vote_average":8,"vote_count":10}"""
            "/3/movie/25" -> """{"id":25,"title":"Search Movie","overview":"Searched movie details","runtime":100,"release_date":"2026-02-01"}"""
            "/3/movie/13" -> """{"id":13,"title":"Another Movie","overview":"Another movie details","runtime":90,"release_date":"2026-03-01"}"""
            else -> error("Unexpected fixture request: ${request.url.encodedPath}")
        }
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { configureMovieApi("test-token") }

    init {
        kotlinx.coroutines.runBlocking {
            if (database.profileDao().get() == null) database.profileDao().insert(
                com.jcotters.database.profile.DeviceProfile(
                    installationId = "fixture",
                    displayName = "Josh",
                    nameConfirmed = true
                )
            )
        }
        initializeMovers(module {
            single<com.jcotters.watchlists.preferences.WatchlistPreferenceStorage> {
                object : com.jcotters.watchlists.preferences.WatchlistPreferenceStorage {
                    private var value: String? = null
                    override suspend fun read(): String? = value
                    override suspend fun write(value: String) {
                        this.value = value
                    }
                }
            }
            single { database }
            single { client }
            single<NetworkMonitor> {
                object : NetworkMonitor {
                    override val networkStatus = flowOf(NetworkStatus.Online)
                }
            }
        })
    }

    val lists: IWatchlistsRepository get() = GlobalContext.get().get()

    override fun close() {
        stopKoin(); client.close(); database.close()
    }

    private companion object {
        const val catalogueMovie =
            """{"id":12,"title":"Test Movie","overview":"Catalogue synopsis","release_date":"2026-01-01"}"""
        const val searchMovie =
            """{"id":25,"title":"Search Movie","overview":"Search synopsis","release_date":"2026-02-01"}"""
        const val anotherMovie =
            """{"id":13,"title":"Another Movie","overview":"Another synopsis","release_date":"2026-03-01"}"""
    }
}
