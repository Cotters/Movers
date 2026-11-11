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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cotters.data.network.NetworkStatus
import com.jcotters.movers.resources.Res
import com.jcotters.movers.resources.offline_message
import org.jetbrains.compose.resources.stringResource

/** A single status notice for all destinations, including startup and authentication. */
@Composable
internal fun OfflineNotice(status: NetworkStatus, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = status == NetworkStatus.Offline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        com.jcotters.designsystem.MoversNotice(
            message = stringResource(Res.string.offline_message),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
