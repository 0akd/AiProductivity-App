// Fixed missing parameter 'onClick' in WebsiteCard call
package com.arjundubey.app

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Data class for website information
data class WebsiteInfo(val title: String, val url: String, val description: String)

@Composable
fun WebsiteCardsView(
    websites: List<WebsiteInfo>,
    modifier: Modifier = Modifier,
    onPageLoaded: ((String?) -> Unit)? = null,
    onError: ((String?) -> Unit)? = null
) {
    var selectedWebsite by remember { mutableStateOf<WebsiteInfo?>(null) }

    Column(modifier = modifier) {
        Text(
            text = "Websites",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(websites) { website ->
                WebsiteCard(website = website, onClick = { selectedWebsite = website })
            }
        }

        selectedWebsite?.let { website ->
            PopupWebView(
                url = website.url,
                title = website.title,
                onDismiss = { selectedWebsite = null },
                onPageLoaded = onPageLoaded,
                onError = onError
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteCard(website: WebsiteInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = website.title.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = website.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = website.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = website.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun PopupWebView(
    url: String,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPageLoaded: ((String?) -> Unit)? = null,
    onError: ((String?) -> Unit)? = null
) {
    var isFullscreen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(if (isFullscreen) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (isFullscreen) 1f else 0.85f),
                shape = RoundedCornerShape(if (isFullscreen) 0.dp else 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Fullscreen toggle
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                imageVector = if (isFullscreen)
                                    Icons.Default.FullscreenExit
                                else
                                    Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider()

                    // WebView
                    SimpleWebView(
                        url = url,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onPageLoaded = onPageLoaded,
                        onError = onError
                    )
                }
            }
        }
    }
}



@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleWebView(
    url: String,
    modifier: Modifier = Modifier,
    onPageLoaded: ((String?) -> Unit)? = null,
    onError: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setupWebView(onPageLoaded, onError)
                loadUrl(url)
            }
        },
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onPageLoaded: ((String?) -> Unit)? = null,
    onError: ((String?) -> Unit)? = null
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        cacheMode = WebSettings.LOAD_DEFAULT
        allowFileAccess = true
        allowContentAccess = true
        loadsImagesAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }
        userAgentString = WebSettings.getDefaultUserAgent(context)
    }

    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageLoaded?.invoke(url)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                onError?.invoke(error?.description?.toString())
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            onError?.invoke(description)
        }
    }

    webChromeClient = object : WebChromeClient() {}
}

@Composable
fun ExampleUsage() {
    val sampleWebsites = listOf(
        WebsiteInfo("Google", "https://www.google.com", "Search the world's information."),
        WebsiteInfo("GitHub", "https://www.github.com", "Code hosting platform."),
        WebsiteInfo("Stack Overflow", "https://stackoverflow.com", "Q&A for developers."),
        WebsiteInfo("Medium", "https://medium.com", "Read and write stories."),
        WebsiteInfo("Reddit", "https://www.reddit.com", "Online communities.")
    )

    WebsiteCardsView(
        websites = sampleWebsites,
        onPageLoaded = { println("Page loaded: $it") },
        onError = { println("Error: $it") }
    )
}
