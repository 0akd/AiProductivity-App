package com.arjundubey.app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest


@Composable
fun TextFileScreen(
    courseName: String,
    subjectName: String,
    chapterName: String,
    fileName: String,
    onBackPressed: () -> Unit,
    // Add new parameter for file system navigation
    onNavigateToFileSystem: (pathSegments: List<String>) -> Unit = { _ -> }
) {
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var availableFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentFileIndex by remember { mutableStateOf(0) }
    var fileTitle by remember { mutableStateOf("") }

    // Convert parameters to path segments, filtering out "default" values
    val pathSegments = listOf(courseName, subjectName, chapterName).filter {
        it != "default_course" && it != "default_subject" && it != "default_chapter" && it.isNotBlank()
    }

    // Create the dynamic URL path
    val basePath = pathSegments.joinToString("/")

    // Create the URL using the dynamic path
    val fileUrl = remember(pathSegments, currentFileName) {
        val random = (Math.random() * 10000).toInt()
        "https://raw.githubusercontent.com/0akd/coursemission/main/$basePath/$currentFileName?cache=$random"
    }

    // Function to load content for a specific file
    fun loadFileContent(fileName: String) {
        currentFileName = fileName
        val index = availableFiles.indexOf(fileName)
        if (index != -1) {
            currentFileIndex = index
        }
    }

    // Function to get potential file names in the directory
    suspend fun discoverAvailableFiles(): List<String> {
        val txtFiles = mutableListOf<String>()

        // Try to find numbered files starting from 1
        for (i in 1..20) { // Check up to 20 files
            try {
                val testUrl = "https://raw.githubusercontent.com/0akd/coursemission/main/$basePath/$i.txt"
                val testContent = withContext(Dispatchers.IO) {
                    try {
                        URL(testUrl).readText()
                        "$i.txt"
                    } catch (e: Exception) {
                        null
                    }
                }
                if (testContent != null) {
                    txtFiles.add(testContent)
                } else {
                    break // Stop when we don't find a consecutive file
                }
            } catch (e: Exception) {
                break
            }
        }

        return txtFiles.sorted()
    }

    // Extract file title from content
    fun extractTitle(content: String): String {
        val firstLine = content.lines().firstOrNull { it.trim().isNotEmpty() }?.trim()
        return if (!firstLine.isNullOrEmpty()) {
            // Clean up the first line (remove markdown formatting)
            firstLine
                .replace(Regex("^#+\\s*"), "") // Remove markdown headers
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold formatting
                .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic formatting
                .trim()
        } else {
            currentFileName.removeSuffix(".txt")
        }
    }

    // Enhanced back button handler
    fun handleBackPress() {
        if (pathSegments.isNotEmpty()) {
            // Navigate back to FileSystemScreen with the current path
            onNavigateToFileSystem(pathSegments)
        } else {
            // Fallback to original back behavior
            onBackPressed()
        }
    }

    // Load the text file when the composable is first created or when parameters change
    LaunchedEffect(fileUrl) {
        isLoading = true
        errorMessage = null
        textContent = null

        try {
            // Discover available files if not already done
            if (availableFiles.isEmpty()) {
                availableFiles = discoverAvailableFiles()
                currentFileIndex = availableFiles.indexOf(currentFileName).takeIf { it != -1 } ?: 0
            }

            val content = withContext(Dispatchers.IO) {
                URL(fileUrl).readText()
            }
            textContent = content
            fileTitle = extractTitle(content)
        } catch (e: Exception) {
            errorMessage = "Error loading file: ${e.message}"
            Log.e("TextFileScreen", "Error loading file: $fileUrl", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Back button and title row - UPDATED to use enhanced back handler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { handleBackPress() },  // ← Updated to use enhanced back handler
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (fileTitle.isNotEmpty()) fileTitle else "Document",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (pathSegments.isNotEmpty()) {
                    Text(
                        text = pathSegments.joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }


        // Main content area
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading content...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error Loading File",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "URL: $fileUrl",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Retry loading
                            isLoading = true
                            errorMessage = null
                            textContent = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }

            textContent != null -> {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val scrollState = rememberScrollState()

                        // Use the markdown renderer instead of plain text
                        MarkdownText(
                            markdown = textContent ?: "",
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No content to display",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Bottom navigation buttons row (only show if multiple files are available)
        if (availableFiles.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous file button
                IconButton(
                    onClick = {
                        if (currentFileIndex > 0) {
                            loadFileContent(availableFiles[currentFileIndex - 1])
                        }
                    },
                    enabled = currentFileIndex > 0
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIos,
                                contentDescription = "Previous File",
                                tint = if (currentFileIndex > 0)
                                    MaterialTheme.colorScheme.onBackground
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Current file indicator
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentFileIndex + 1} / ${availableFiles.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentFileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                // Next file button
                IconButton(
                    onClick = {
                        if (currentFileIndex < availableFiles.size - 1) {
                            loadFileContent(availableFiles[currentFileIndex + 1])
                        }
                    },
                    enabled = currentFileIndex < availableFiles.size - 1
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Next File",
                                tint = if (currentFileIndex < availableFiles.size - 1)
                                    MaterialTheme.colorScheme.onBackground
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data classes for markdown elements
sealed class MarkdownElement {
    data class TextElement(val annotatedString: androidx.compose.ui.text.AnnotatedString) : MarkdownElement()
    data class ImageElement(val url: String, val altText: String, val title: String?) : MarkdownElement()
}

// Enhanced Markdown parser and renderer with image support
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val elements = remember(markdown) {
        parseMarkdownWithImages(markdown)
    }

    Column(modifier = modifier) {
        elements.forEach { element ->
            when (element) {
                is MarkdownElement.TextElement -> {
                    if (element.annotatedString.text.isNotBlank()) {
                        Text(
                            text = element.annotatedString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                is MarkdownElement.ImageElement -> {
                    MarkdownImage(
                        url = element.url,
                        altText = element.altText,
                        title = element.title,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownImage(
    url: String,
    altText: String,
    title: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Debug logging
        LaunchedEffect(url) {
            Log.d("MarkdownImage", "Loading image: $url")
            Log.d("MarkdownImage", "Alt text: $altText")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            Column {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = altText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 400.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Failed to load image",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Failed to load image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )

                // Caption section
                if (altText.isNotBlank() || title != null) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        if (altText.isNotBlank()) {
                            Text(
                                text = altText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = if (altText.isNotBlank()) 4.dp else 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Enhanced markdown parser that handles both text and images
fun parseMarkdownWithImages(markdown: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = markdown.split("\n")
    val textBuffer = mutableListOf<String>()

    Log.d("MarkdownParser", "Starting to parse markdown with ${lines.size} lines")

    fun flushTextBuffer() {
        if (textBuffer.isNotEmpty()) {
            val textContent = textBuffer.joinToString("\n")
            if (textContent.trim().isNotEmpty()) {
                elements.add(MarkdownElement.TextElement(parseTextMarkdown(textContent)))
                Log.d("MarkdownParser", "Added text element with ${textContent.length} characters")
            }
            textBuffer.clear()
        }
    }

    lines.forEachIndexed { index, line ->
        // Enhanced regex to handle various image formats
        val imageRegex = """!\[([^\]]*)\]\(([^\s)]+)(?:\s+"([^"]*)")?\)""".toRegex()
        val imageMatch = imageRegex.find(line)

        if (imageMatch != null) {
            Log.d("MarkdownParser", "Found image on line $index: $line")

            // Flush any accumulated text before processing the image
            flushTextBuffer()

            val altText = imageMatch.groupValues[1]
            val url = imageMatch.groupValues[2].trim()
            val title = imageMatch.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }

            Log.d("MarkdownParser", "Image details - Alt: '$altText', URL: '$url', Title: '$title'")
            elements.add(MarkdownElement.ImageElement(url, altText, title))

            // Add any remaining text after the image in the same line
            val remainingText = line.substring(imageMatch.range.last + 1).trim()
            if (remainingText.isNotEmpty()) {
                textBuffer.add(remainingText)
            }
        } else {
            // Check if line might contain malformed image syntax
            if (line.contains("![") && line.contains("](")) {
                Log.w("MarkdownParser", "Potential malformed image syntax on line $index: $line")
            }
            // Regular text line
            textBuffer.add(line)
        }
    }

    // Flush any remaining text
    flushTextBuffer()

    Log.d("MarkdownParser", "Parsing complete. Total elements: ${elements.size}")
    elements.forEachIndexed { index, element ->
        when (element) {
            is MarkdownElement.TextElement -> Log.d("MarkdownParser", "Element $index: Text")
            is MarkdownElement.ImageElement -> Log.d("MarkdownParser", "Element $index: Image - ${element.url}")
        }
    }

    return elements
}

// Enhanced text markdown parser

// Extension functions to enable operator overloading for TextUnit
operator fun TextUnit.plus(other: TextUnit): TextUnit = TextUnit(this.value + other.value, this.type)
operator fun TextUnit.minus(other: TextUnit): TextUnit = TextUnit(this.value - other.value, this.type)
fun parseTextMarkdown(
    markdown: String,
    baseSize: TextUnit = 24.sp
): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")

        // Skip the first line by starting from index 1
        for (i in 1 until lines.size) {
            val line = lines[i]
            when {
                // Headers
                line.startsWith("# ") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.8f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize + 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(line.substring(2))
                        }
                    }
                    append("\n\n")
                }
                line.startsWith("## ") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.7f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize + 6.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(line.substring(3))
                        }
                    }
                    append("\n\n")
                }
                line.startsWith("### ") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.6f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize + 4.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(line.substring(4))
                        }
                    }
                    append("\n\n")
                }
                // Bold text **text**
                line.contains("**") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.5f
                        )
                    ) {
                        processBoldText(line, baseSize)
                    }
                }
                // Italic text *text* (but not **)
                line.contains("*") && !line.contains("**") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.5f
                        )
                    ) {
                        processItalicText(line, baseSize)
                    }
                }
                // Code blocks ```
                line.startsWith("```") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.4f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize - 1.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                background = Color.LightGray
                            )
                        ) {
                            append(line)
                        }
                    }
                    append("\n")
                }
                // List items
                line.startsWith("- ") || line.startsWith("* ") -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.5f
                        )
                    ) {
                        append("• ")
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize
                            )
                        ) {
                            append(line.substring(2))
                        }
                    }
                    append("\n")
                }
                // Numbered lists
                line.matches("""\d+\.\s.*""".toRegex()) -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.5f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize
                            )
                        ) {
                            append(line)
                        }
                    }
                    append("\n")
                }
                // Empty line
                line.isBlank() -> {
                    append("\n")
                }
                // Regular text
                else -> {
                    withStyle(
                        style = ParagraphStyle(
                            lineHeight = baseSize * 1.5f
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = baseSize
                            )
                        ) {
                            append(line)
                        }
                    }
                    append("\n")
                }
            }
        }
    }
}

