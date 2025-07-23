package com.arjundubey.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray



@Composable
fun Hack() {
    var titles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        titles = fetchTitlesFromMultipleApis(
            listOf(
                "https://unstop.com/api/public/opportunity/search-result?opportunity=hackathons&page=1&per_page=15&oppstatus=open",
                "https://unstop.com/api/public/opportunity/search-result?opportunity=hackathons&page=2&per_page=15&oppstatus=open",
                "https://unstop.com/api/public/opportunity/search-result?opportunity=hackathons&page=3&per_page=15&oppstatus=open",
                "https://unstop.com/api/public/opportunity/search-result?opportunity=hackathons&page=4&per_page=15&oppstatus=open",
                "https://unstop.com/api/public/opportunity/search-result?opportunity=hackathons&page=5&per_page=15&oppstatus=open",


                // Add more pages/URLs here
            )
        )
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(titles) { title ->
                Text(text = title, modifier = Modifier.padding(vertical = 8.dp))
                Divider()
            }
        }
    }
}

suspend fun fetchTitlesFromMultipleApis(urls: List<String>): List<String> = coroutineScope {
    val allTitles = mutableListOf<String>()

    val jobs = urls.map { url ->
        async(Dispatchers.IO) {
            fetchTitlesFromSingleApi(url)
        }
    }

    jobs.awaitAll().forEach { titles ->
        allTitles.addAll(titles)
    }

    allTitles
}

fun fetchTitlesFromSingleApi(apiUrl: String): List<String> {
    val titles = mutableListOf<String>()

    try {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val outerJson = JSONObject(response)
        val innerData = outerJson.getJSONObject("data")
        val dataArray: JSONArray = innerData.getJSONArray("data")

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val title = item.optString("title")
            if (title.isNotBlank()) {
                titles.add(title)
            }
        }

    } catch (e: Exception) {
        titles.add("Error from $apiUrl: ${e.message}")
    }

    return titles
}
