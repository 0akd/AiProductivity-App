package com.arjundubey.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.net.URL

enum class DisplayCardType {
    URL_CARD,
    CONTAINER_CARD
}

data class DisplayCard(
    val id: String = "",
    val title: String = "",
    val type: DisplayCardType = DisplayCardType.URL_CARD,
    val urls: List<String> = emptyList(),
    val childCards: List<String> = emptyList(),
    val parentId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDisplayScreen() {
    val firestore = FirebaseFirestore.getInstance()
    var allCards by remember { mutableStateOf<List<DisplayCard>>(emptyList()) }
    var currentParentId by remember { mutableStateOf<String?>(null) }
    var navigationStack by remember { mutableStateOf<List<Pair<String?, String>>>(listOf(null to "Home")) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCard by remember { mutableStateOf<DisplayCard?>(null) }
    var urlTexts by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentTextIndex by remember { mutableStateOf(0) }
    var isLoadingText by remember { mutableStateOf(false) }

    // Load cards from Firestore
    LaunchedEffect(Unit) {
        loadDisplayCards(firestore) { loadedCards ->
            allCards = loadedCards
            isLoading = false
        }
    }

    // Get current level cards
    val currentCards = allCards.filter { it.parentId == currentParentId }

    if (selectedCard != null) {
        FullScreenTextView(
            card = selectedCard!!,
            texts = urlTexts,
            currentIndex = currentTextIndex,
            isLoading = isLoadingText,
            onClose = {
                selectedCard = null
                urlTexts = emptyList()
                currentTextIndex = 0
            },
            onNext = {
                if (currentTextIndex < urlTexts.size - 1) {
                    currentTextIndex++
                }
            },
            onPrevious = {
                if (currentTextIndex > 0) {
                    currentTextIndex--
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Navigation breadcrumb
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement =  Arrangement.spacedBy(4.dp)
            ) {
                items(navigationStack.size) { index ->
                    val (_, name) = navigationStack[index]
                    TextButton(
                        onClick = {
                            // Navigate back to this level
                            val newStack = navigationStack.take(index + 1)
                            navigationStack = newStack
                            currentParentId = newStack.last().first
                        }
                    ) {
                        Text(
                            text = if (index < navigationStack.size - 1) "$name >" else name,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Text Cards",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Back button (if not at root)
                if (currentParentId != null) {
                    OutlinedButton(
                        onClick = {
                            if (navigationStack.size > 1) {
                                val newStack = navigationStack.dropLast(1)
                                navigationStack = newStack
                                currentParentId = newStack.last().first
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentCards) { card ->
                        DisplayCardItem(
                            card = card,
                            childCount = allCards.count { it.parentId == card.id },
                            onClick = {
                                if (card.type == DisplayCardType.URL_CARD) {
                                    // Open text view for URL cards
                                    selectedCard = card
                                    isLoadingText = true
                                    currentTextIndex = 0
                                    fetchTextsFromUrls(card.urls) { texts ->
                                        urlTexts = texts
                                        isLoadingText = false
                                    }
                                } else {
                                    // Navigate into container cards
                                    currentParentId = card.id
                                    navigationStack = navigationStack + (card.id to card.title)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayCardItem(
    card: DisplayCard,
    childCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (card.type == DisplayCardType.CONTAINER_CARD)
                            Icons.Default.Folder else Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (card.type == DisplayCardType.CONTAINER_CARD)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )

                    Column {
                        Text(
                            text = card.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (card.type == DisplayCardType.CONTAINER_CARD) {
                            Text(
                                text = "$childCount item(s) inside",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to explore",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "${card.urls.size} text piece(s) available",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to read",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (card.type == DisplayCardType.CONTAINER_CARD) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Enter folder",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenTextView(
    card: DisplayCard,
    texts: List<String>,
    currentIndex: Int,
    isLoading: Boolean,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = card.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (texts.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1} of ${texts.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                if (texts.size > 1) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = currentIndex < texts.size - 1
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading text content...",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else if (texts.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = texts[currentIndex],
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Justify,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Navigation hints
                        if (texts.size > 1) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (currentIndex > 0) {
                                    Card(
                                        modifier = Modifier
                                            .clickable { onPrevious() }
                                            .padding(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            text = "← Previous",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                if (currentIndex < texts.size - 1) {
                                    Card(
                                        modifier = Modifier
                                            .clickable { onNext() }
                                            .padding(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            text = "Next →",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Floating navigation buttons
                    if (texts.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FloatingActionButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(48.dp),
                                containerColor = if (currentIndex > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = if (currentIndex > 0)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${currentIndex + 1} / ${texts.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            FloatingActionButton(
                                onClick = onNext,
                                modifier = Modifier.size(48.dp),
                                containerColor = if (currentIndex < texts.size - 1)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Next",
                                    tint = if (currentIndex < texts.size - 1)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No content available",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Firestore operations
private fun loadDisplayCards(
    firestore: FirebaseFirestore,
    onComplete: (List<DisplayCard>) -> Unit
) {
    firestore.collection("text_cards")
        .orderBy("createdAt")
        .get()
        .addOnSuccessListener { documents ->
            val cards = documents.map { document ->
                DisplayCard(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    type = DisplayCardType.valueOf(
                        document.getString("type") ?: DisplayCardType.URL_CARD.name
                    ),
                    urls = document.get("urls") as? List<String> ?: emptyList(),
                    childCards = document.get("childCards") as? List<String> ?: emptyList(),
                    parentId = document.getString("parentId")
                )
            }
            onComplete(cards)
        }
        .addOnFailureListener {
            onComplete(emptyList())
        }
}

// Function to fetch text content from URLs
private fun fetchTextsFromUrls(
    urls: List<String>,
    onComplete: (List<String>) -> Unit
) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
        val texts = mutableListOf<String>()

        urls.forEach { url ->
            try {
                // For raw GitHub URLs or direct text URLs
                val connection = URL(url).openConnection()
                val content = connection.getInputStream().bufferedReader().use { it.readText() }
                texts.add(content)
            } catch (e: Exception) {
                // If URL fetch fails, add error message
                texts.add("Error loading content from: $url\n\nError: ${e.message}")
            }
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            onComplete(texts)
        }
    }
}