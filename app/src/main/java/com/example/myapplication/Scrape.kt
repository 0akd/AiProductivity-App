package com.example.myapplication
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

data class ScrapedContent(
    val title: String,
    val description: String,
    val additionalInfo: String,
    val sourceUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScraperScreen() {

        var searchQuery by remember { mutableStateOf("") }
        val context = LocalContext.current
        val urlList = listOf(
            "https://devpost.com/hackathons?challenge_type[]=online&page=4&status[]=open",
            "https://unstop.com/hackathons?page=1",
            "https://devpost.com/hackathons?challenge_type[]=online&status[]=upcoming",
            "https://devfolio.co/search?primary_filter=hackathons&type=application_open",
            "https://devpost.com/hackathons?challenge_type[]=in-person&page=2&status[]=open",
            "https://devpost.com/hackathons?challenge_type[]=in-person&page=3&status[]=upcoming",
            "https://www.hackerearth.com/challenges/hackathon/"
        )
        val listState = rememberLazyListState()
        var allContent by remember { mutableStateOf<List<ScrapedContent>>(emptyList()) }
        var displayedContent by remember { mutableStateOf<List<ScrapedContent>>(emptyList()) }
        var currentIndex by remember { mutableStateOf(0) }
        var isProcessing by remember { mutableStateOf(false) }
        var webView: WebView? = remember { null }
        var isCompleted by remember { mutableStateOf(false) }

        // Filter content based on search query
        val searchFilteredContent = remember(displayedContent, searchQuery) {
            val uniqueContent = displayedContent.distinctBy { "${it.title}_${it.sourceUrl}" }
            if (searchQuery.isEmpty()) {
                uniqueContent
            } else {
                uniqueContent.filter { content ->
                    val searchLower = searchQuery.lowercase()
                    val titleMatches = content.title.lowercase().contains(searchLower)
                    val descMatches = content.description.lowercase().contains(searchLower)
                    val infoMatches = content.additionalInfo.lowercase().contains(searchLower)
                    titleMatches || descMatches || infoMatches
                }
            }
        }

        // Animation for loading progress
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val loadingProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )

        // Stream cards one by one
        LaunchedEffect(allContent.size) {
            if (allContent.size > displayedContent.size) {
                val newItems = allContent.drop(displayedContent.size)
                for (item in newItems) {
                    delay(100)
                    displayedContent = displayedContent + item
                }
            }
        }

        // Function to load next URL
        fun loadNextUrl() {
            if (currentIndex < urlList.size - 1) {
                currentIndex++
                webView?.postDelayed({
                    webView?.loadUrl(urlList[currentIndex])
                }, 3000)
            } else {
                isCompleted = true
                isProcessing = false
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {

            // Heading Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()

                .zIndex(3f),

                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀 Hackathon Hunter",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Discovering amazing hackathons across the web",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Loading Status or Search Bar
                    if (isCompleted) {
                        // Search Bar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🔍 Search Hackathons",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search by title, description, or info...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val totalCount =
                                    displayedContent.distinctBy { "${it.title}_${it.sourceUrl}" }.size
                                Text(
                                    text = if (searchQuery.isEmpty()) {
                                        "Showing all $totalCount hackathons"
                                    } else {
                                        "Found ${searchFilteredContent.size} hackathons matching \"$searchQuery\""
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        // Loading status
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(

                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scanning ${currentIndex + 1}/${urlList.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "${allContent.size}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hidden WebView for scraping
            AndroidView(
                factory = {
                    WebView(context).apply {
                        webView = this

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                        }
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.flush()
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                // Prevent multiple simultaneous processing
                                if (isProcessing) return
                                isProcessing = true

                                val maxScrolls = 15
                                var scrollCount = 0
                                var previousHeight = 0
                                var stableHeightCount =
                                    0 // Count how many times height remained same

                                fun scrollAndScrape() {
                                    if (scrollCount >= maxScrolls || stableHeightCount >= 3) {
                                        // Finished with current URL, move to next
                                        isProcessing = false
                                        loadNextUrl()
                                        return
                                    }

                                    view?.evaluateJavascript(
                                        """
                                    (function(){
                                        const height = document.body.scrollHeight;
                                        window.scrollTo(0, height);
                                        return height;
                                    })();
                                    """.trimIndent()
                                    ) { heightResult ->
                                        val newHeight = heightResult.toIntOrNull() ?: 0

                                        if (newHeight == previousHeight) {
                                            stableHeightCount++
                                            if (stableHeightCount >= 3) {
                                                // Height stable for 3 attempts, move to next URL
                                                isProcessing = false
                                                loadNextUrl()
                                                return@evaluateJavascript
                                            }
                                        } else {
                                            stableHeightCount = 0 // Reset counter if height changed
                                        }

                                        previousHeight = newHeight

                                        view?.postDelayed({
                                            view?.evaluateJavascript(
                                                """
    (function() {
        const headingSelectors = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', '[class*="title"]', '[class*="heading"]', '[class*="name"]'];
        let contentItems = [];
        
        // Define blocked keywords for filtering
        const blockedKeywords = [
            'hackathons', 'filter', 'end', 'clubnapa', 'publicinvite', 'linkedin', 'devpost',
            'insights', 'stories', 'recordings', 'documentation', 'filters', 'live challenges',
            'upcoming challenges', 'previous challenges', 'knowledge', 'contact', 'notifications',
            'log in', 'sign up', 'requirements', 'following', 'available', 'please'
        ];
        
        // Function to check if content should be filtered out
        function shouldFilter(title, description, additionalInfo) {
            const combinedText = (title + ' ' + description + ' ' + additionalInfo).toLowerCase();
            return blockedKeywords.some(keyword => combinedText.includes(keyword.toLowerCase()));
        }
        
        headingSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(heading => {
                const title = heading.innerText.trim();
                if (title.length > 2) {
                    let description = '';
                    let additionalInfo = '';
                    
                    // Method 1: Look for immediate next sibling paragraph
                    let nextElement = heading.nextElementSibling;
                    let attempts = 0;
                    while (nextElement && attempts < 5) {
                        if (nextElement.tagName === 'P') {
                            description = nextElement.innerText.trim();
                            
                            // Look for additional info after the paragraph
                            let nextAfterP = nextElement.nextElementSibling;
                            if (nextAfterP) {
                                const nextText = nextAfterP.innerText.trim();
                                if (nextText.match(/\d+/) || nextText.match(/\$/) || nextText.match(/₹/) || nextText.match(/\d{4}/) || nextText.length < 50) {
                                    additionalInfo = nextText;
                                }
                            }
                            break;
                        } else if (nextElement.tagName && nextElement.innerText.trim().length > 5 && nextElement.innerText.trim().length < 200) {
                            description = nextElement.innerText.trim();
                            break;
                        }
                        nextElement = nextElement.nextElementSibling;
                        attempts++;
                    }
                    
                    // Method 2: Look in parent container for paragraphs
                    if (!description) {
                        const parent = heading.parentElement;
                        if (parent) {
                            // Look for p tags in parent
                            const paragraphs = parent.querySelectorAll('p');
                            paragraphs.forEach(p => {
                                const pText = p.innerText.trim();
                                if (pText.length > 5 && pText.length < 300 && !description) {
                                    description = pText;
                                }
                            });
                            
                            // Also check for spans or divs that might contain description
                            if (!description) {
                                const spans = parent.querySelectorAll('span, div');
                                spans.forEach(span => {
                                    const spanText = span.innerText.trim();
                                    if (spanText.length > 10 && spanText.length < 200 && !description && spanText !== title) {
                                        description = spanText;
                                    }
                                });
                            }
                        }
                    }
                    
                    // Method 3: Look in grandparent container
                    if (!description) {
                        const grandParent = heading.parentElement?.parentElement;
                        if (grandParent) {
                            const allParagraphs = grandParent.querySelectorAll('p');
                            allParagraphs.forEach(p => {
                                const pText = p.innerText.trim();
                                if (pText.length > 5 && pText.length < 300 && !description && pText !== title) {
                                    description = pText;
                                }
                            });
                        }
                    }
                    
                    // Method 4: Look for card/item containers
                    let cardContainer = heading.closest('[class*="card"], [class*="item"], [class*="event"], [class*="hackathon"], [class*="competition"]');
                    if (!description && cardContainer) {
                        const cardParagraphs = cardContainer.querySelectorAll('p');
                        cardParagraphs.forEach(p => {
                            const pText = p.innerText.trim();
                            if (pText.length > 5 && pText.length < 300 && !description && pText !== title) {
                                description = pText;
                            }
                        });
                    }
                    
                    // Extract additional info from the entire container
                    if (!additionalInfo) {
                        const containerText = cardContainer ? cardContainer.innerText : (heading.parentElement ? heading.parentElement.innerText : '');
                        const patterns = [
                            /(\d+[^\w\s]*\s*(?:participants|entries|days|hours|minutes|₹|\$|prizes?|winners?|deadline|teams?))/i,
                            /(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})/,
                            /(₹\s*\d+[,\d]*|$\s*\d+[,\d]*)/,
                            /(\d+\s*(?:days?|hours?|minutes?)\s*(?:left|remaining))/i,
                            /(deadline|due|ends?|closes?)\s*:?\s*([^\n]{1,50})/i
                        ];
                        
                        for (let pattern of patterns) {
                            const match = containerText.match(pattern);
                            if (match) {
                                additionalInfo = match[1] || match[0];
                                break;
                            }
                        }
                    }
                    
                    // FILTER CHECK: Only add if content passes filter
                    if (!shouldFilter(title, description, additionalInfo)) {
                        // Check if this content is already added (avoid duplicates)
                        const isDuplicate = contentItems.some(item => item.title === title);
                        if (!isDuplicate && title.length > 2) {
                            contentItems.push({
                                title: title,
                                description: description || 'No description available',
                                additionalInfo: additionalInfo || ''
                            });
                        }
                    }
                }
            });
        });
        
        return JSON.stringify(contentItems);
    })();
    """.trimIndent()
                                            ) { result ->
                                                try {
                                                    val clean = result.removeSurrounding("\"")
                                                        .replace("\\\"", "\"")
                                                        .replace("\\n", " ")
                                                        .replace("\\r", "")
                                                        .replace("\\t", " ")

                                                    if (clean.startsWith("[") && clean.endsWith("]")) {
                                                        val jsonContent =
                                                            clean.substring(1, clean.length - 1)
                                                        val items = mutableListOf<ScrapedContent>()

                                                        // Simple JSON parsing for our structure
                                                        val objectPattern =
                                                            """\{[^}]*\}""".toRegex()
                                                        val matches =
                                                            objectPattern.findAll(jsonContent)

                                                        for (match in matches) {
                                                            val obj = match.value
                                                            val titleMatch =
                                                                """"title":"([^"]*)"""".toRegex()
                                                                    .find(obj)
                                                            val descMatch =
                                                                """"description":"([^"]*)"""".toRegex()
                                                                    .find(obj)
                                                            val infoMatch =
                                                                """"additionalInfo":"([^"]*)"""".toRegex()
                                                                    .find(obj)

                                                            if (titleMatch != null) {
                                                                val newItem = ScrapedContent(
                                                                    title = titleMatch.groupValues[1],
                                                                    description = descMatch?.groupValues?.get(
                                                                        1
                                                                    ) ?: "",
                                                                    additionalInfo = infoMatch?.groupValues?.get(
                                                                        1
                                                                    ) ?: "",
                                                                    sourceUrl = urlList.getOrNull(
                                                                        currentIndex
                                                                    ) ?: ""
                                                                )

                                                                // Check if this item already exists in allContent
                                                                if (!allContent.any { it.title == newItem.title && it.sourceUrl == newItem.sourceUrl }) {
                                                                    items.add(newItem)
                                                                }
                                                            }
                                                        }

                                                        // Add new items to allContent
                                                        if (items.isNotEmpty()) {
                                                            allContent = allContent + items
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    allContent = allContent + ScrapedContent(
                                                        title = "Error during scroll scrape: ${e.message}",
                                                        description = "",
                                                        additionalInfo = "",
                                                        sourceUrl = urlList.getOrNull(currentIndex)
                                                            ?: ""
                                                    )
                                                }

                                                scrollCount++
                                                scrollAndScrape()
                                            }
                                        }, 2000) // Wait 2 seconds between scrolls
                                    }
                                }

                                // Start scrolling + scraping loop
                                scrollAndScrape()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                allContent = allContent + ScrapedContent(
                                    title = "Error loading: $description",
                                    description = "Failed to load: $failingUrl",
                                    additionalInfo = "",
                                    sourceUrl = failingUrl ?: ""
                                )
                                isProcessing = false
                                loadNextUrl()
                            }
                        }

                        // Start with the first URL
                        loadUrl(urlList[0])
                    }
                },
                modifier = Modifier.size(0.dp),
                update = { /* Keep empty to prevent conflicts */ }
            )
            // Results Section
            if (displayedContent.isEmpty() && !isCompleted) {
                Popup(
                    alignment = Alignment.Center,
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Discovering hackathons...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),

                            verticalArrangement = Arrangement.spacedBy(12.dp),

                    state = listState
                ) {
                    if (searchFilteredContent.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🔍 No Results Found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try adjusting your search terms",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(searchFilteredContent) { index, content ->
                            val visible = remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                delay(index * 50L)
                                visible.value = true
                            }

                            AnimatedVisibility(
                                visible = visible.value,
                                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                                    animationSpec = tween(600),
                                    initialOffsetY = { it / 2 }
                                )
                            ) {
                                val siteName = try {
                                    val uri = android.net.Uri.parse(content.sourceUrl)
                                    uri.host?.replace("www.", "") ?: ""
                                } catch (e: Exception) {
                                    ""
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val searchQuery = "${content.title} $siteName"
                                            val encodedQuery =
                                                java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://www.google.com/search?q=$encodedQuery")
                                            )
                                            context.startActivity(intent)
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = content.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        if (siteName.isNotEmpty()) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                ),
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            ) {
                                                Text(
                                                    text = "🌐 $siteName",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    )
                                                )
                                            }
                                        }

                                        if (content.description.isNotEmpty()) {
                                            Text(
                                                text = content.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }

                                        if (content.additionalInfo.isNotEmpty()) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            ) {
                                                Text(
                                                    text = "ℹ️ ${content.additionalInfo}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
