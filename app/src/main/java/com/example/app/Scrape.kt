package com.arjundubey.app
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.net.URLEncoder


data class ScrapedContent(
    val title: String = "",
    val content: String = "",
    val sourceUrl: String = "",
    var description: String = "",
    val additionalInfo: String = ""
)

data class AwanMessage(val role: String, val content: String)

@Composable
fun ChatTriggerPopup(
    displayedContent: List<ScrapedContent>,
    welcomeText: String = "i know all about current ongoing hackathons ask me and wait for minimum 1 minute",
    chatPopupModifier: Modifier = Modifier,
    fabContainerColor: Color = MaterialTheme.colorScheme.tertiary,
    fabContentColor: Color = MaterialTheme.colorScheme.onBackground,
    fabIcon: ImageVector = Icons.Default.QuestionMark,
    fabIconDescription: String = "Open Chat",
    showOverlay: Boolean = true,
    overlayColor: Color = Color.Black.copy(alpha = 0.5f)
) {
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
            containerColor = fabContainerColor,
            contentColor = fabContentColor
        ) {
            Icon(
                imageVector = fabIcon,
                contentDescription = fabIconDescription,
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
                    .background(if (showOverlay) overlayColor else Color.Transparent)
            ) {
                ChatPopup(
                    isVisible = true,
                    onDismiss = { showChatPopup = false },
                    extractedTitles = extractedTitles,
                    welcomeText = welcomeText,
                    modifier = chatPopupModifier
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
fun ScraperJobScreen() {

    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val urlList = listOf(
        "https://m.timesjobs.com/mobile/jobs-search-result.html?cboWorkExp1=0",
        "https://www.naukri.com/fresher-jobs-1",
        "https://www.naukri.com/fresher-jobs-2",
        "https://www.naukri.com/fresher-jobs-3",
        "https://www.naukri.com/fresher-jobs-4",
        "https://www.naukri.com/remote-jobs-1",
        "https://www.naukri.com/remote-jobs-2",
        "https://www.naukri.com/remote-jobs-3",
        "https://www.naukri.com/remote-jobs-4",

        "https://www.naukri.com/sales-jobs-1",
        "https://www.naukri.com/sales-jobs-2",
        "https://www.naukri.com/sales-jobs-3",
        "https://www.naukri.com/sales-jobs-4",
        "https://www.naukri.com/mnc-jobs-1",
        "https://www.naukri.com/mnc-jobs-2",
        "https://www.naukri.com/mnc-jobs-3",
        "https://www.naukri.com/mnc-jobs-4",
        "https://www.naukri.com/hr-jobs-1",
        "https://www.naukri.com/hr-jobs-2",
        "https://www.naukri.com/hr-jobs-3",
        "https://www.naukri.com/hr-jobs-4",
        "https://www.naukri.com/jobs-in-india-1",
        "https://www.naukri.com/jobs-in-india-2",
        "https://www.naukri.com/jobs-in-india-3",
        "https://www.naukri.com/jobs-in-india-4",
        "https://www.naukri.com/engineering-jobs-1",
        "https://www.naukri.com/engineering-jobs-2",
        "https://www.naukri.com/engineering-jobs-3",
        "https://www.naukri.com/engineering-jobs-4",
        "https://www.naukri.com/it-jobs-1",
        "https://www.naukri.com/it-jobs-2",
        "https://www.naukri.com/it-jobs-3",
        "https://www.naukri.com/it-jobs-4",
        "https://www.naukri.com/analytics-jobs-1",
        "https://www.naukri.com/analytics-jobs-2",
        "https://www.naukri.com/analytics-jobs-3",
        "https://www.naukri.com/analytics-jobs-4",
        "https://www.naukri.com/data-science-jobs-1",
        "https://www.naukri.com/data-science-jobs-2",
        "https://www.naukri.com/data-science-jobs-3",
        "https://www.naukri.com/data-science-jobs-4",
        "https://www.naukri.com/marketing-jobs-1",
        "https://www.naukri.com/marketing-jobs-2",
        "https://www.naukri.com/marketing-jobs-3",
        "https://www.naukri.com/marketing-jobs-4"
    )

    val listState = rememberLazyListState()
    var allContent by remember { mutableStateOf<List<ScrapedContent>>(emptyList()) }
    var completedUrls by remember { mutableStateOf(0) }
    var isScrapingComplete by remember { mutableStateOf(false) }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    var showInitialLoader by remember { mutableStateOf(true) }
    var restartTrigger by remember { mutableStateOf(0) }
    var newHackathonsCount by remember { mutableStateOf(0) }

    // Hide loader after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        showInitialLoader = false
    }

    // AUTO-RESTART: Deep scraping only
    LaunchedEffect(isScrapingComplete) {
        if (isScrapingComplete) {
            delay(300000) // Wait 5 minutes before next cycle

            // Snapshot existing hackathons for comparison
            val existingKeys = allContent.map { "${it.title}_${it.sourceUrl}" }.toSet()

            // Reset for re-scraping
            completedUrls = 0
            isScrapingComplete = false

            // Trigger WebView reload
            restartTrigger++

            // After deep scraping completes, filter and append only new items
            delay(15000) // Wait for deep scrape to complete

            // Get items added since restart
            val newItems = allContent.filter { item ->
                val key = "${item.title}_${item.sourceUrl}"
                !existingKeys.contains(key)
            }

            if (newItems.isNotEmpty()) {
                newHackathonsCount += newItems.size
            }
        }
    }

    // Filter content based on search query
    val searchFilteredContent = remember(allContent, searchQuery) {
        val uniqueContent = allContent.distinctBy { "${it.title}_${it.sourceUrl}" }
        if (searchQuery.isEmpty()) {
            uniqueContent
        } else {
            uniqueContent.filter { content ->
                val searchLower = searchQuery.lowercase()
                content.title.lowercase().contains(searchLower) ||
                        content.description.lowercase().contains(searchLower) ||
                        content.additionalInfo.lowercase().contains(searchLower)
            }
        }
    }

    // Track progress
    val progress = remember(completedUrls) {
        if (urlList.isEmpty()) 1f else completedUrls.toFloat() / urlList.size
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
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(5.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("🔍 Search roles for internships") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalCount = allContent.distinctBy { "${it.title}_${it.sourceUrl}" }.size
                            Text(
                                text = if (searchQuery.isEmpty()) {
                                    when {
                                        !isScrapingComplete -> "Found $totalCount internships • Deep scraping... ⚡"
                                        newHackathonsCount > 0 -> "Showing all $totalCount internshps • $newHackathonsCount new ✨"
                                        else -> "Showing all $totalCount internships • Auto-refreshing..."
                                    }
                                } else {
                                    "Found ${searchFilteredContent.size} matching \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (!isScrapingComplete) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Text(
                                        text = "${completedUrls}/${urlList.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isScrapingComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }

        // DEEP WEBVIEWS ONLY
        Box(modifier = Modifier.size(0.dp)) {
            urlList.forEachIndexed { index, url ->
                key("$index-$restartTrigger") {
                    AndroidView(
                        factory = {
                            WebView(context).apply {
                                webViews[index] = this

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = false
                                    cacheMode = WebSettings.LOAD_NO_CACHE // Always fresh data
                                    blockNetworkImage = false // Load images for deep scraping
                                    loadsImagesAutomatically = true
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                                }

                                webViewClient = object : WebViewClient() {
                                    private var isProcessing = false
                                    private var scrollCount = 0
                                    private val maxScrolls = 10 // Deep scraping: 10 scrolls

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        super.onPageFinished(view, pageUrl)

                                        if (isProcessing) return
                                        isProcessing = true

                                        // DEEP DELAY: Wait longer for JS to load completely
                                        val initialDelay = 2000L
                                        view?.postDelayed({
                                            startScrollingScrape(view)
                                        }, initialDelay)
                                    }

                                    private fun startScrollingScrape(view: WebView?) {
                                        scrollAndScrape(view)
                                    }

                                    private fun scrapeCurrentView(view: WebView?) {
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                const selectors = ['h1', 'h2', 'h3', '[class*="title"]', '[class*="heading"]'];
                                                let items = [];
                                                const blockedKeywords = [
                                                'connect with us','employer home','help center','privacy policy','company',
                                                    'help', 'legal', 'fresher jobs', 'sales jobs', 'remote jobs', "mnc jobs",
    "hr jobs",
    "jobs in india",
    "engineering jobs",
    "it jobs",
    "analytics jobs",
    "data science jobs",
    "marketing jobs", 'jobs found', 'loading',
                                                    'we care for you', 'recruiters are', 'more job offers', 'sort by', 'filters', 'live challenges',
                                                    'upcoming challenges', 'previous challenges', 'knowledge', 'contact', 'notifications',
                                                    'log in', 'sign up', 'requirements', 'following', 'available', 'please'
                                                ];
                                                
                                                function shouldFilter(title, description, additionalInfo) {
                                                    const combinedText = (title + ' ' + description + ' ' + additionalInfo).toLowerCase();
                                                    return blockedKeywords.some(keyword => combinedText.includes(keyword.toLowerCase()));
                                                }
                                                
                                                selectors.forEach(sel => {
                                                    document.querySelectorAll(sel).forEach(h => {
                                                        const title = h.innerText.trim();
                                                        if (title.length < 3) return;
                                                        
                                                        let desc = '';
                                                        let info = '';
                                                        
                                                        let next = h.nextElementSibling;
                                                        for (let i = 0; i < 5 && next; i++) {
                                                            const text = next.innerText?.trim() || '';
                                                            if (text && text.length > 5 && text.length < 200 && !desc) {
                                                                desc = text;
                                                                break;
                                                            }
                                                            next = next.nextElementSibling;
                                                        }
                                                        
                                                        const container = h.closest('[class*="card"], [class*="item"]');
                                                        if (container) {
                                                            const containerText = container.innerText;
                                                            const match = containerText.match(/(\d+[^\w\s]*\s*(?:participants|days|₹|\$|prizes?))/i);
                                                            if (match) info = match[1];
                                                        }
                                                        
                                                        if (!shouldFilter(title, desc, info)) {
                                                            const isDuplicate = items.some(item => item.title === title);
                                                            if (!isDuplicate && title.length > 2) {
                                                                items.push({
                                                                    title: title,
                                                                    description: desc || 'No description',
                                                                    additionalInfo: info || ''
                                                                });
                                                            }
                                                        }
                                                    });
                                                });
                                                
                                                return JSON.stringify(items);
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
                                                    val jsonContent = clean.substring(1, clean.length - 1)
                                                    val items = mutableListOf<ScrapedContent>()

                                                    val objectPattern = """\{[^}]*\}""".toRegex()
                                                    objectPattern.findAll(jsonContent).forEach { match ->
                                                        val obj = match.value
                                                        val titleMatch = """"title":"([^"]*)"""".toRegex().find(obj)
                                                        val descMatch = """"description":"([^"]*)"""".toRegex().find(obj)
                                                        val infoMatch = """"additionalInfo":"([^"]*)"""".toRegex().find(obj)

                                                        if (titleMatch != null) {
                                                            val newItem = ScrapedContent(
                                                                title = titleMatch.groupValues[1],
                                                                description = descMatch?.groupValues?.get(1) ?: "",
                                                                additionalInfo = infoMatch?.groupValues?.get(1) ?: "",
                                                                sourceUrl = url
                                                            )

                                                            if (!allContent.any { it.title == newItem.title && it.sourceUrl == newItem.sourceUrl }) {
                                                                items.add(newItem)
                                                            }
                                                        }
                                                    }

                                                    if (items.isNotEmpty()) {
                                                        allContent = allContent + items
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Silent fail
                                            }
                                        }
                                    }

                                    private fun scrollAndScrape(view: WebView?) {
                                        if (scrollCount >= maxScrolls) {
                                            completedUrls++
                                            if (completedUrls >= urlList.size) {
                                                isScrapingComplete = true
                                            }
                                            return
                                        }

                                        scrapeCurrentView(view)

                                        // DEEP DELAY: Longer wait between scrolls for complete loading
                                        val scrollDelay = 1500L

                                        view?.evaluateJavascript(
                                            "(function(){ window.scrollTo(0, document.body.scrollHeight); })();"
                                        ) { _ ->
                                            scrollCount++
                                            view?.postDelayed({ scrollAndScrape(view) }, scrollDelay)
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        completedUrls++
                                        if (completedUrls >= urlList.size) {
                                            isScrapingComplete = true
                                        }
                                    }
                                }

                                // START LOADING
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier.size(0.dp),
                        update = { }
                    )
                }
            }
        }

        // Results Section
        if (allContent.isEmpty() && showInitialLoader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚡ Deep scraping ${urlList.size} sites...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (allContent.isEmpty() && !showInitialLoader) {
            LoadingScreen(
                messages = listOf(
                    "Please wait...",
                    "Loading your data...",
                    "Almost there...",
                    "Just a moment...",
                    "maybe you have slow internet",
                    "uffffff",
                    "just right there ",
                    "i beg dont go",
                ))
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
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                } else {
                    items(searchFilteredContent.size) { index ->
                        val content = searchFilteredContent[index]
                        val siteName = try {
                            Uri.parse(content.sourceUrl).host?.replace("www.", "") ?: ""
                        } catch (e: Exception) {
                            ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val query = "${content.title} ${content.description} $siteName"
                                    val encoded = URLEncoder.encode(query, "UTF-8")
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=$encoded")
                                    )
                                    context.startActivity(intent)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = content.title,
                                    style = MaterialTheme.typography.headlineSmall,
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
                                            text = siteName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                            text = content.additionalInfo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    ChatTriggerPopup(allContent, welcomeText = "ask me about internship or job or tell me your skill i wil find the best suiting one ")
}


// Make sure your ScrapedContent data class is defined like this:

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
    var completedUrls by remember { mutableStateOf(0) }
    var isScrapingComplete by remember { mutableStateOf(false) }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    var showInitialLoader by remember { mutableStateOf(true) }
    var restartTrigger by remember { mutableStateOf(0) }
    var newHackathonsCount by remember { mutableStateOf(0) }
    var isFirstScrape by remember { mutableStateOf(true) }

    // Hide loader after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        showInitialLoader = false
    }

    // AUTO-RESTART: Fast first scrape, then deep subsequent scrapes
    LaunchedEffect(isScrapingComplete) {
        if (isScrapingComplete) {
            delay(0) // Wait 5 minutes before next cycle

            // Snapshot existing hackathons for comparison
            val existingKeys = allContent.map { "${it.title}_${it.sourceUrl}" }.toSet()

            // Reset for re-scraping
            completedUrls = 0
            isScrapingComplete = false
            isFirstScrape = false // Switch to deep scraping mode

            // Trigger WebView reload
            restartTrigger++

            // After deep scraping completes, filter and append only new items
            delay(0)

            // Get items added since restart
            val newItems = allContent.filter { item ->
                val key = "${item.title}_${item.sourceUrl}"
                !existingKeys.contains(key)
            }

            if (newItems.isNotEmpty()) {
                newHackathonsCount += newItems.size
            }
        }
    }

    // Filter content based on search query
    val searchFilteredContent = remember(allContent, searchQuery) {
        val uniqueContent = allContent.distinctBy { "${it.title}_${it.sourceUrl}" }
        if (searchQuery.isEmpty()) {
            uniqueContent
        } else {
            uniqueContent.filter { content ->
                val searchLower = searchQuery.lowercase()
                content.title.lowercase().contains(searchLower) ||
                        content.description.lowercase().contains(searchLower) ||
                        content.additionalInfo.lowercase().contains(searchLower)
            }
        }
    }

    // Track progress
    val progress = remember(completedUrls) {
        if (urlList.isEmpty()) 1f else completedUrls.toFloat() / urlList.size
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
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(5.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("🔍 Search Hackathons, Organisers...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalCount = allContent.distinctBy { "${it.title}_${it.sourceUrl}" }.size
                            Text(
                                text = if (searchQuery.isEmpty()) {
                                    when {
                                        !isScrapingComplete && isFirstScrape -> "Found $totalCount hackathons • Quick scraping..."
                                        !isScrapingComplete && !isFirstScrape -> "Found $totalCount hackathons • Deep scraping... ⚡"
                                        newHackathonsCount > 0 -> "Showing all $totalCount hackathons • $newHackathonsCount new ✨"
                                        else -> "Showing all $totalCount hackathons • Auto-refreshing..."
                                    }
                                } else {
                                    "Found ${searchFilteredContent.size} matching \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (!isScrapingComplete) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFirstScrape)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Text(
                                        text = "${completedUrls}/${urlList.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFirstScrape)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isScrapingComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    if (isFirstScrape)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.tertiary
                                )
                        )
                    }
                }
            }
        }

        // ADAPTIVE WEBVIEWS: Fast first scrape, deep subsequent scrapes
        Box(modifier = Modifier.size(0.dp)) {
            urlList.forEachIndexed { index, url ->
                key("$index-$restartTrigger") {
                    AndroidView(
                        factory = {
                            WebView(context).apply {
                                webViews[index] = this

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = false
                                    cacheMode = if (isFirstScrape) {
                                        WebSettings.LOAD_CACHE_ELSE_NETWORK // Fast initial load
                                    } else {
                                        WebSettings.LOAD_NO_CACHE // Fresh data on deep scrape
                                    }
                                    blockNetworkImage = !isFirstScrape // Load images on deep scrape
                                    loadsImagesAutomatically = !isFirstScrape
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                                }

                                webViewClient = object : WebViewClient() {
                                    private var isProcessing = false
                                    private var scrollCount = 0
                                    // ADAPTIVE: 3 scrolls for fast scrape, 10 for deep scrape
                                    private val maxScrolls = if (isFirstScrape) 3 else 10

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        super.onPageFinished(view, pageUrl)

                                        if (isProcessing) return
                                        isProcessing = true

                                        // ADAPTIVE DELAY: Wait longer on deep scrape for JS to load
                                        val initialDelay = if (isFirstScrape) 500L else 2000L
                                        view?.postDelayed({
                                            startScrollingScrape(view)
                                        }, initialDelay)
                                    }

                                    private fun startScrollingScrape(view: WebView?) {
                                        scrollAndScrape(view)
                                    }

                                    private fun scrapeCurrentView(view: WebView?) {
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                const selectors = ['h1', 'h2', 'h3', '[class*="title"]', '[class*="heading"]'];
                                                let items = [];
                                                const blockedKeywords = [
                                                'searching...','back','peers','within','and promote',
                                                    'hackathons', 'filter', 'end', 'clubnapa', 'publicinvite', 'linkedin', 'devpost',
                                                    'insights', 'stories', 'recordings', 'documentation', 'filters', 'live challenges',
                                                    'upcoming challenges', 'previous challenges', 'knowledge', 'contact', 'notifications',
                                                    'log in', 'sign up', 'requirements', 'following', 'available', 'please'
                                                ];
                                                
                                                function shouldFilter(title, description, additionalInfo) {
                                                    const combinedText = (title + ' ' + description + ' ' + additionalInfo).toLowerCase();
                                                    return blockedKeywords.some(keyword => combinedText.includes(keyword.toLowerCase()));
                                                }
                                                
                                                selectors.forEach(sel => {
                                                    document.querySelectorAll(sel).forEach(h => {
                                                        const title = h.innerText.trim();
                                                        if (title.length < 3) return;
                                                        
                                                        let desc = '';
                                                        let info = '';
                                                        
                                                        let next = h.nextElementSibling;
                                                        for (let i = 0; i < 5 && next; i++) {
                                                            const text = next.innerText?.trim() || '';
                                                            if (text && text.length > 5 && text.length < 200 && !desc) {
                                                                desc = text;
                                                                break;
                                                            }
                                                            next = next.nextElementSibling;
                                                        }
                                                        
                                                        const container = h.closest('[class*="card"], [class*="item"]');
                                                        if (container) {
                                                            const containerText = container.innerText;
                                                            const match = containerText.match(/(\d+[^\w\s]*\s*(?:participants|days|₹|\$|prizes?))/i);
                                                            if (match) info = match[1];
                                                        }
                                                        
                                                        if (!shouldFilter(title, desc, info)) {
                                                            const isDuplicate = items.some(item => item.title === title);
                                                            if (!isDuplicate && title.length > 2) {
                                                                items.push({
                                                                    title: title,
                                                                    description: desc || 'No description',
                                                                    additionalInfo: info || ''
                                                                });
                                                            }
                                                        }
                                                    });
                                                });
                                                
                                                return JSON.stringify(items);
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
                                                    val jsonContent = clean.substring(1, clean.length - 1)
                                                    val items = mutableListOf<ScrapedContent>()

                                                    val objectPattern = """\{[^}]*\}""".toRegex()
                                                    objectPattern.findAll(jsonContent).forEach { match ->
                                                        val obj = match.value
                                                        val titleMatch = """"title":"([^"]*)"""".toRegex().find(obj)
                                                        val descMatch = """"description":"([^"]*)"""".toRegex().find(obj)
                                                        val infoMatch = """"additionalInfo":"([^"]*)"""".toRegex().find(obj)

                                                        if (titleMatch != null) {
                                                            val newItem = ScrapedContent(
                                                                title = titleMatch.groupValues[1],
                                                                description = descMatch?.groupValues?.get(1) ?: "",
                                                                additionalInfo = infoMatch?.groupValues?.get(1) ?: "",
                                                                sourceUrl = url
                                                            )

                                                            if (!allContent.any { it.title == newItem.title && it.sourceUrl == newItem.sourceUrl }) {
                                                                items.add(newItem)
                                                            }
                                                        }
                                                    }

                                                    if (items.isNotEmpty()) {
                                                        allContent = allContent + items
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Silent fail
                                            }
                                        }
                                    }

                                    private fun scrollAndScrape(view: WebView?) {
                                        if (scrollCount >= maxScrolls) {
                                            completedUrls++
                                            if (completedUrls >= urlList.size) {
                                                isScrapingComplete = true
                                            }
                                            return
                                        }

                                        scrapeCurrentView(view)

                                        // ADAPTIVE DELAY: Longer wait between scrolls on deep scrape
                                        val scrollDelay = if (isFirstScrape) 400L else 1500L

                                        view?.evaluateJavascript(
                                            "(function(){ window.scrollTo(0, document.body.scrollHeight); })();"
                                        ) { _ ->
                                            scrollCount++
                                            view?.postDelayed({ scrollAndScrape(view) }, scrollDelay)
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        completedUrls++
                                        if (completedUrls >= urlList.size) {
                                            isScrapingComplete = true
                                        }
                                    }
                                }

                                // START LOADING
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier.size(0.dp),
                        update = { }
                    )
                }
            }
        }

        // Results Section
        if (allContent.isEmpty() && showInitialLoader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚡ Parallel scraping ${urlList.size} sites...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (allContent.isEmpty() && !showInitialLoader) {
            LoadingScreen(
                messages = listOf(
                    "Please wait...",
                    "Loading your data...",
                    "Almost there...",
                    "Just a moment...",
                    "maybe you have slow internet",
                    "uffffff",
                    "just right there ",
                    "i beg dont go",
                )
            )
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
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                } else {
                    items(searchFilteredContent.size) { index ->
                        val content = searchFilteredContent[index]
                        val siteName = try {
                            Uri.parse(content.sourceUrl).host?.replace("www.", "") ?: ""
                        } catch (e: Exception) {
                            ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val query = "${content.title} ${content.description} $siteName"
                                    val encoded = URLEncoder.encode(query, "UTF-8")
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=$encoded")
                                    )
                                    context.startActivity(intent)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = content.title,
                                    style = MaterialTheme.typography.headlineSmall,
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
                                            text = siteName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                            text = content.additionalInfo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    ChatTriggerPopup(allContent, welcomeText = "ask me about hackathons or tell me your skill i wil find the best suiting one and wait for 1 min i take some time to analyse and think :) ")
}
@Composable
fun ChatPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    extractedTitles: List<String>,
    welcomeText: String = "i know all about current ongoing hackathons ask me and wait for minimum 1 minute", // Default parameter
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
        // Now using the parameter instead of hardcoded text
        onStreamStart()

        Thread {
            try {
                welcomeText.forEach { char ->
                    Thread.sleep(10) // Slightly slower for welcome message
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
                    chatMessages = listOf(AwanMessage("Helpful Assitant that gives answer in as much detail as possible with points examples and with easy to understand language pretends to know each and every detail of hackathons", finalMessage))
                    streamingMessage = ""
                }
            )
        }
    }

    // Rest of your component remains the same...
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
                text = "📋currently i know about  ${extractedTitles.size} items",
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
