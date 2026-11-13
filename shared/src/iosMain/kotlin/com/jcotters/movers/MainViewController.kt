package com.jcotters.movers

import androidx.compose.ui.uikit.OnFocusBehavior
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
import org.koin.dsl.onClose

fun MainViewController() = ComposeUIViewController(configure = {
    // Shared content handles IME insets; panning this view would also move bottom navigation.
    onFocusBehavior = OnFocusBehavior.DoNothing
}) {
    SharedApp()
}

fun initializeIosMovers(apiToken: String, backendUrl: String = "", backendKey: String = "") = initializeMovers(module {
    val backend = com.jcotters.backend.supabase.createIosBackend(backendUrl, backendKey)
    single<com.jcotters.profile.domain.ParticipantIdentity> { backend }
    single<com.jcotters.contract.watchlists.data.SharedWatchlistRemote> { backend }
    single<com.jcotters.contract.watchlists.data.PushRegistrations> { backend }
    single<com.jcotters.movers.notifications.NativeNotifications> {
        com.jcotters.movers.notifications.IosNotifications
    }
    single<com.jcotters.profile.domain.Notifications> {
        com.jcotters.movers.notifications.WatchlistNotifications(get(), get(), get())
    } onClose { (it as? com.jcotters.movers.notifications.WatchlistNotifications)?.close() }
    single<com.jcotters.contract.watchlists.domain.InvitationSharing> { IosInvitationSharing() }
    single<com.jcotters.watchlists.preferences.WatchlistPreferenceStorage> {
        com.jcotters.watchlists.preferences.IosWatchlistPreferenceStorage()
    }
    single { createDatabase(databaseBuilder(), Dispatchers.IO) }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single { HttpClient(Darwin) { configureMovieApi(apiToken) } }
})
