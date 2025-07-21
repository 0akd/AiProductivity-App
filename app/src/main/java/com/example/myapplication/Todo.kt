@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

data class CardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val value: Int = 0
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", 0)
}

class CardRepository(private val context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("card_prefs", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val gson = Gson()

    suspend fun saveCards(cards: List<CardItem>) {
        // Save to SharedPreferences
        val json = gson.toJson(cards)
        sharedPrefs.edit().putString("cards", json).apply()

        // Save to Firestore
        try {
            val batch = firestore.batch()

            // First, delete all existing cards
            val existingCards = firestore.collection("cards").get().await()
            existingCards.documents.forEach { document ->
                batch.delete(document.reference)
            }

            // Then add new cards
            cards.forEach { card ->
                val cardRef = firestore.collection("cards").document(card.id)
                batch.set(cardRef, card)
            }

            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadCards(): List<CardItem> {
        return try {
            // Try to load from Firestore first
            val snapshot = firestore.collection("cards").get().await()
            val firestoreCards = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CardItem::class.java)
            }

            if (firestoreCards.isNotEmpty()) {
                // Save to SharedPreferences as backup
                val json = gson.toJson(firestoreCards)
                sharedPrefs.edit().putString("cards", json).apply()
                firestoreCards
            } else {
                // Fallback to SharedPreferences
                loadFromSharedPrefs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to SharedPreferences
            loadFromSharedPrefs()
        }
    }

    private fun loadFromSharedPrefs(): List<CardItem> {
        val json = sharedPrefs.getString("cards", null)
        return if (json != null) {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun deleteCard(cardId: String) {
        try {
            firestore.collection("cards").document(cardId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun CardListManager() {
    val context = LocalContext.current
    val repository = remember { CardRepository(context) }
    val scope = rememberCoroutineScope()

    var cards by remember { mutableStateOf(listOf<CardItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<CardItem?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<CardItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Load cards on startup
    LaunchedEffect(Unit) {
        scope.launch {
            cards = repository.loadCards()
            isLoading = false
        }
    }

    // Save cards whenever the list changes (but don't save on initial load)
    LaunchedEffect(cards) {
        if (!isLoading && cards.isNotEmpty()) {
            scope.launch {
                repository.saveCards(cards)
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF6200EE))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with title and buttons





        // Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards, key = { it.id }) { card ->
                SwipeableCard(
                    card = card,
                    getCurrentValue = { cards.find { it.id == card.id }?.value ?: card.value },
                    onValueChanged = { newValue ->
                        cards = cards.map { c ->
                            if (c.id == card.id) c.copy(value = newValue) else c
                        }
                    },
                    onCardClick = {
                        selectedCard = card
                        showDetailDialog = true
                    },
                    onDeleteRequest = {
                        cardToDelete = card
                        showDeleteDialog = true
                    },
                    onEdit = { updatedCard ->
                        cards = cards.map { c ->
                            if (c.id == updatedCard.id) updatedCard else c
                        }
                    }
                )

            }
        }

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No cards yet!",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Tap + to add your first card",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        scope.launch {
                            cards = cards.map { it.copy(value = 0) }
                            repository.saveCards(cards)
                        }
                    },
                    modifier = Modifier
                        .background(
                            Color(0xFF6200EE),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }

                // Add Button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF03DAC6),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Card",
                        tint = Color.White
                    )
                }
            }
        }

    }

    // Add Card Dialog
    if (showAddDialog) {
        AddEditCardDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, description ->
                cards = cards + CardItem(name = name, description = description)
                showAddDialog = false
            }
        )
    }

    // Card Detail Dialog
    if (showDetailDialog && selectedCard != null) {
        // Make sure we're showing the current version of the card
        val currentSelectedCard = cards.find { it.id == selectedCard!!.id } ?: selectedCard!!
        CardDetailDialog(
            card = currentSelectedCard,
            onDismiss = { showDetailDialog = false },
            onEdit = { card ->
                selectedCard = card
                showDetailDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && cardToDelete != null) {
        DeleteConfirmationDialog(
            cardName = cardToDelete!!.name,
            onConfirm = {
                scope.launch {
                    repository.deleteCard(cardToDelete!!.id)
                    cards = cards.filter { it.id != cardToDelete!!.id }
                    cardToDelete = null
                    showDeleteDialog = false
                }
            },
            onDismiss = {
                cardToDelete = null
                showDeleteDialog = false
            }
        )
    }
}
// Alternative approach: Modify the SwipeableCard to get current value dynamically
@Composable
fun SwipeableCard(
    card: CardItem,
    getCurrentValue: () -> Int, // New parameter to get the current value
    onValueChanged: (Int) -> Unit,
    onCardClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onEdit: (CardItem) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }

    // Use the dynamic current value instead of the card.value
    val currentValue = getCurrentValue()
    val cardColor = getCardColor(currentValue)
    val animatedColor by animateColorAsState(
        targetValue = cardColor,
        label = "cardColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        label = "cardScale"
    )

    // Calculate preview value based on drag using current value
    val previewValue = when {
        dragOffset > swipeThreshold && currentValue < 5 -> currentValue + 1
        dragOffset < -swipeThreshold && currentValue > 0 -> currentValue - 1
        else -> currentValue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer(
                translationX = dragOffset,
                rotationZ = dragOffset * 0.01f
            )
            .pointerInput(card.id, currentValue) { // Add currentValue as key
                detectDragGestures(
                    onDragStart = { _ ->
                        isDragging = true
                    },
                    onDragEnd = {
                        val absOffset = abs(dragOffset)
                        val latestValue = getCurrentValue() // Get the very latest value

                        if (absOffset > swipeThreshold) {
                            val newValue = when {
                                dragOffset > 0 && latestValue < 5 -> {
                                    println("Incrementing: $latestValue -> ${latestValue + 1}")
                                    latestValue + 1
                                }
                                dragOffset < 0 && latestValue > 0 -> {
                                    println("Decrementing: $latestValue -> ${latestValue - 1}")
                                    latestValue - 1
                                }
                                else -> latestValue
                            }

                            if (newValue != latestValue) {
                                onValueChanged(newValue)
                            }
                        }

                        dragOffset = 0f
                        isDragging = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val currentVal = getCurrentValue()
                    val newOffset = dragOffset + dragAmount.x

                    dragOffset = when {
                        newOffset > 0 && currentVal >= 5 -> newOffset * 0.2f
                        newOffset < 0 && currentVal <= 0 -> newOffset * 0.2f
                        else -> newOffset.coerceIn(-200f, 200f)
                    }
                }
            }
            .clickable(enabled = !isDragging && abs(dragOffset) < 10f) {
                onCardClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging && previewValue != currentValue) {
                getCardColor(previewValue).copy(alpha = 0.9f)
            } else {
                animatedColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 12.dp else 8.dp
        )
    ) {
        // Rest of your UI code using currentValue instead of card.value
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)

        ) {
            // First Row: Card Name + Icon Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,

                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                if (previewValue > 2) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .graphicsLayer {
                                scaleX = if (isDragging) 1.2f else 1f
                                scaleY = if (isDragging) 1.2f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewValue.toString(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (previewValue > 2) Color.White else Color.Black
                        )
                    }

                    IconButton(
                        onClick = { showEditDialog = true },
                        enabled = !isDragging
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (previewValue > 2) Color.White else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onDeleteRequest,
                        enabled = !isDragging
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (previewValue > 2) Color.White else Color.Red
                        )
                    }
                }
            }

            // Second Row: Description and Status
            Column(

            ) {
                Text(
                    text = card.description
                        .replace("\n", " ")        // Replace newlines with space
                        .replace("\t", " ")
                        .replace(Regex("\\s+"), " ")  // Collapse multiple whitespace
                        .trim()// Replace tabs with space
                        .trimStart()               // Remove leading whitespace
                        .take(50) + if (card.description.length > 50) "..." else "",
                    fontSize = 10.sp,
                    color = if (previewValue > 2) Color.White.copy(alpha = 0.8f) else Color.Gray
                )



            }
        }


        // Drag indicators using currentValue
        if (isDragging && abs(dragOffset) > swipeThreshold / 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (dragOffset < 0 && currentValue > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(35.dp)
                            .background(
                                if (abs(dragOffset) > swipeThreshold) Color.Red else Color.Red.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentValue - 1}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (dragOffset > 0 && currentValue < 5) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(35.dp)
                            .background(
                                if (abs(dragOffset) > swipeThreshold) Color.Green else Color.Green.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentValue + 1}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AddEditCardDialog(
            card = card,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                onEdit(card.copy(name = name, description = description))
                showEditDialog = false
            }
        )
    }
}

