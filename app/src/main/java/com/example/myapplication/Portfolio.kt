package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Data class for website information
data class WebsiteInfo(
    val title: String,
    val url: String,
    val description: String,
    val iconRes: Int? = null // Optional icon resource
)

@Composable
fun WebsiteCardsView(
    websites: List<WebsiteInfo>,
    modifier: Modifier = Modifier,
    onPageLoaded: ((String?) -> Unit)? = null,
    onError: ((String?) -> Unit)? = null
) {
    var selectedWebsite by remember { mutableStateOf<WebsiteInfo?>(null) }

    Column(modifier = modifier) {
        // Header
        Text(
            text = "Websites",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Website cards grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(websites) { website ->
                WebsiteCard(
                    website = website,
                    onClick = { selectedWebsite = website }
                )
            }
        }

        // Show popup when website is selected
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
fun WebsiteCard(
    website: WebsiteInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder (you can replace with actual icons)
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

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
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

            // Arrow icon
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                contentDescription = "Open website",
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .clickable(enabled = false) { }, // Prevent click propagation
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    // WebView content
                    SimpleWebView(
                        url = url,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
    modifier: Modifier = Modifier.fillMaxSize(),
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
    // Basic WebView settings - minimal and safe
    settings.apply {
        // Essential settings
        javaScriptEnabled = true
        domStorageEnabled = true

        // Viewport settings for proper display
        useWideViewPort = true
        loadWithOverviewMode = true

        // Basic zoom settings
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false

        // Performance settings
        cacheMode = WebSettings.LOAD_DEFAULT

        // Content settings
        allowFileAccess = true
        allowContentAccess = true
        loadsImagesAutomatically = true

        // Modern security features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }

        // User agent - use default browser user agent
        userAgentString = WebSettings.getDefaultUserAgent(context)
    }

    // Set up WebViewClient - minimal intervention
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageLoaded?.invoke(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            // Let WebView handle all URLs normally
            return false
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
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
            super.onReceivedError(view, errorCode, description, failingUrl)
            onError?.invoke(description)
        }
    }

    // Set up WebChromeClient for popup support
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
        }

        // Support for popup windows
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        // Support for JavaScript alerts
        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            return super.onJsAlert(view, url, message, result)
        }
    }
}

// Usage example:
@Composable
fun ExampleUsage() {
    val sampleWebsites = listOf(
        WebsiteInfo(
            title = "Google",
            url = "https://www.google.com",
            description = "Search the world's information, including webpages, images, videos and more."
        ),
        WebsiteInfo(
            title = "GitHub",
            url = "https://www.github.com",
            description = "GitHub is where over 100 million developers shape the future of software, together."
        ),
        WebsiteInfo(
            title = "Stack Overflow",
            url = "https://stackoverflow.com",
            description = "Stack Overflow is the largest, most trusted online community for developers to learn and share knowledge."
        ),
        WebsiteInfo(
            title = "Medium",
            url = "https://medium.com",
            description = "Medium is an open platform where readers find dynamic thinking, and where expert voices are heard."
        ),
        WebsiteInfo(
            title = "Reddit",
            url = "https://www.reddit.com",
            description = "Reddit is a network of communities where people can dive into their interests, hobbies and passions."
        )
    )

    WebsiteCardsView(
        websites = sampleWebsites,
        onPageLoaded = { url ->
            println("Page loaded: $url")
        },
        onError = { error ->
            println("Error: $error")
        }
    )
}