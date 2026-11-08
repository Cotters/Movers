package com.jcotters.movers

import androidx.compose.ui.window.ComposeUIViewController
import com.cotters.data.network.IosNetworkMonitor
import com.cotters.data.network.NetworkMonitor
import com.jcotters.database.createDatabase
import com.jcotters.database.databaseBuilder
import com.jcotters.internal.configureMovieApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController {
    SharedApp()
}

fun initializeIosMovers(apiToken: String) = initializeMovers(module {
    single { createDatabase(databaseBuilder(), Dispatchers.IO) }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single { HttpClient(Darwin) { configureMovieApi(apiToken) } }
})
