package com.example.myapplication
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming


data class ScrapedContent(
    val title: String = "",
    val content: String = "",
    val sourceUrl: String = "",
    val description: String = "",
    val additionalInfo: String = ""
)

data class AwanMessage(val role: String, val content: String)

@Composable
fun ChatTriggerPopup(displayedContent: List<ScrapedContent>) {
    var showChatPopup by remember { mutableStateOf(false) }

    // Extract titles from displayedContent
    val extractedTitles = remember(displayedContent) {
        displayedContent.map { it.title }.distinct()
    }

    // ✅ FAB Launcher Popup - Only sized to the FAB, no touch blocking
    Popup(
        alignment = Alignment.BottomEnd,
        properties = PopupProperties(focusable = false)
    ) {
        FloatingActionButton(
            onClick = { showChatPopup = true },
            modifier = Modifier
                .padding(16.dp), // space from bottom and start
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Icon(
                imageVector = Icons.Default.QuestionMark,
                contentDescription = "Open Chat",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // 💬 ChatPopup in a separate fullscreen popup
    if (showChatPopup) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = { showChatPopup = false },
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // Optional overlay
            ) {
                ChatPopup(
                    isVisible = true,
                    onDismiss = { showChatPopup = false },
                    extractedTitles = extractedTitles
                )
            }
        }
    }
}


data class AwanChoice(val message: AwanMessage)


interface AwanApi {
    @POST("v1/chat/completions")
    fun chat(@Body request: AwanRequest): Call<AwanResponse>