// Helper functions need to be extension functions on AnnotatedString.Builder


private fun AnnotatedString.Builder.processBoldText(line: String, baseSize: TextUnit) {
    val regex = "\\*\\*(.*?)\\*\\*".toRegex()
    var currentIndex = 0
    val matches = regex.findAll(line)

    for (match in matches) {
        // Append text before the bold section
        if (match.range.first > currentIndex) {
            withStyle(
                style = SpanStyle(
                    fontSize = baseSize
                )
            ) {
                append(line.substring(currentIndex, match.range.first))
            }
        }

        // Append bold text
        withStyle(
            style = SpanStyle(
                fontSize = baseSize,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(match.groupValues[1])
        }

        currentIndex = match.range.last + 1
    }

    // Append any remaining text
    if (currentIndex < line.length) {
        withStyle(
            style = SpanStyle(
                fontSize = baseSize
            )
        ) {
            append(line.substring(currentIndex))
        }
    }
    append("\n")
}

private fun AnnotatedString.Builder.processItalicText(line: String, baseSize: TextUnit) {
    val regex = "\\*(.*?)\\*".toRegex()
    var currentIndex = 0
    val matches = regex.findAll(line)

    for (match in matches) {
        // Append text before the italic section
        if (match.range.first > currentIndex) {
            withStyle(
                style = SpanStyle(
                    fontSize = baseSize
                )
            ) {
                append(line.substring(currentIndex, match.range.first))
            }
        }

        // Append italic text
        withStyle(
            style = SpanStyle(
                fontSize = baseSize,
                fontStyle = FontStyle.Italic
            )
        ) {
            append(match.groupValues[1])
        }

        currentIndex = match.range.last + 1
    }

    // Append any remaining text
    if (currentIndex < line.length) {
        withStyle(
            style = SpanStyle(
                fontSize = baseSize
            )
        ) {
            append(line.substring(currentIndex))
        }
    }
    append("\n")
}
// Helper function for processing bold text
private fun androidx.compose.ui.text.AnnotatedString.Builder.processBoldText(line: String) {
    val boldRegex = """\*\*(.*?)\*\*""".toRegex()
    val matches = boldRegex.findAll(line).toList()

    var lastEnd = 0
    for (match in matches) {
        append(line.substring(lastEnd, match.range.first))
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        append(match.groupValues[1])
        pop()
        lastEnd = match.range.last + 1
    }
    append(line.substring(lastEnd))
    append("\n")
}

// Helper function for processing italic text
private fun androidx.compose.ui.text.AnnotatedString.Builder.processItalicText(line: String) {
    val italicRegex = """\*(.*?)\*""".toRegex()
    val matches = italicRegex.findAll(line).toList()

    var lastEnd = 0
    for (match in matches) {
        append(line.substring(lastEnd, match.range.first))
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
        append(match.groupValues[1])
        pop()
        lastEnd = match.range.last + 1
    }
    append(line.substring(lastEnd))
    append("\n")
}

// For preview in Android Studio
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewTextFileScreen() {
    MaterialTheme {
        TextFileScreen(
            courseName = "electrical",
            subjectName = "network_analysis_synthesis",
            chapterName = "chapter_twoport",
            fileName = "1.txt",
            onBackPressed = { /* Preview - no action */ }
        )
    }
}