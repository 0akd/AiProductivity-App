package com.arjundubey.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        val hintsArray = json.optJSONArray("hints") ?: JSONArray()
        val hints = mutableListOf<String>()
        for (i in 0 until hintsArray.length()) {
            hints.add(hintsArray.getString(i))
        }

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
    onBackClick: () -> Unit,
    onSearchClick: (String) -> Unit // New callback for search
) {
    var problemData by remember { mutableStateOf<ProblemData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val apiUrl = "https://leetcode-api-pied.vercel.app/problem/$slug"

    LaunchedEffect(slug) {
        try {
            isLoading = true
            errorMessage = null

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

    Box(modifier = Modifier.fillMaxSize()) {
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

                Spacer(modifier = Modifier.width(48.dp))
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
                        item {
                            ProblemDescriptionCard(problemData!!.content)
                        }

                        item {
                            ProblemInfoCard(problemData!!)
                        }

                        item {
                            StatisticsCard(problemData!!)
                        }

                        item {
                            TopicTagsCard(problemData!!.topicTags)
                        }

                        if (problemData!!.hints.isNotEmpty()) {
                            item {
                                HintsCard(problemData!!.hints)
                            }
                        }

                        item {
                            AdditionalInfoCard(problemData!!)
                        }

                        // Extra space for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Floating Action Button for Search
        if (problemData != null) {
            FloatingActionButton(
                onClick = {
                    onSearchClick(problemData!!.title + " leetcode")
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search for solutions"
                )
            }
        }

        // Chat Trigger Popup
        if (problemData != null) {
            val list = listOf(ScrapedContent(problemData!!.content))
            ChatTriggerPopup(
                list,
                welcomeText = "Ah i see i know this question very well so what should i tell you hint solution or thinking methodology (my model takes time so please wait upto 60 seconds)"
            )
        }
    }
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

fun decodeHtmlEntities(text: String): String {
    val entityMap = mapOf(
        "&nbsp;" to " ", "&lt;" to "<", "&gt;" to ">", "&amp;" to "&",
        "&quot;" to "\"", "&apos;" to "'", "&le;" to "≤", "&ge;" to "≥",
        "&lt;=" to "≤", "&gt;=" to "≥", "&equals;" to "=", "&ne;" to "≠",
        "&plus;" to "+", "&minus;" to "-", "&times;" to "×", "&divide;" to "÷",
        "&cent;" to "¢", "&pound;" to "£", "&euro;" to "€", "&copy;" to "©",
        "&reg;" to "®", "&trade;" to "™", "&deg;" to "°", "&permil;" to "‰",
        "&prime;" to "′", "&Prime;" to "″", "&infin;" to "∞", "&radic;" to "√",
        "&sum;" to "∑", "&prod;" to "∏", "&int;" to "∫", "&there4;" to "∴",
        "&because;" to "∵", "&sim;" to "∼", "&cong;" to "≅", "&asymp;" to "≈",
        "&ne;" to "≠", "&equiv;" to "≡", "&le;" to "≤", "&ge;" to "≥",
        "&sub;" to "⊂", "&sup;" to "⊃", "&nsub;" to "⊄", "&sube;" to "⊆",
        "&supe;" to "⊇", "&oplus;" to "⊕", "&otimes;" to "⊗", "&perp;" to "⊥",
        "&sdot;" to "⋅", "&lceil;" to "⌈", "&rceil;" to "⌉", "&lfloor;" to "⌊",
        "&rfloor;" to "⌋", "&lang;" to "⟨", "&rang;" to "⟩", "&loz;" to "◊",
        "&spades;" to "♠", "&clubs;" to "♣", "&hearts;" to "♥", "&diams;" to "♦"
    )

    var result = text
    entityMap.forEach { (entity, replacement) ->
        result = result.replace(entity, replacement)
    }

    result = result.replace("&#(\\d+);".toRegex()) { match ->
        val code = match.groupValues[1].toIntOrNull()
        if (code != null) Character.toString(code) else match.value
    }

    result = result.replace("&#x([0-9a-fA-F]+);".toRegex()) { match ->
        val code = match.groupValues[1].toIntOrNull(16)
        if (code != null) Character.toString(code) else match.value
    }

    return result
}

@Composable
fun ProblemDescriptionCard(content: String) {
    val decodedContent = remember(content) {
        decodeHtmlEntities(content.replace("<[^>]*>".toRegex(), ""))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Question",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = decodedContent,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 25.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
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