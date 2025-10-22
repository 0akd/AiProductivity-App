package com.arjundubey.app

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubeFullscreenScreen(videoUrl: String) {
    val videoId = extractYouTubeId(videoUrl)
    val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&controls=1&playsinline=0&rel=0"

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()

                    loadUrl(embedUrl)
                }
            },
            update = { webView ->
                webView.loadUrl(embedUrl)
            }
        )
    }
}

fun extractYouTubeId(url: String): String {
    return when {
        url.contains("youtu.be/") -> {
            url.substringAfter("youtu.be/").substringBefore("?")
        }
        url.contains("youtube.com/watch?v=") -> {
            url.substringAfter("v=").substringBefore("&")
        }
        url.contains("youtube.com/embed/") -> {
            url.substringAfter("embed/").substringBefore("?")
        }
        else -> url
    }
}