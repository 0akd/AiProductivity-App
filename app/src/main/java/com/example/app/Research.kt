package com.arjundubey.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Composable
fun SearchButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
fun CategoryToggleButton(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
fun SearchLauncherScreen() {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    val sharedPreferences = remember { context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // State for category expansion
    var generalSearchExpanded by remember { mutableStateOf(false) }
    var pdfSearchExpanded by remember { mutableStateOf(false) }
    var aiAssistantsExpanded by remember { mutableStateOf(false) }

    // Load saved text on startup
    LaunchedEffect(Unit) {
        searchText = sharedPreferences.getString("saved_text", "") ?: ""
    }

    // Auto-save text with debounce
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty()) {
            delay(500) // Debounce to avoid saving on every keystroke
            withContext(Dispatchers.IO) {
                sharedPreferences.edit {
                    putString("saved_text", searchText)
                    apply()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Text field with auto-save
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            label = { Text("Enter search text") },
            placeholder = { Text("Type something to search...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            })
        )

        // General Search category
        CategoryToggleButton(
            title = "General Search",
            isExpanded = generalSearchExpanded,
            onToggle = { generalSearchExpanded = !generalSearchExpanded }
        )

        if (generalSearchExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    SearchButton(
                        icon = Icons.Default.Search,
                        label = "Google Search",
                        onClick = { openSearch(context, searchText, SearchEngine.GOOGLE) }
                    )

                    SearchButton(
                        icon = Icons.Default.PlayArrow,
                        label = "YouTube Search",
                        onClick = { openSearch(context, searchText, SearchEngine.YOUTUBE) }
                    )

                    SearchButton(
                        icon = Icons.Default.Code,
                        label = "GitHub Search",
                        onClick = { openSearch(context, searchText, SearchEngine.GITHUB) }
                    )
                }
            }
        }

        // PDF Search category
        CategoryToggleButton(
            title = "PDF Search",
            isExpanded = pdfSearchExpanded,
            onToggle = { pdfSearchExpanded = !pdfSearchExpanded }
        )

        if (pdfSearchExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    SearchButton(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Google PDF Search",
                        onClick = { openSearch(context, "$searchText filetype:pdf", SearchEngine.GOOGLE) }
                    )
                }
            }
        }

        // AI Assistants category
        CategoryToggleButton(
            title = "AI Assistants",
            isExpanded = aiAssistantsExpanded,
            onToggle = { aiAssistantsExpanded = !aiAssistantsExpanded }
        )

        if (aiAssistantsExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    SearchButton(
                        icon = Icons.Default.Chat,
                        label = "ChatGPT",
                        onClick = { openSearch(context, searchText, SearchEngine.CHATGPT) }
                    )

                    SearchButton(
                        icon = Icons.Default.Memory,
                        label = "Gemini",
                        onClick = { openSearch(context, searchText, SearchEngine.GEMINI) }
                    )

                    SearchButton(
                        icon = Icons.Default.Explore,
                        label = "DeepSeek",
                        onClick = { openSearch(context, searchText, SearchEngine.DEEPSEEK) }
                    )

                    SearchButton(
                        icon = Icons.Default.Psychology,
                        label = "Perplexity AI",
                        onClick = { openSearch(context, searchText, SearchEngine.PERPLEXITY) }
                    )

                    SearchButton(
                        icon = Icons.Default.Rocket,
                        label = "Grok",
                        onClick = { openSearch(context, searchText, SearchEngine.GROK) }
                    )

                    SearchButton(
                        icon = Icons.Default.Lightbulb,
                        label = "Google AI Mode",
                        onClick = { openSearch(context, searchText, SearchEngine.GOOGLE_AI_MODE) }
                    )
                }
            }
        }
    }
}

enum class SearchEngine {
    GOOGLE, YOUTUBE, GITHUB, CHATGPT, GEMINI, DEEPSEEK, PERPLEXITY, GROK, GOOGLE_AI_MODE
}

fun openSearch(context: Context, query: String, engine: SearchEngine) {
    if (query.isBlank()) {
        Toast.makeText(context, "Please enter some text to search", Toast.LENGTH_SHORT).show()
        return
    }

    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = when (engine) {
        SearchEngine.GOOGLE -> "https://www.google.com/search?q=$encodedQuery"
        SearchEngine.YOUTUBE -> "https://www.youtube.com/results?search_query=$encodedQuery"
        SearchEngine.GITHUB -> "https://github.com/search?q=$encodedQuery"
        SearchEngine.CHATGPT -> "https://chat.openai.com/?q=$encodedQuery"
        SearchEngine.GEMINI -> "https://gemini.google.com/app?q=$encodedQuery"
        SearchEngine.DEEPSEEK -> "https://chat.deepseek.com/?q=$encodedQuery"
        SearchEngine.PERPLEXITY -> "https://www.perplexity.ai/?q=$encodedQuery"
        SearchEngine.GROK -> "https://x.com/i/grok?q=$encodedQuery"
        SearchEngine.GOOGLE_AI_MODE -> "https://www.google.com/search?udm=50&q=$encodedQuery"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found to open the link", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening search: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}