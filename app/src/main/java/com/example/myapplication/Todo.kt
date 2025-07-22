@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.myapplication
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import androidx.compose.material.icons.filled.Error
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
data class CardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val value: Int = 0,
    val priority: Int,
    val youtubeUrl: String = ""
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", 0,0,"")
}
fun extractYouTubeVideoId(url: String): String? {
    val patterns = listOf(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([^&\\n?#]+)",
        "youtube\\.com/watch\\?.*v=([^&\\n?#]+)"
    )

    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
}

fun getYouTubeEmbedUrl(videoId: String): String {
    return "https://www.youtube.com/embed/$videoId"
}

fun isValidYouTubeUrl(url: String): Boolean {
    return extractYouTubeVideoId(url) != null
}
class CardNotificationManager(private val context: Context) {
    companion object {
        const val WORK_NAME = "card_notification_work"
    }

    fun startNotifications() {
        // Cancel existing work first
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        val workRequest = PeriodicWorkRequestBuilder<CardNotificationWorker>(
            15, TimeUnit.MINUTES // Changed to 15 minutes (minimum for periodic work)
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresDeviceIdle(false)
                    .build()
            )
            .setInitialDelay(10, TimeUnit.SECONDS) // Start after 10 seconds
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

        Log.d("CardNotificationManager", "Notification work scheduled")
    }

    fun stopNotifications() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME)
    }

    fun isNotificationScheduled(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)

        return try {
            val workInfo = workInfos.get()
            val isScheduled = workInfo.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
            Log.d("CardNotificationManager", "Is notification scheduled: $isScheduled")
            isScheduled
        } catch (e: Exception) {
            Log.e("CardNotificationManager", "Error checking notification status", e)
            false
        }
    }

    // Add method to test notification immediately
    fun testNotificationNow() {
        val repository = CardRepository(context)
        val notificationHelper = NotificationHelper(context)
        val cards = repository.loadFromSharedPrefs()

        if (cards.isNotEmpty()) {
            notificationHelper.sendRandomCardNotification(cards)
            Log.d("CardNotificationManager", "Test notification sent")
        } else {
            Log.d("CardNotificationManager", "No cards available for notification")
        }
    }
}

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "card_notifications"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Card Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for random cards"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Notification channel created")
        }
    }

    fun sendRandomCardNotification(cards: List<CardItem>) {
        Log.d("NotificationHelper", "Attempting to send notification. Cards count: ${cards.size}")

        if (cards.isEmpty()) {
            Log.d("NotificationHelper", "No cards available")
            return
        }

        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w("NotificationHelper", "Notifications are disabled in system settings")
            return
        }

        val randomCard = cards.random()
        Log.d("NotificationHelper", "Selected card: ${randomCard.name}")

        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle("📋 ${randomCard.name}")
            .setContentText(getNotificationMessage(randomCard))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${getNotificationMessage(randomCard)}\n\n💡 Tap to open app"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(100, 200, 100))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, notification)
                Log.d("NotificationHelper", "Notification sent successfully")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error sending notification", e)
        }
    }

    private fun getNotificationMessage(card: CardItem): String {
        return when (card.value) {
            0 -> "🔴 Needs attention! Current level: ${card.value}/5"
            1 -> "🟠 Low progress. Current level: ${card.value}/5"
            2 -> "🟡 Making progress. Current level: ${card.value}/5"
            3 -> "🟢 Good progress! Current level: ${card.value}/5"
            4 -> "🔵 Almost there! Current level: ${card.value}/5"
            5 -> "⭐ Excellent! Completed: ${card.value}/5"
            else -> "Current level: ${card.value}/5"
        }
    }
}

class CardNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("CardNotificationWorker", "Worker started")

        return try {
            val repository = CardRepository(applicationContext)
            val notificationHelper = NotificationHelper(applicationContext)

            // Get cards from SharedPreferences (for background access)
            val cards = repository.loadFromSharedPrefs()
            Log.d("CardNotificationWorker", "Loaded ${cards.size} cards")

            if (cards.isNotEmpty()) {
                notificationHelper.sendRandomCardNotification(cards)
                Log.d("CardNotificationWorker", "Notification work completed successfully")
            } else {
                Log.d("CardNotificationWorker", "No cards available for notification")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CardNotificationWorker", "Worker failed", e)
            e.printStackTrace()
            Result.failure()
        }
    }
}

