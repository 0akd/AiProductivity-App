

package com.arjundubey.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class SearchResullt(
    val title: String,
    val url: String,
    val snippet: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemSearchScreen(
    initialQuery: String = "",
    onBackClick: () -> Unit,
    onYoutubeClick: (url: String) -> Unit = {},
    onWebResultClick: (url: String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // YouTube states
    var youtubeResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var youtubeIsLoading by remember { mutableStateOf(false) }
    var youtubeErrorMessage by remember { mutableStateOf<String?>(null) }
    var youtubeCurrentPage by remember { mutableStateOf(1) }
    var youtubeHasMoreResults by remember { mutableStateOf(true) }

    // Web results states
    var webResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var webIsLoading by remember { mutableStateOf(false) }
    var webErrorMessage by remember { mutableStateOf<String?>(null) }
    var webCurrentPage by remember { mutableStateOf(1) }
    var webHasMoreResults by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf(initialQuery) }

    fun searchYouTube(query: String, page: Int = 1) {
        if (query.isBlank()) return

        scope.launch {
            youtubeIsLoading = true
            youtubeErrorMessage = null

            if (page == 1) {
                youtubeResults = emptyList()
                youtubeCurrentPage = 1
                youtubeHasMoreResults = true
            }

            try {
                // Append "solution" to the query for YouTube searches
                val modifiedQuery = "$query solution"
                val results = withContext(Dispatchers.IO) {
                    searchGooglle(modifiedQuery, "site:youtube.com", page)
                }

                if (page == 1) {
                    youtubeResults = results
                } else {
                    youtubeResults = youtubeResults + results
                }

                youtubeHasMoreResults = results.size == 10
                youtubeCurrentPage = page

                if (results.isEmpty() && page == 1) {
                    youtubeErrorMessage = "No YouTube results found"
                }
            } catch (e: Exception) {
                youtubeErrorMessage = "Error: ${e.message}"
            } finally {
                youtubeIsLoading = false
            }
        }
    }

    fun searchWeb(query: String, page: Int = 1) {
        if (query.isBlank()) return

        scope.launch {
            webIsLoading = true
            webErrorMessage = null

            if (page == 1) {
                webResults = emptyList()
                webCurrentPage = 1
                webHasMoreResults = true
            }

            try {
                // Append "solution" to the query for web searches
                val modifiedQuery = "$query solution"
                val results = withContext(Dispatchers.IO) {
                    searchGooglle(modifiedQuery, "", page)
                }

                if (page == 1) {
                    webResults = results
                } else {
                    webResults = webResults + results
                }

                webHasMoreResults = results.size == 10
                webCurrentPage = page

                if (results.isEmpty() && page == 1) {
                    webErrorMessage = "No web results found"
                }
            } catch (e: Exception) {
                webErrorMessage = "Error: ${e.message}"
            } finally {
                webIsLoading = false
            }
        }
    }

    fun performBothSearches() {
        searchYouTube(searchQuery, 1)
        searchWeb(searchQuery, 1)
    }

    // Auto-search on launch if query provided
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            performBothSearches()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Results") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Search for Solutions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Enter search query...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    youtubeResults = emptyList()
                                    webResults = emptyList()
                                }) {
                                    Text("✕")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { performBothSearches() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = searchQuery.isNotBlank() && !youtubeIsLoading && !webIsLoading
                    ) {
                        Text(
                            if (youtubeIsLoading || webIsLoading) "Searching..." else "Search"
                        )
                    }
                }
            }

            // Results Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // Left Column - YouTube Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    ResulltsColumn(
                        title = "Video Solutions",
                        results = youtubeResults,
                        isLoading = youtubeIsLoading,
                        errorMessage = youtubeErrorMessage,
                        currentPage = youtubeCurrentPage,
                        hasMoreResults = youtubeHasMoreResults,
                        onLoadMore = {
                            if (!youtubeIsLoading && youtubeHasMoreResults) {
                                searchYouTube(searchQuery, youtubeCurrentPage + 1)
                            }
                        },
                        onResultClick = { result ->
                            onYoutubeClick(result.url)
                        },
                        cardColor = MaterialTheme.colorScheme.errorContainer
                    )
                }

                // Divider
                VerticalDivider()

                // Right Column - Web Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    ResulltsColumn(
                        title = "Web Results",
                        results = webResults,
                        isLoading = webIsLoading,
                        errorMessage = webErrorMessage,
                        currentPage = webCurrentPage,
                        hasMoreResults = webHasMoreResults,
                        onLoadMore = {
                            if (!webIsLoading && webHasMoreResults) {
                                searchWeb(searchQuery, webCurrentPage + 1)
                            }
                        },
                        onResultClick = { result ->
                            onWebResultClick(result.url)
                        },
                        cardColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ResulltsColumn(
    title: String,
    results: List<SearchResult>,
    isLoading: Boolean,
    errorMessage: String?,
    currentPage: Int,
    hasMoreResults: Boolean,
    onLoadMore: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
    cardColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading && currentPage == 1 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            results.isNotEmpty() -> {
                Text(
                    text = "${results.size} results" + if (hasMoreResults) " (scroll for more)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.url }) { result ->
                        ResulltCard(
                            result = result,
                            onClick = { onResultClick(result) }
                        )
                    }

                    if (hasMoreResults) {
                        item {
                            Card(
                                onClick = onLoadMore,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Load more...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter a query and click Search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ResulltCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (result.snippet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

suspend fun searchGooglle(query: String, filter: String, page: Int = 1): List<SearchResult> {
    val apiKey = "AIzaSyB06BgsQp_D7om4S1tuZHCYMC9RjYGYBpk"
    val searchEngineId = "b01af29d4ac22497a"

    val startIndex = (page - 1) * 10 + 1
    val encodedQuery = URLEncoder.encode("$query $filter", "UTF-8")
    val urlString = "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$searchEngineId&q=$encodedQuery&start=$startIndex"

    return try {
        val response = URL(urlString).readText()
        val json = JSONObject(response)
        val items = json.optJSONArray("items")

        val results = mutableListOf<SearchResult>()

        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                results.add(
                    SearchResult(
                        title = item.optString("title", "Untitled"),
                        url = item.optString("link", ""),
                        snippet = item.optString("snippet", "")
                    )
                )
            }
        }

        results
    } catch (e: Exception) {
        throw Exception("Failed to fetch results: ${e.message}")
    }
}