package com.jcotters.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import com.jcotters.designsystem.MoversTextButton

/** Fits the full artwork, retaining its cached thumbnail during loading or a failed upgrade. */
@Composable
fun ExpandedImageView(
    imageUrl: String?,
    contentDescription: String,
    placeholderKey: String,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        val description = contentDescription
        Box(modifier.semantics { this.contentDescription = description })
        return
    }
    val context = LocalPlatformContext.current
    val imageLoader = SingletonImageLoader.get(context)
    var loading by remember(imageUrl) { mutableStateOf(true) }
    var failed by remember(imageUrl) { mutableStateOf(false) }
    var retry by remember(imageUrl) { mutableIntStateOf(0) }
    val request = remember(context, imageLoader, imageUrl, placeholderKey, retry) {
        val thumbnail = imageLoader.memoryCache?.get(MemoryCache.Key(placeholderKey))?.image
        ImageRequest.Builder(context)
            .data(imageUrl)
            .placeholderMemoryCacheKey(placeholderKey)
            .placeholder(thumbnail)
            .fallback(thumbnail)
            .error(thumbnail)
            .build()
    }
    Box(modifier) {
        key(retry) {
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
                onLoading = { loading = true; failed = false },
                onSuccess = { loading = false; failed = false },
                onError = {
                    // A null URL means the owning screen is still reading its cached movie.
                    loading = imageUrl == null
                    failed = imageUrl != null
                },
            )
        }
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).semantics {
                    this.contentDescription = "Loading full-size image"
                },
            )
        }
        if (failed) {
            ExpandedImageError(
                onRetry = { loading = true; failed = false; retry++ },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ExpandedImageError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp)
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Couldn't load full-size image.", color = Color.White)
        MoversTextButton(onClick = onRetry, contentColor = Color.White) { Text("Retry") }
    }
}