// Then in your CardListManager, call it like this:
/*
SwipeableCard(
    card = currentCard,
    getCurrentValue = { cards.find { it.id == currentCard.id }?.value ?: currentCard.value },
    onValueChanged = { newValue ->
        cards = cards.map { c ->
            if (c.id == currentCard.id) c.copy(value = newValue) else c
        }
    },
    // ... other parameters
)
*/
    // Reset animation and drag offset


@Composable
fun DeleteConfirmationDialog(
    cardName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = Color.Red
            )
        },
        title = {
            Text(
                text = "Delete Card",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to delete \"$cardName\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete", color = Color.White)
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
fun AddEditCardDialog(
    card: CardItem?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(card?.name ?: "") }
    var description by remember { mutableStateOf(card?.description ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (card == null) "Add New Card" else "Edit Card",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, description)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun CardDetailDialog(
    card: CardItem,
    onDismiss: () -> Unit,
    onEdit: (CardItem) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = getCardColor(card.value))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.value > 2) Color.White else Color.Black
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (card.value > 2) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.value.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (card.value > 2) Color.White else Color.Black
                        )
                    }
                }

                Text(
                    text = "Description:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (card.value > 2) Color.White else Color.Black
                )

                Text(
                    text = card.description.ifEmpty { "No description provided" },
                    fontSize = 14.sp,
                    color = if (card.value > 2) Color.White.copy(alpha = 0.9f) else Color.Gray,
                    textAlign = TextAlign.Justify
                )

                Text(
                    text = when {
                        card.value == 0 -> "💡 Swipe right to increment value"
                        card.value == 5 -> "💡 Swipe left to decrement value"
                        else -> "💡 Swipe left/right to change value (0-5)"
                    },
                    fontSize = 12.sp,
                    color = if (card.value > 2) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (card.value > 2) Color.White else Color(0xFF6200EE),
                        contentColor = if (card.value > 2) Color.Black else Color.White
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

fun getCardColor(value: Int): Color {
    return when (value) {
        0 -> Color(0xFFFF6360) // Light Red
        1 -> Color(0xFFFF8c89) //
        2 -> Color(0xFFFFAEAC) // Orange
        3 -> Color(0xFFFFFF66) // Amber
        4 -> Color(0xFF00FF7F) // Green
        5 -> Color(0xFFACFDFF) // Blue
        else -> Color.Gray
    }
}