package com.arjundubey.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualSearchScreen(
    // YouTube states
    youtubeSearchQuery: String,
    onYoutubeSearchQueryChange: (String) -> Unit,
    youtubeResults: List<SearchResult>,
    onYoutubeResultsChange: (List<SearchResult>) -> Unit,
    youtubeIsLoading: Boolean,
    onYoutubeLoadingChange: (Boolean) -> Unit,
    youtubeErrorMessage: String?,
    onYoutubeErrorMessageChange: (String?) -> Unit,
    onYoutubeClick: (url: String) -> Unit = {},

    // PDF states
    pdfSearchQuery: String,
    onPdfSearchQueryChange: (String) -> Unit,
    pdfResults: List<SearchResult>,
    onPdfResultsChange: (List<SearchResult>) -> Unit,
    pdfIsLoading: Boolean,
    onPdfLoadingChange: (Boolean) -> Unit,
    pdfErrorMessage: String?,
    onPdfErrorMessageChange: (String?) -> Unit,
    onPdfClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    // Common search query
    var commonSearchQuery by remember { mutableStateOf("") }

    var youtubeCurrentPage by remember { mutableStateOf(1) }
    var youtubeHasMoreResults by remember { mutableStateOf(true) }

    var pdfCurrentPage by remember { mutableStateOf(1) }
    var pdfHasMoreResults by remember { mutableStateOf(true) }

    fun searchYouTube(query: String, page: Int = 1) {
        if (query.isBlank()) return

        scope.launch {
            onYoutubeLoadingChange(true)
            onYoutubeErrorMessageChange(null)

            if (page == 1) {
                onYoutubeResultsChange(emptyList())
                youtubeCurrentPage = 1
                youtubeHasMoreResults = true
            }

            try {
                val results = withContext(Dispatchers.IO) {
                    searchGoogle(query, "site:youtube.com", page)
                }

                if (page == 1) {
                    onYoutubeResultsChange(results)
                } else {
                    onYoutubeResultsChange(youtubeResults + results)
                }

                youtubeHasMoreResults = results.size == 10
                youtubeCurrentPage = page

                if (results.isEmpty() && page == 1) {
                    onYoutubeErrorMessageChange("No YouTube results found")
                }
            } catch (e: Exception) {
                onYoutubeErrorMessageChange("Error: ${e.message}")
            } finally {
                onYoutubeLoadingChange(false)
            }
        }
    }

    fun searchPdfs(query: String, page: Int = 1) {
        if (query.isBlank()) return

        scope.launch {
            onPdfLoadingChange(true)
            onPdfErrorMessageChange(null)

            if (page == 1) {
                onPdfResultsChange(emptyList())
                pdfCurrentPage = 1
                pdfHasMoreResults = true
            }

            try {
                val results = withContext(Dispatchers.IO) {
                    searchGoogle(query, "filetype:pdf", page)
                }

                if (page == 1) {
                    onPdfResultsChange(results)
                } else {
                    onPdfResultsChange(pdfResults + results)
                }

                pdfHasMoreResults = results.size == 10
                pdfCurrentPage = page

                if (results.isEmpty() && page == 1) {
                    onPdfErrorMessageChange("No PDF results found")
                }
            } catch (e: Exception) {
                onPdfErrorMessageChange("Error: ${e.message}")
            } finally {
                onPdfLoadingChange(false)
            }
        }
    }

    fun performBothSearches() {
        searchYouTube(commonSearchQuery, 1)
        searchPdfs(commonSearchQuery, 1)
    }

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()

        ) {
            // Common Search Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(

                ) {
                    Text(
                        text = "Search Engine for Studying",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = commonSearchQuery,
                        onValueChange = { commonSearchQuery = it },
                        label = { Text("Enter search query...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (commonSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    commonSearchQuery = ""
                                    onYoutubeResultsChange(emptyList())
                                    onPdfResultsChange(emptyList())
                                    youtubeCurrentPage = 1
                                    youtubeHasMoreResults = true
                                    pdfCurrentPage = 1
                                    pdfHasMoreResults = true
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
                        enabled = commonSearchQuery.isNotBlank() && !youtubeIsLoading && !pdfIsLoading
                    ) {
                        Text(
                            if (youtubeIsLoading || pdfIsLoading) "Searching..." else "Extract"
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
                    ResultsColumn(
                        title = "Videos",
                        results = youtubeResults,
                        isLoading = youtubeIsLoading,
                        errorMessage = youtubeErrorMessage,
                        currentPage = youtubeCurrentPage,
                        hasMoreResults = youtubeHasMoreResults,
                        onLoadMore = {
                            if (!youtubeIsLoading && youtubeHasMoreResults) {
                                searchYouTube(commonSearchQuery, youtubeCurrentPage + 1)
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

                // Right Column - PDF Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    ResultsColumn(
                        title = "ManuScripts",
                        results = pdfResults,
                        isLoading = pdfIsLoading,
                        errorMessage = pdfErrorMessage,
                        currentPage = pdfCurrentPage,
                        hasMoreResults = pdfHasMoreResults,
                        onLoadMore = {
                            if (!pdfIsLoading && pdfHasMoreResults) {
                                searchPdfs(commonSearchQuery, pdfCurrentPage + 1)
                            }
                        },
                        onResultClick = { result ->
                            onPdfClick(result.url, result.title)
                        },
                        cardColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ResultsColumn(
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
                        ResultCard(
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
                        text = "Enter a query and click Extract",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ResultCard(result: SearchResult, onClick: () -> Unit) {
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

suspend fun searchGoogle(query: String, filter: String, page: Int = 1): List<SearchResult> {
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