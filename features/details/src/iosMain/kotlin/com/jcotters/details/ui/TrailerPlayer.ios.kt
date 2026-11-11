@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jcotters.details.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.setValue
import platform.WebKit.WKAudiovisualMediaTypeAll
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@Composable
internal actual fun TrailerPlayer(embedUrl: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeAll
            }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
                val request = NSMutableURLRequest(uRL = checkNotNull(NSURL.URLWithString(embedUrl)))
                val appId = checkNotNull(NSBundle.mainBundle.bundleIdentifier).lowercase()
                request.setValue("https://$appId", forHTTPHeaderField = "Referer")
                loadRequest(request)
            }
        },
        onRelease = { webView ->
            webView.pauseAllMediaPlaybackWithCompletionHandler(null)
            webView.stopLoading()
            webView.loadHTMLString("", baseURL = null)
        },
    )
}
