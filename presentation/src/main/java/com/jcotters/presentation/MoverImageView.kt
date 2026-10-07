package com.jcotters.presentation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest

fun moviePosterKey(movieId: Int): String = "movie-poster-$movieId"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MoverImageView(
    posterUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    MoverImageView(
        posterUrl = posterUrl,
        contentDescription = contentDescription,
        modifier = modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = posterUrl),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    )
}


@Composable
fun MoverImageView(
    posterUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sharedImageKey: String? = null,
) {
    val context = LocalContext.current
    val imageLoader = SingletonImageLoader.get(context)
    val request = remember(context, imageLoader, posterUrl, sharedImageKey) {
        val cachedImage = sharedImageKey?.let { imageLoader.memoryCache?.get(MemoryCache.Key(it))?.image }
        ImageRequest.Builder(context)
            .data(posterUrl.takeIf { it.isNotBlank() })
            .placeholderMemoryCacheKey(sharedImageKey)
            .placeholder(cachedImage)
            // Until Room supplies the URL, null data uses a fallback rather than a placeholder.
            .fallback(cachedImage)
            .build()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            onSuccess = { state ->
                sharedImageKey?.let { key ->
                    // Alias only for the transition; normal URL-based cache keys stay intact.
                    imageLoader.memoryCache?.set(MemoryCache.Key(key), MemoryCache.Value(state.result.image))
                }
            },
        )
    }
}
