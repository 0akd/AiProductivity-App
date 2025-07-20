package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class ProblemData(
    val questionId: String,
    val questionFrontendId: String,
    val title: String,
    val content: String,
    val likes: Int,
    val dislikes: Int,
    val stats: String,
    val similarQuestions: String,
    val categoryTitle: String,
    val hints: List<String>,
    val topicTags: List<String>,
    val companyTags: String?,
    val difficulty: String,
    val isPaidOnly: Boolean,
    val solution: String?,
    val hasSolution: Boolean,
    val hasVideoSolution: Boolean,
    val url: String
)

fun parseJsonResponse(jsonString: String): ProblemData? {
    return try {
        val json = JSONObject(jsonString)

        // Parse hints array
        val hintsArray = json.optJSONArray("hints") ?: JSONArray()
        val hints = mutableListOf<String>()
        for (i in 0 until hintsArray.length()) {
            hints.add(hintsArray.getString(i))
        }

        // Parse topic tags array
        val topicTagsArray = json.optJSONArray("topicTags") ?: JSONArray()
        val topicTags = mutableListOf<String>()
        for (i in 0 until topicTagsArray.length()) {
            val tagObject = topicTagsArray.getJSONObject(i)
            topicTags.add(tagObject.getString("name"))
        }

        ProblemData(
            questionId = json.optString("questionId", "N/A"),
            questionFrontendId = json.optString("questionFrontendId", "N/A"),
            title = json.optString("title", "N/A"),
            content = json.optString("content", "N/A"),
            likes = json.optInt("likes", 0),
            dislikes = json.optInt("dislikes", 0),
            stats = json.optString("stats", "N/A"),
            similarQuestions = json.optString("similarQuestions", "N/A"),
            categoryTitle = json.optString("categoryTitle", "N/A"),
            hints = hints,
            topicTags = topicTags,
            companyTags = json.optString("companyTags"),
            difficulty = json.optString("difficulty", "N/A"),
            isPaidOnly = json.optBoolean("isPaidOnly", false),
            solution = json.optString("solution"),
            hasSolution = json.optBoolean("hasSolution", false),
            hasVideoSolution = json.optBoolean("hasVideoSolution", false),
            url = json.optString("url", "N/A")
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ProblemDetailScreen(
    slug: String,
    url: String,
    onBackClick: () -> Unit
) {
    var problemData by remember { mutableStateOf<ProblemData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Construct the API URL with the slug
    val apiUrl = "https://leetcode-api-pied.vercel.app/problem/$slug"

    LaunchedEffect(slug) {
        try {
            isLoading = true
            errorMessage = null

            // Make API call
            val response = withContext(Dispatchers.IO) {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                }
            }

            problemData = parseJsonResponse(response)
            if (problemData == null) {
                errorMessage = "Failed to parse problem data"
            }
            isLoading = false

        } catch (e: Exception) {
            errorMessage = "Failed to load problem: ${e.message}"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = problemData?.title ?: "Problem: $slug",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading problem data...")
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // Trigger retry by changing the LaunchedEffect key
                            problemData = null
                            isLoading = true
                            errorMessage = null
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            problemData != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Problem Info Card
                    item {
                        ProblemInfoCard(problemData!!)
                    }

                    // Problem Description Card
                    item {
                        ProblemDescriptionCard(problemData!!.content)
                    }

                    // Statistics Card
                    item {
                        StatisticsCard(problemData!!)
                    }

                    // Topic Tags Card
                    item {
                        TopicTagsCard(problemData!!.topicTags)
                    }

                    // Hints Card
                    if (problemData!!.hints.isNotEmpty()) {
                        item {
                            HintsCard(problemData!!.hints)
                        }
                    }

                    // Additional Info Card
                    item {
                        AdditionalInfoCard(problemData!!)
                    }
                }
            }
        }
    }
    if (problemData != null) {
    val list = listOf(ScrapedContent(problemData!!.content))


    ChatTriggerPopup(list)}
}

@Composable
fun ProblemInfoCard(problemData: ProblemData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Problem Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Question ID", problemData.questionId)
            InfoRow("Frontend ID", problemData.questionFrontendId)
            InfoRow("Title", problemData.title)
            InfoRow("Difficulty", problemData.difficulty)
            InfoRow("Category", problemData.categoryTitle)
            InfoRow("Paid Only", if (problemData.isPaidOnly) "Yes" else "No")
        }
    }
}

@Composable
fun ProblemDescriptionCard(content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Problem Description",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content.replace("<[^>]*>".toRegex(), ""), // Basic HTML tag removal
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun StatisticsCard(problemData: ProblemData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Likes", problemData.likes.toString())
            InfoRow("Dislikes", problemData.dislikes.toString())
            InfoRow("Stats", problemData.stats)
            InfoRow("Has Solution", if (problemData.hasSolution) "Yes" else "No")
            InfoRow("Has Video Solution", if (problemData.hasVideoSolution) "Yes" else "No")
        }
    }
}

@Composable
fun TopicTagsCard(topicTags: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Topic Tags",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (topicTags.isEmpty()) {
                Text(
                    text = "No topic tags available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                topicTags.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HintsCard(hints: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hints",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            hints.forEachIndexed { index, hint ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "${index + 1}. $hint",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdditionalInfoCard(problemData: ProblemData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("URL", problemData.url)
            InfoRow("Similar Questions", problemData.similarQuestions)
            problemData.companyTags?.let {
                if (it.isNotEmpty()) {
                    InfoRow("Company Tags", it)
                }
            }
            problemData.solution?.let {
                if (it.isNotEmpty()) {
                    InfoRow("Solution", it)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}