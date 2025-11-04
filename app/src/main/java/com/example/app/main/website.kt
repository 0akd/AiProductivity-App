package com.arjundubey.app

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val websiteUrls = arrayOf(
    "https://google.com",
    "https://github.com",
    "https://stackoverflow.com",
    "https://youtube.com",
    "https://facebook.com",
    "https://twitter.com",
    "https://instagram.com",
    "https://linkedin.com",
    "https://reddit.com",
    "https://wikipedia.org",
    "https://amazon.com",
    "https://netflix.com",
    "https://spotify.com",
    "https://medium.com",
    "https://unstop.com"
)

// Function to extract website name from URL
fun extractWebsiteName(url: String): String {
    return try {
        val cleanUrl = url.replace("https://", "").replace("http://", "").replace("www.", "")
        val domain = cleanUrl.split("/")[0]
        val name = domain.split(".")[0]
        name.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        "Website"
    }
}

@Composable
fun WebsiteButton(url: String, name: String) {
    val context = LocalContext.current

    Button(
        onClick = {
            try {
                println("Button clicked for: $name with URL: $url")

                val intent = Intent(context, FullscreenWebViewActivity::class.java).apply {
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                println("Activity started successfully")

            } catch (e: Exception) {
                println("Error starting activity: ${e.message}")
                e.printStackTrace()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Open $name",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WebsitesList() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose a Website to Open",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(websiteUrls) { url ->
                    val websiteName = extractWebsiteName(url)
                    WebsiteButton(url = url, name = websiteName)
                }
            }
        }
    }
}

