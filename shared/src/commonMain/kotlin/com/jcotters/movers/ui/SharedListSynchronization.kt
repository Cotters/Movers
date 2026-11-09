package com.jcotters.movers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cotters.data.network.NetworkMonitor
import com.cotters.data.network.NetworkStatus
import com.jcotters.contract.watchlists.data.SharedWatchlistRemote
import com.jcotters.contract.watchlists.domain.ISharedWatchlistsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun SharedListSynchronization() {
    val repository: ISharedWatchlistsRepository = koinInject()
    val network: NetworkMonitor = koinInject()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, repository) {
        if (!repository.configured) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { network.networkStatus.collect { if (it == NetworkStatus.Online) repository.refresh() } }
            launch { while (true) { repository.refresh(); delay(30_000) } }
            val remote = org.koin.mp.KoinPlatform.getKoin().getOrNull<SharedWatchlistRemote>()
            if (remote != null) launch {
                while (true) {
                    try { remote.invalidations().collect { repository.refresh() } }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { delay(5_000) }
                }
            }
        }
    }
}