// Optional: Boot receiver to restart notifications after device reboot
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {

            val notificationManager = CardNotificationManager(context)
            notificationManager.startNotifications()
            Log.d("BootReceiver", "Notifications restarted after boot")
        }
    }
}

class CardRepository(private val context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("card_prefs", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val gson = Gson()
    private val collectionName = "cards"

    // Create a new card
    suspend fun createCard(card: CardItem): Boolean {
        return try {
            // Save to Firestore
            firestore.collection(collectionName)
                .document(card.id)
                .set(card)
                .await()

            // Update local backup
            val currentCards = loadFromSharedPrefs().toMutableList()
            currentCards.add(card)
            saveToSharedPrefs(currentCards)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Read all cards
    suspend fun loadCards(): List<CardItem> {
        return try {
            // Try to load from Firestore first
            val snapshot = firestore.collection(collectionName)
                .orderBy("priority")
                .get()
                .await()

            val firestoreCards = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CardItem::class.java)?.copy(id = doc.id)
            }

            if (firestoreCards.isNotEmpty()) {
                // Save to SharedPreferences as backup
                saveToSharedPrefs(firestoreCards)
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

    // Update an existing card
    suspend fun updateCard(card: CardItem): Boolean {
        return try {
            firestore.collection(collectionName)
                .document(card.id)
                .set(card)
                .await()

            // Update local backup
            val currentCards = loadFromSharedPrefs().toMutableList()
            val index = currentCards.indexOfFirst { it.id == card.id }
            if (index != -1) {
                currentCards[index] = card
                saveToSharedPrefs(currentCards)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Delete a card
    suspend fun deleteCard(cardId: String): Boolean {
        return try {
            firestore.collection(collectionName)
                .document(cardId)
                .delete()
                .await()

            // Update local backup
            val currentCards = loadFromSharedPrefs().toMutableList()
            currentCards.removeAll { it.id == cardId }
            saveToSharedPrefs(currentCards)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Bulk update cards (for saving all cards at once)
    suspend fun saveCards(cards: List<CardItem>): Boolean {
        return try {
            val batch = firestore.batch()

            // Update each card in the batch
            cards.forEach { card ->
                val cardRef = firestore.collection(collectionName).document(card.id)
                batch.set(cardRef, card)
            }

            batch.commit().await()

            // Save to SharedPreferences as backup
            saveToSharedPrefs(cards)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Reset all card values to 0
    suspend fun resetAllCardValues(): Boolean {
        return try {
            val cards = loadCards()
            val resetCards = cards.map { it.copy(value = 0) }
            saveCards(resetCards)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Private helper methods
    private fun saveToSharedPrefs(cards: List<CardItem>) {
        val json = gson.toJson(cards)
        sharedPrefs.edit().putString("cards", json).apply()
    }

    fun loadFromSharedPrefs(): List<CardItem> {
        val json = sharedPrefs.getString("cards", null)
        return if (json != null) {
            val type = object : TypeToken<List<CardItem>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // Sync local data with Firestore (useful for offline-online sync)
    suspend fun syncWithFirestore(): List<CardItem> {
        return try {
            val firestoreCards = firestore.collection(collectionName)
                .orderBy("priority")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(CardItem::class.java)?.copy(id = doc.id)
                }

            saveToSharedPrefs(firestoreCards)
            firestoreCards
        } catch (e: Exception) {
            e.printStackTrace()
            loadFromSharedPrefs()
        }
    }
    suspend fun updateCardPriorities(cards: List<CardItem>): Boolean {
        return try {
            val batch = firestore.batch()

            // Update priorities for all cards
            cards.forEachIndexed { index, card ->
                val updatedCard = card.copy(priority = index)
                val cardRef = firestore.collection(collectionName).document(card.id)
                batch.set(cardRef, updatedCard)
            }

            batch.commit().await()

            // Save to SharedPreferences as backup
            val cardsWithUpdatedPriorities = cards.mapIndexed { index, card ->
                card.copy(priority = index)
            }
            saveToSharedPrefs(cardsWithUpdatedPriorities)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun CardListManager() {
    val context = LocalContext.current
    val repository = remember { CardRepository(context) }
    val scope = rememberCoroutineScope()
    val notificationManager = remember { CardNotificationManager(context) }
    var cards by remember { mutableStateOf(listOf<CardItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<CardItem?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<CardItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isOperationInProgress by remember { mutableStateOf(false) }
    var draggedCard by remember { mutableStateOf<CardItem?>(null) }
    var draggedOverIndex by remember { mutableIntStateOf(-1) }
    // Load cards on startup
    LaunchedEffect(Unit) {
        scope.launch {
            cards = repository.loadCards()
            isLoading = false
        }
    }
    var isNotificationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isNotificationEnabled = notificationManager.isNotificationScheduled()
        scope.launch {
            cards = repository.loadCards().sortedBy { it.priority }
            isLoading = false
        }
    }
    // Auto-save function for value changes
    fun saveCardValue(updatedCard: CardItem) {
        if (!isOperationInProgress) {
            scope.launch {
                isOperationInProgress = true
                val success = repository.updateCard(updatedCard)
                if (!success) {
                    // Handle error - maybe show a snackbar
                    println("Failed to save card value to Firestore")
                }
                isOperationInProgress = false
            }
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
    fun reorderCards(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        scope.launch {
            val reorderedCards = cards.toMutableList()
            val draggedCard = reorderedCards.removeAt(fromIndex)
            reorderedCards.add(toIndex, draggedCard)

            // Update local state immediately
            cards = reorderedCards

            // Save to Firebase and SharedPreferences
            val success = repository.updateCardPriorities(reorderedCards)
            if (!success) {
                println("Failed to save card order")
                // Optionally revert the order on failure
                cards = repository.loadCards().sortedBy { it.priority }
            }
        }
    }

    // Use Box with proper positioning instead of Column
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp) // Add bottom padding for buttons
        ) {
            // Header with title and buttons (if you have any)

            // Cards List or Empty State
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
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards.size) { index ->
                        val card = cards[index]

                        DraggableSwipeableCard(
                            card = card,
                            index = index,
                            getCurrentValue = { cards.find { it.id == card.id }?.value ?: card.value },
                            onValueChanged = { newValue ->
                                val updatedCard = card.copy(value = newValue)
                                cards = cards.map { c ->
                                    if (c.id == card.id) updatedCard else c
                                }
                                saveCardValue(updatedCard)
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
                                scope.launch {
                                    repository.updateCard(updatedCard)
                                }
                            },
                            onDragStart = { draggedCard = card },
                            onDragEnd = {
                                draggedCard = null
                                draggedOverIndex = -1
                            },
                            onDragOver = { draggedOverIndex = index },
                            onReorder = { fromIndex, toIndex ->
                                reorderCards(fromIndex, toIndex)
                            },
                            isDraggedOver = draggedOverIndex == index,
                            isDragging = draggedCard?.id == card.id
                        )
                    }
                }
            }
        }





        // Fixed positioned buttons at bottom right
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    if (isNotificationEnabled) {
                        notificationManager.stopNotifications()
                        isNotificationEnabled = false
                    } else {
                        notificationManager.startNotifications()
                        isNotificationEnabled = true
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isNotificationEnabled) Color(0xFF4CAF50) else Color(0xFF757575),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isNotificationEnabled)
                        Icons.Default.Notifications
                    else
                        Icons.Default.NotificationsOff,
                    contentDescription = if (isNotificationEnabled) "Stop Notifications" else "Start Notifications",
                    tint = Color.White
                )
            }
            // Reset Button
            IconButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .size(48.dp)
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

    // Dialogs remain the same...

    // Reset Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reset All Cards") },
            text = { Text("Are you sure you want to reset all card values to 0?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isOperationInProgress = true
                            val success = repository.resetAllCardValues()
                            if (success) {
                                cards = cards.map { it.copy(value = 0) }
                            } else {
                                // Handle error
                                println("Failed to reset card values")
                            }
                            isOperationInProgress = false
                        }
                        showConfirmDialog = false
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    // Add Card Dialog
    if (showAddDialog) {
        AddEditCardDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, description, youtubeUrl ->
                scope.launch {
                    val newCard = CardItem(
                        name = name,
                        description = description,
                        youtubeUrl = youtubeUrl,
                        priority = cards.size
                    )
                    val success = repository.createCard(newCard)
                    if (success) {
                        cards = cards + newCard
                    } else {
                        println("Failed to create card")
                    }
                }
                showAddDialog = false
            }
        )
    }


    // Card Detail Dialog
    if (showDetailDialog && selectedCard != null) {
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
                    val success = repository.deleteCard(cardToDelete!!.id)
                    if (success) {
                        cards = cards.filter { it.id != cardToDelete!!.id }
                    } else {
                        // Handle error
                        println("Failed to delete card")
                    }
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
fun DraggableSwipeableCard(
    card: CardItem,
    index: Int,
    getCurrentValue: () -> Int,
    onValueChanged: (Int) -> Unit,
    onCardClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onEdit: (CardItem) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragOver: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    isDraggedOver: Boolean,
    isDragging: Boolean
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isHorizontalDragging by remember { mutableStateOf(false) }
    var isVerticalDragging by remember { mutableStateOf(false) }
    var startIndex by remember { mutableIntStateOf(-1) }

    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }
    val reorderThreshold = with(density) { 60.dp.toPx() }

    val currentValue = getCurrentValue()
    val cardColor = getCardColor(currentValue)
    val animatedColor by animateColorAsState(
        targetValue = cardColor,
        label = "cardColor"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.05f
            isDraggedOver -> 0.95f
            else -> 1f
        },
        label = "cardScale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else 8f,
        label = "cardElevation"
    )

    val previewValue = when {
        horizontalDragOffset > swipeThreshold && currentValue < 5 -> currentValue + 1
        horizontalDragOffset < -swipeThreshold && currentValue > 0 -> currentValue - 1
        else -> currentValue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer(
                translationX = horizontalDragOffset,
                translationY = verticalDragOffset,
                rotationZ = horizontalDragOffset * 0.01f,
                alpha = if (isDragging) 0.9f else 1f
            )
            .pointerInput(card.id, currentValue) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startIndex = index
                        onDragStart()
                    },
                    onDragEnd = {
                        val absHorizontalOffset = abs(horizontalDragOffset)
                        val absVerticalOffset = abs(verticalDragOffset)

                        if (isVerticalDragging) {
                            // Handle reordering
                            val draggedDistance = verticalDragOffset
                            val cardHeight = 120.dp.toPx() // Approximate card height
                            val positionsToMove = (draggedDistance / cardHeight).toInt()
                            val targetIndex = (startIndex + positionsToMove).coerceIn(0, 10) // Adjust max as needed

                            if (targetIndex != startIndex) {
                                onReorder(startIndex, targetIndex)
                            }
                        } else if (isHorizontalDragging) {
                            // Handle value change
                            val latestValue = getCurrentValue()

                            if (absHorizontalOffset > swipeThreshold) {
                                val newValue = when {
                                    horizontalDragOffset > 0 && latestValue < 5 -> latestValue + 1
                                    horizontalDragOffset < 0 && latestValue > 0 -> latestValue - 1
                                    else -> latestValue
                                }

                                if (newValue != latestValue) {
                                    onValueChanged(newValue)
                                }
                            }
                        }

                        // Reset states
                        horizontalDragOffset = 0f
                        verticalDragOffset = 0f
                        isHorizontalDragging = false
                        isVerticalDragging = false
                        onDragEnd()
                    }
                ) { change, dragAmount ->
                    change.consume()

                    // Determine drag direction based on initial movement
                    if (!isHorizontalDragging && !isVerticalDragging) {
                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            isHorizontalDragging = true
                        } else {
                            isVerticalDragging = true
                            onDragOver()
                        }
                    }

                    if (isHorizontalDragging) {
                        val currentVal = getCurrentValue()
                        val newOffset = horizontalDragOffset + dragAmount.x

                        horizontalDragOffset = when {
                            newOffset > 0 && currentVal >= 5 -> newOffset * 0.2f
                            newOffset < 0 && currentVal <= 0 -> newOffset * 0.2f
                            else -> newOffset.coerceIn(-200f, 200f)
                        }
                    } else if (isVerticalDragging) {
                        verticalDragOffset += dragAmount.y
                    }
                }
            }
            .clickable(enabled = !isHorizontalDragging && !isVerticalDragging && abs(horizontalDragOffset) < 10f && abs(verticalDragOffset) < 10f) {
                onCardClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDraggedOver -> animatedColor.copy(alpha = 0.7f)
                isHorizontalDragging && previewValue != currentValue -> getCardColor(previewValue).copy(alpha = 0.9f)
                else -> animatedColor
            }
        ),

    ) {
        // Your existing card content here - keep all the existing UI code from SwipeableCard
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
                                scaleX = if (isHorizontalDragging) 1.2f else 1f
                                scaleY = if (isHorizontalDragging) 1.2f else 1f
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
                        enabled = !isHorizontalDragging && !isVerticalDragging
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (previewValue > 2) Color.White else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onDeleteRequest,
                        enabled = !isHorizontalDragging && !isVerticalDragging
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (previewValue > 2) Color.White else Color.Red
                        )
                    }
                }
            }

            // Second Row: Description
            Column {
                Text(
                    text = card.description
                        .replace("\n", " ")
                        .replace("\t", " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(50) + if (card.description.length > 50) "..." else "",
                    fontSize = 10.sp,
                    color = if (previewValue > 2) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }
        }

        // Horizontal drag indicators
        if (isHorizontalDragging && abs(horizontalDragOffset) > swipeThreshold / 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (horizontalDragOffset < 0 && currentValue > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(35.dp)
                            .background(
                                if (abs(horizontalDragOffset) > swipeThreshold) Color.Red else Color.Red.copy(alpha = 0.5f),
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

                if (horizontalDragOffset > 0 && currentValue < 5) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(35.dp)
                            .background(
                                if (abs(horizontalDragOffset) > swipeThreshold) Color.Green else Color.Green.copy(alpha = 0.5f),
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

        // Vertical drag indicator
        if (isVerticalDragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color.Blue.copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⇕ Reordering",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AddEditCardDialog(
            card = card,
            onDismiss = { showEditDialog = false },
            onSave = { name, description, _ ->
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
    onSave: (String, String, String) -> Unit // Added YouTube URL parameter
) {
    var name by remember { mutableStateOf(card?.name ?: "") }
    var description by remember { mutableStateOf(card?.description ?: "") }
    var youtubeUrl by remember { mutableStateOf(card?.youtubeUrl ?: "") }
    var isYouTubeUrlValid by remember { mutableStateOf(true) }

    // Validate YouTube URL when it changes
    LaunchedEffect(youtubeUrl) {
        isYouTubeUrlValid = youtubeUrl.isEmpty() || isValidYouTubeUrl(youtubeUrl)
    }

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

                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = { youtubeUrl = it },
                    label = { Text("YouTube URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isYouTubeUrlValid,
                    supportingText = {
                        if (!isYouTubeUrlValid) {
                            Text(
                                text = "Please enter a valid YouTube URL",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (youtubeUrl.isNotEmpty()) {
                            Text(
                                text = "✓ Valid YouTube URL",
                                color = Color.Green
                            )
                        }
                    },
                    trailingIcon = {
                        if (youtubeUrl.isNotEmpty()) {
                            if (isYouTubeUrlValid) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid URL",
                                    tint = Color.Green
                                )
                            } else {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Invalid URL",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )

                if (youtubeUrl.isNotEmpty()) {
                    Text(
                        text = "💡 Supported formats:\n• youtube.com/watch?v=VIDEO_ID\n• youtu.be/VIDEO_ID\n• youtube.com/embed/VIDEO_ID",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

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
                            if (name.isNotBlank() && isYouTubeUrlValid) {
                                onSave(name, description, youtubeUrl)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && isYouTubeUrlValid
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Updated CardDetailDialog with YouTube video embedding
// Updated CardDetailDialog with YouTube video embedding
@Composable
fun CardDetailDialog(
    card: CardItem,
    onDismiss: () -> Unit,
    onEdit: (CardItem) -> Unit
) {
    val videoId = remember(card.youtubeUrl) {
        if (card.youtubeUrl.isNotEmpty()) extractYouTubeVideoId(card.youtubeUrl) else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)  // Allow more height for video
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = getCardColor(card.value))
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (card.value > 2) Color.White else Color.Black,
                            modifier = Modifier.weight(1f)
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
                }

                if (card.description.isNotEmpty()) {
                    item {
                        Text(
                            text = "Description:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (card.value > 2) Color.White else Color.Black
                        )

                        Text(
                            text = card.description,
                            fontSize = 14.sp,
                            color = if (card.value > 2) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            textAlign = TextAlign.Justify
                        )
                    }
                }

                // YouTube Video Section - FIXED VERSION
                if (videoId != null) {
                    item {
                        Text(
                            text = "Related Video:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (card.value > 2) Color.White else Color.Black
                        )
                        val context = LocalContext.current
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = {
                                    WebView(context).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            builtInZoomControls = false
                                            displayZoomControls = false
                                            userAgentString =
                                                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                                        }

                                        // ✅ Required for video playback
                                        webChromeClient = WebChromeClient()

                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.setAcceptCookie(true)
                                        cookieManager.setAcceptThirdPartyCookies(this, true)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            cookieManager.flush()
                                        }
                                    }
                                },
                                update = { webView ->
                                    // YouTube embed URL
                                    webView.loadUrl("https://www.youtube.com/embed/$videoId")
                                }
                            )
                        }
                    }
                }

                item {
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
                }

                item {
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