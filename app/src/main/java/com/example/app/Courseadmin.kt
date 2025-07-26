package com.arjundubey.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class CardType {
    URL_CARD,
    CONTAINER_CARD
}

data class TextCard(
    val id: String = "",
    val title: String = "",
    val type: CardType = CardType.URL_CARD,
    val urls: List<String> = emptyList(),
    val childCards: List<String> = emptyList(), // IDs of child cards
    val parentId: String? = null // ID of parent card, null for root cards
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: TextCard,
    onDismiss: () -> Unit,
    onConfirm: (String, CardType, List<String>) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var selectedType by remember { mutableStateOf(card.type) }
    var urlsText by remember { mutableStateOf(card.urls.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card type selection
                Text(
                    text = "Card Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = CardType.URL_CARD },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("URL Card")
                            }
                        },
                        selected = selectedType == CardType.URL_CARD
                    )

                    FilterChip(
                        onClick = { selectedType = CardType.CONTAINER_CARD },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Container")
                            }
                        },
                        selected = selectedType == CardType.CONTAINER_CARD
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show URL input only for URL cards
                if (selectedType == CardType.URL_CARD) {
                    OutlinedTextField(
                        value = urlsText,
                        onValueChange = { urlsText = it },
                        label = { Text("URLs (one per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 6
                    )

                    Text(
                        text = "Enter each URL on a new line",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Container cards can hold other cards. Add child cards after saving.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val urls = if (selectedType == CardType.URL_CARD && urlsText.isNotBlank()) {
                            urlsText.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .map { url ->
                                    if (!url.startsWith("http")) "https://raw.githubusercontent.com/0akd/$url" else url
                                }
                        } else {
                            emptyList()
                        }

                        onConfirm(title, selectedType, urls)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminScreen() {
    val firestore = FirebaseFirestore.getInstance()
    var allCards by remember { mutableStateOf<List<TextCard>>(emptyList()) }
    var currentParentId by remember { mutableStateOf<String?>(null) }
    var navigationStack by remember { mutableStateOf<List<Pair<String?, String>>>(listOf(null to "Root")) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<TextCard?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Load cards from Firestore
    LaunchedEffect(Unit) {
        loadCards(firestore) { loadedCards ->
            allCards = loadedCards
        }
    }

    // Get current level cards
    val currentCards = allCards.filter { it.parentId == currentParentId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation breadcrumb
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)

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
                        fontSize = 14.sp
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
                text = "Admin Panel",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text("Back")
                    }
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        var deletingCard by remember { mutableStateOf<TextCard?>(null) }
        var showFirstConfirm by remember { mutableStateOf(false) }
        var showFinalConfirm by remember { mutableStateOf(false) }

// First confirmation (soft warning)
        // First confirmation
        if (showFirstConfirm) {
            AlertDialog(
                onDismissRequest = { showFirstConfirm = false },
                title = { Text("Are you sure?") },
                text = { Text("This will delete '${deletingCard?.title}' and all its children.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFirstConfirm = false
                            showFinalConfirm = true
                        }
                    ) { Text("Continue") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showFirstConfirm = false
                        deletingCard = null
                    }) { Text("Cancel") }
                }
            )
        }

// Final confirmation
        if (showFinalConfirm && deletingCard != null) {
            AlertDialog(
                onDismissRequest = { showFinalConfirm = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to permanently delete '${deletingCard?.title}'? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFinalConfirm = false
                            isLoading = true
                            deleteCardRecursively(firestore, deletingCard!!.id, allCards) {
                                loadCards(firestore) { loadedCards ->
                                    allCards = loadedCards
                                    isLoading = false
                                    deletingCard = null
                                }
                            }
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showFinalConfirm = false
                        deletingCard = null
                    }) { Text("Cancel") }
                }
            )
        }


        // Cards list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentCards) { card ->
                AdminCardItem(
                    card = card,
                    childCount = allCards.count { it.parentId == card.id },
                    onEdit = {
                        editingCard = card
                        showEditDialog = true
                    },
                    onDelete = {
                        deletingCard = card
                        showFirstConfirm = true
                    }


                    ,
                    onNavigate = if (card.type == CardType.CONTAINER_CARD) {
                        {
                            currentParentId = card.id
                            navigationStack = navigationStack + (card.id to card.title)
                        }
                    } else null
                )
            }
        }
    }

    // Add Card Dialog
    if (showAddDialog) {
        AddCardDialog(
            parentId = currentParentId,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, urls ->
                isLoading = true
                addCard(firestore, title, type, urls, currentParentId) {
                    loadCards(firestore) { loadedCards ->
                        allCards = loadedCards
                        isLoading = false
                        showAddDialog = false
                    }
                }
            }
        )
    }

    // Edit Card Dialog
    if (showEditDialog && editingCard != null) {
        EditCardDialog(
            card = editingCard!!,
            onDismiss = {
                showEditDialog = false
                editingCard = null
            },
            onConfirm = { title, type, urls ->
                isLoading = true
                updateCard(firestore, editingCard!!.id, title, type, urls) {
                    loadCards(firestore) { loadedCards ->
                        allCards = loadedCards
                        isLoading = false
                        showEditDialog = false
                        editingCard = null
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCardItem(
    card: TextCard,
    childCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onNavigate ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (card.type == CardType.CONTAINER_CARD)
                                Icons.Default.Folder else Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (card.type == CardType.CONTAINER_CARD)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = card.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        if (onNavigate != null) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Enter folder",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (card.type == CardType.CONTAINER_CARD) {
                        Text(
                            text = "$childCount item(s)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "${card.urls.size} URL(s)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        card.urls.take(3).forEachIndexed { index, url ->
                            Text(
                                text = "${index + 1}. ${url.take(50)}${if (url.length > 50) "..." else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                        if (card.urls.size > 3) {
                            Text(
                                text = "... and ${card.urls.size - 3} more",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCardDialog(
    parentId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, CardType, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CardType.URL_CARD) }
    var urlsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (parentId == null) "Add New Card" else "Add New Child Card")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card type selection
                Text(
                    text = "Card Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = CardType.URL_CARD },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("URL Card")
                            }
                        },
                        selected = selectedType == CardType.URL_CARD
                    )

                    FilterChip(
                        onClick = { selectedType = CardType.CONTAINER_CARD },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Container")
                            }
                        },
                        selected = selectedType == CardType.CONTAINER_CARD
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show URL input only for URL cards
                if (selectedType == CardType.URL_CARD) {
                    OutlinedTextField(
                        value = urlsText,
                        onValueChange = { urlsText = it },
                        label = { Text("URLs (one per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 6
                    )

                    Text(
                        text = "Enter each URL on a new line",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Container cards can hold other cards. Add child cards after saving.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val urls = if (selectedType == CardType.URL_CARD && urlsText.isNotBlank()) {
                            urlsText.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .map { url ->
                                    if (!url.startsWith("http")) "https://raw.githubusercontent.com/0akd/$url" else url
                                }
                        } else {
                            emptyList()
                        }
                        onConfirm(title, selectedType, urls)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Firestore operations
private fun loadCards(
    firestore: FirebaseFirestore,
    onComplete: (List<TextCard>) -> Unit
) {
    firestore.collection("text_cards")
        .get()
        .addOnSuccessListener { documents ->
            val cards = documents.map { document ->
                TextCard(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    type = CardType.valueOf(
                        document.getString("type") ?: CardType.URL_CARD.name
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

private fun addCard(
    firestore: FirebaseFirestore,
    title: String,
    type: CardType,
    urls: List<String>,
    parentId: String?,
    onComplete: () -> Unit
) {
    val cardData = mutableMapOf<String, Any>(
        "title" to title,
        "type" to type.name,
        "urls" to urls,
        "childCards" to emptyList<String>(),
        "createdAt" to System.currentTimeMillis()
    )

    if (parentId != null) {
        cardData["parentId"] = parentId
    }

    firestore.collection("text_cards")
        .add(cardData)
        .addOnSuccessListener { documentRef ->
            // If this card has a parent, update the parent's childCards list
            if (parentId != null) {
                firestore.collection("text_cards")
                    .document(parentId)
                    .get()
                    .addOnSuccessListener { parentDoc ->
                        val currentChildren = parentDoc.get("childCards") as? List<String> ?: emptyList()
                        val updatedChildren = currentChildren + documentRef.id
                        firestore.collection("text_cards")
                            .document(parentId)
                            .update("childCards", updatedChildren)
                            .addOnCompleteListener { onComplete() }
                    }
                    .addOnFailureListener { onComplete() }
            } else {
                onComplete()
            }
        }
        .addOnFailureListener { onComplete() }
}

private fun updateCard(
    firestore: FirebaseFirestore,
    cardId: String,
    title: String,
    type: CardType,
    urls: List<String>,
    onComplete: () -> Unit
) {
    val cardData = mapOf(
        "title" to title,
        "type" to type.name,
        "urls" to urls,
        "updatedAt" to System.currentTimeMillis()
    )

    firestore.collection("text_cards")
        .document(cardId)
        .update(cardData)
        .addOnSuccessListener { onComplete() }
        .addOnFailureListener { onComplete() }
}

private fun deleteCardRecursively(
    firestore: FirebaseFirestore,
    cardId: String,
    allCards: List<TextCard>,
    onComplete: () -> Unit
) {
    // Find all child cards recursively
    fun findAllChildren(parentId: String): List<String> {
        val directChildren = allCards.filter { it.parentId == parentId }.map { it.id }
        val allChildren = mutableListOf<String>()
        allChildren.addAll(directChildren)
        directChildren.forEach { childId ->
            allChildren.addAll(findAllChildren(childId))
        }
        return allChildren
    }

    val allChildIds = findAllChildren(cardId)
    val allIdsToDelete = listOf(cardId) + allChildIds

    // Delete all cards
    val batch = firestore.batch()
    allIdsToDelete.forEach { id ->
        batch.delete(firestore.collection("text_cards").document(id))
    }

    // Remove from parent's childCards list if this card has a parent
    val cardToDelete = allCards.find { it.id == cardId }
    if (cardToDelete?.parentId != null) {
        val parentRef = firestore.collection("text_cards").document(cardToDelete.parentId!!)
        firestore.collection("text_cards")
            .document(cardToDelete.parentId!!)
            .get()
            .addOnSuccessListener { parentDoc ->
                val currentChildren = parentDoc.get("childCards") as? List<String> ?: emptyList()
                val updatedChildren = currentChildren.filter { it != cardId }
                batch.update(parentRef, "childCards", updatedChildren)

                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onComplete() }
            }
    } else {
        batch.commit()
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onComplete() }
    }
}