    @POST("v1/chat/completions")
    @Streaming
    fun chatStream(@Body request: AwanRequest): Call<ResponseBody>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScraperScreen() {

        var searchQuery by remember { mutableStateOf("") }
        val context = LocalContext.current
        val urlList = listOf(
            "https://unstop.com/hackathons?page=1",
            "https://devpost.com/hackathons?challenge_type[]=online&page=4&status[]=open",
            "https://unstop.com/hackathons?page=2",


            "https://devfolio.co/search?primary_filter=hackathons&type=application_open",
            "https://unstop.com/hackathons?page=3",
            "https://devpost.com/hackathons?challenge_type[]=online&status[]=upcoming",
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


                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    // Loading Status or Search Bar
                    if (isCompleted) {
                        // Search Bar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(5.dp)) {

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("🔍 Search Hackathons , Organisers... ") },
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
                            shape = RectangleShape, // 👈 This is crucial
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

                        ) {
                            Column(

                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .clip(RectangleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(loadingProgress)
                                            .clip(RectangleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentSite = try {
                                        val uri = android.net.Uri.parse(urlList[currentIndex])
                                        uri.host?.replace("www.", "") ?: "Unknown"
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                    Text(
                                        text = "Currently scraping: $currentSite",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                    Card(

                                    ) {
                                        Text(
                                            text = "${allContent.size}",

                                            color = MaterialTheme.colorScheme.onBackground,


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
                                                    text = " $siteName",
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
                                                    text = "${content.additionalInfo}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    ChatTriggerPopup(displayedContent)
    }
@Composable
fun ChatPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    extractedTitles: List<String>,
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf<List<AwanMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var streamingMessage by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var hasShownWelcome by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val api = remember { getAwanApi() }
    fun streamWelcomeMessage(
        onStreamStart: () -> Unit,
        onStreamChunk: (String) -> Unit,
        onStreamComplete: (String) -> Unit
    ) {
        val welcomeText = "Hello there how can i help you? ( Note - I know in detail about all the hackathons and dsa questions listed in the app )"

        onStreamStart()

        Thread {
            try {
                welcomeText.forEach { char ->
                    Thread.sleep(50) // Slightly slower for welcome message
                    onStreamChunk(char.toString())
                }
                onStreamComplete(welcomeText)
            } catch (e: Exception) {
                onStreamComplete(welcomeText)
            }
        }.start()
    }
    // Show welcome message when popup first opens
    LaunchedEffect(isVisible) {
        if (isVisible && !hasShownWelcome && chatMessages.isEmpty()) {
            hasShownWelcome = true
            streamWelcomeMessage(
                onStreamStart = {
                    isStreaming = true
                    streamingMessage = ""
                },
                onStreamChunk = { chunk ->
                    streamingMessage += chunk
                },
                onStreamComplete = { finalMessage ->
                    isStreaming = false
                    chatMessages = listOf(AwanMessage("Helpful Assitant that gives answer in as much detail as possible with points examples and with easy to understand language", finalMessage))
                    streamingMessage = ""
                }
            )
        }
    }

    if (isVisible) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            onDismissRequest = onDismiss
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header (same as before)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                                    CircleShape

                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(24.dp)
                                    .zIndex(999f)

                            )
                        }
                    }

                    // Chat Messages with streaming support
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        // Streaming message (if active)
                        if (isStreaming && streamingMessage.isNotEmpty()) {
                            item {
                                ChatMessageCard(
                                    message = AwanMessage("Helpful Assitant that gives answer in as much detail as possible with points examples and with easy to understand language", streamingMessage),
                                    isUser = false,
                                    isStreaming = true
                                )
                            }
                        }

                        // Loading indicator
                        if (isLoading && !isStreaming) {
                            item {
                                LoadingIndicator()
                            }
                        }

                        // Regular chat messages
                        items(chatMessages.reversed()) { message ->
                            ChatMessageCard(
                                message = message,
                                isUser = message.role == "user",
                                isStreaming = false
                            )
                        }
                    }

                    // Input Area
                    InputArea(
                        userInput = userInput,
                        onInputChange = { userInput = it },
                        isLoading = isLoading || isStreaming,
                        extractedTitles = extractedTitles,
                        onSendMessage = {
                            if (userInput.isNotBlank() && !isLoading && !isStreaming) {
                                val titlesContext = if (extractedTitles.isNotEmpty()) {
                                    "Here are the current hackathons I found:\n\n" +
                                            extractedTitles.joinToString("\n") { "• $it" } +
                                            "\n\nUser Question: $userInput"
                                } else {
                                    userInput
                                }

                                val userMessage = AwanMessage("user", userInput)
                                val contextMessage = AwanMessage("user", titlesContext)
                                chatMessages = chatMessages + userMessage

                                userInput = ""
                                isLoading = true
                                streamingMessage = ""

                                // Start streaming
                                streamChatResponse(
                                    api = api,
                                    contextMessage = contextMessage,
                                    onStreamStart = {
                                        isLoading = false
                                        isStreaming = true
                                        streamingMessage = ""
                                    },
                                    onStreamChunk = { chunk ->
                                        streamingMessage += chunk
                                    },
                                    onStreamComplete = { finalMessage ->
                                        isStreaming = false
                                        chatMessages = chatMessages + AwanMessage("Helpful Assitant that gives answer in as much detail as possible with points examples and with easy to understand language", finalMessage)
                                        streamingMessage = ""
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        isStreaming = false
                                        chatMessages = chatMessages + AwanMessage("Helpful Assitant that gives answer in as much detail as possible with points examples and with easy to understand language", "❌ Error: $error")
                                        streamingMessage = ""
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
fun streamChatResponse(
    api: AwanApi,
    contextMessage: AwanMessage,
    onStreamStart: () -> Unit,
    onStreamChunk: (String) -> Unit,
    onStreamComplete: (String) -> Unit,
    onError: (String) -> Unit
) {
    val request = AwanRequest(messages = listOf(contextMessage), stream = true)
    val gson = Gson()

    api.chatStream(request).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (!response.isSuccessful) {
                onError("HTTP ${response.code()}")
                return
            }

            val responseBody = response.body()
            if (responseBody == null) {
                onError("Empty response")
                return
            }

            onStreamStart()

            Thread {
                try {
                    val reader = responseBody.byteStream().bufferedReader()
                    val completeMessage = StringBuilder()

                    reader.forEachLine { line ->
                        if (line.startsWith("data: ")) {
                            val jsonData = line.substring(6).trim()
                            if (jsonData != "[DONE]" && jsonData.isNotEmpty()) {
                                try {
                                    val chunk = gson.fromJson(jsonData, StreamingChunk::class.java)
                                    val content = chunk.choices?.firstOrNull()?.delta?.content

                                    if (content != null) {
                                        completeMessage.append(content)

                                        // Stream character by character with delay
                                        content.forEach { char ->
                                            Thread.sleep(30) // Adjust delay as needed (30ms per character)
                                            onStreamChunk(char.toString())
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Skip malformed chunks
                                }
                            }
                        }
                    }

                    onStreamComplete(completeMessage.toString())
                    reader.close()
                } catch (e: Exception) {
                    onError(e.message ?: "Streaming error")
                }
            }.start()
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            onError(t.message ?: "Network error")
        }
    })
}
@Composable
fun ChatMessageCard(
    message: AwanMessage,
    isUser: Boolean,
    isStreaming: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 32.dp else 0.dp,
                end = if (isUser) 0.dp else 32.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "You" else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (isStreaming) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Blinking cursor for streaming effect
                    BlinkingCursor()
                }
            }

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
fun BlinkingCursor() {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            visible = !visible
        }
    }

    Text(
        text = if (visible) "▌" else "",
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun LoadingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AI is thinking...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InputArea(
    userInput: String,
    onInputChange: (String) -> Unit,
    isLoading: Boolean,
    extractedTitles: List<String>,
    onSendMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📋 ${extractedTitles.size} hackathons scraped",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about hackathons...") },
                    enabled = !isLoading,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = onSendMessage,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}
