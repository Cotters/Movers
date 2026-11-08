package com.jcotters.movers

import android.content.Context
import com.cotters.data.network.DeviceNetworkMonitor
import com.cotters.data.network.NetworkMonitor
import com.jcotters.database.createDatabase
import com.jcotters.database.databaseBuilder
import com.jcotters.internal.configureMovieApi
import com.jcotters.internal.configuredAndroidMovieToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

fun initializeAndroidMovers(context: Context) = initializeMovers(module {
    single { createDatabase(databaseBuilder(context.applicationContext), Dispatchers.IO) }
    single<NetworkMonitor> { DeviceNetworkMonitor(context.applicationContext) }
    single { HttpClient(OkHttp) { configureMovieApi(configuredAndroidMovieToken()) } }
})
