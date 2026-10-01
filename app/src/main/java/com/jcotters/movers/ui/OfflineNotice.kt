package com.jcotters.movers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cotters.data.network.NetworkStatus
import com.jcotters.movers.R

/** A single status notice for all destinations, including startup and authentication. */
@Composable
internal fun OfflineNotice(status: NetworkStatus, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = status == NetworkStatus.Offline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Snackbar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(stringResource(R.string.offline_message))
        }
    }
}
