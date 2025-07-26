@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.arjundubey.app
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import kotlinx.coroutines.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.geometry.Offset
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.delay

import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs
data class CardItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val check: Boolean =false,
    val description: String,
    val value: Int = 0,
    val priority: Int,
    val youtubeUrl: String = "",
    val userEmail: String = "",
    val disableHorizontalDrag: Boolean = true,
    val counter:Int=0,
    val timerValue: Long=0,
) {
    // Empty constructor for Firestore
    constructor() : this("", "", false,"", 0, 0, "", "",true,0,0)
}


object UserAuthHelper {

    /**
     * Get the current authenticated user's email
     * @return User email if authenticated, null otherwise
     */
    fun getCurrentUserEmail(): String? {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.email
        } catch (e: Exception) {
            println("Error getting current user email: ${e.message}")
            null
        }
    }

    /**
     * Get the current authenticated user's UID
     * @return User UID if authenticated, null otherwise
     */
    fun getCurrentUserId(): String? {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid
        } catch (e: Exception) {
            println("Error getting current user ID: ${e.message}")
            null
        }
    }

    /**
     * Check if user is currently authenticated
     * @return true if user is signed in, false otherwise
     */
    fun isUserAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            println("Error checking authentication status: ${e.message}")
            false
        }
    }

    /**
     * Get current user display name
     * @return User display name if available, null otherwise
     */
    fun getCurrentUserDisplayName(): String? {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.displayName
        } catch (e: Exception) {
            println("Error getting current user display name: ${e.message}")
            null
        }
    }
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

    // Get current user email from Firebase Auth
    private fun getCurrentUserEmail(): String? {
        return FirebaseAuth.getInstance()?.currentUser?.email
    }

    // Create a new card with user email
    suspend fun createCard(card: CardItem): Boolean {
        val userEmail = getCurrentUserEmail() ?: return false
        val cardWithUser = card.copy(userEmail = userEmail)

        return try {
            // Save to Firestore
            firestore.collection(collectionName)
                .document(cardWithUser.id)
                .set(cardWithUser)
                .await()

            // Update local backup
            val currentCards = loadFromSharedPrefs().toMutableList()
            currentCards.add(cardWithUser)
            saveToSharedPrefs(currentCards)

            true
        } catch (e: Exception) {
            println("Error creating card: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Read cards only for current user
    suspend fun loadCards(): List<CardItem> {
        val userEmail = getCurrentUserEmail()

        if (userEmail == null) {
            println("No user email found - user not authenticated")
            return emptyList()
        }

        println("Loading cards for user: $userEmail")

        return try {
            // Try to load from Firestore first - filter by user email
            val snapshot = firestore.collection(collectionName)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()

            println("Firestore query completed. Documents found: ${snapshot.documents.size}")

            val firestoreCards = snapshot.documents.mapNotNull { doc ->
                try {
                    val cardData = doc.data
                    println("Document ${doc.id} data: $cardData")

                    doc.toObject(CardItem::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    println("Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }.sortedBy { it.priority }

            println("Successfully converted ${firestoreCards.size} cards from Firestore")

            if (firestoreCards.isNotEmpty()) {
                // Save to SharedPreferences as backup
                saveToSharedPrefs(firestoreCards)
                firestoreCards
            } else {
                println("No cards found in Firestore, checking SharedPreferences")
                // Fallback to SharedPreferences
                val localCards = loadFromSharedPrefs().filter { it.userEmail == userEmail }
                println("Found ${localCards.size} cards in SharedPreferences")
                localCards
            }
        } catch (e: Exception) {
            println("Error loading cards from Firestore: ${e.message}")
            e.printStackTrace()
            // Fallback to SharedPreferences
            val localCards = loadFromSharedPrefs().filter { it.userEmail == userEmail }
            println("Fallback: Found ${localCards.size} cards in SharedPreferences")
            localCards
        }
    }

    // Update an existing card (ensure user owns the card)
    suspend fun updateCard(card: CardItem): Boolean {
        val userEmail = getCurrentUserEmail() ?: return false

        // Ensure the card belongs to current user
        if (card.userEmail != userEmail) {
            println("Card doesn't belong to current user")
            return false
        }

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
            println("Error updating card: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Delete a card (ensure user owns the card)
    suspend fun deleteCard(cardId: String): Boolean {
        val userEmail = getCurrentUserEmail() ?: return false

        return try {
            // First verify the card belongs to current user
            val cardDoc = firestore.collection(collectionName)
                .document(cardId)
                .get()
                .await()

            val card = cardDoc.toObject(CardItem::class.java)
            if (card?.userEmail != userEmail) {
                println("Card doesn't belong to current user or doesn't exist")
                return false
            }

            // Delete from Firestore
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
            println("Error deleting card: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Bulk update cards (for saving all cards at once) - only user's cards
    suspend fun saveCards(cards: List<CardItem>): Boolean {
        val userEmail = getCurrentUserEmail() ?: return false

        // Filter cards to ensure all belong to current user
        val userCards = cards.filter { it.userEmail == userEmail }

        return try {
            val batch = firestore.batch()

            // Update each card in the batch
            userCards.forEach { card ->
                val cardRef = firestore.collection(collectionName).document(card.id)
                batch.set(cardRef, card)
            }

            batch.commit().await()

            // Save to SharedPreferences as backup
            saveToSharedPrefs(userCards)

            true
        } catch (e: Exception) {
            println("Error saving cards: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Reset all card values to 0 - only for current user
    suspend fun resetAllCardValues(): Boolean {
        return try {
            val cards = loadCards() // This already filters by user
         val resetCards = cards.map { card ->
                card.copy(
                    value = 0,
                    check = false,                     // ← reset boolean field
                    counter = 0,                       // ← optionally reset other fields
                    timerValue = 0L  ,
                    disableHorizontalDrag=true,// ← reset timer if needed
                )
            }
            saveCards(resetCards)


        } catch (e: Exception) {
            println("Error resetting card values: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Private helper methods
    private fun saveToSharedPrefs(cards: List<CardItem>) {
        val userEmail = getCurrentUserEmail() ?: return

        // Store cards with user-specific key
        val json = gson.toJson(cards)
        sharedPrefs.edit().putString("cards_$userEmail", json).apply()
        println("Saved ${cards.size} cards to SharedPreferences for user: $userEmail")
    }

    fun loadFromSharedPrefs(): List<CardItem> {
        val userEmail = getCurrentUserEmail() ?: return emptyList()

        val json = sharedPrefs.getString("cards_$userEmail", null)
        return if (json != null) {
            val type = object : TypeToken<List<CardItem>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                println("Error loading from SharedPreferences: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // Sync local data with Firestore (useful for offline-online sync) - only user's cards
    suspend fun syncWithFirestore(): List<CardItem> {
        val userEmail = getCurrentUserEmail() ?: return emptyList()

        return try {
            val firestoreCards = firestore.collection(collectionName)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(CardItem::class.java)?.copy(id = doc.id)
                }
                .sortedBy { it.priority }

            saveToSharedPrefs(firestoreCards)
            firestoreCards
        } catch (e: Exception) {
            println("Error syncing with Firestore: ${e.message}")
            e.printStackTrace()
            loadFromSharedPrefs()
        }
    }

    suspend fun updateCardPriorities(cards: List<CardItem>): Boolean {
        val userEmail = getCurrentUserEmail() ?: return false

        // Filter cards to ensure all belong to current user
        val userCards = cards.filter { it.userEmail == userEmail }

        return try {
            val batch = firestore.batch()

            // Update priorities for all user's cards
            userCards.forEachIndexed { index, card ->
                val updatedCard = card.copy(priority = index)
                val cardRef = firestore.collection(collectionName).document(card.id)
                batch.set(cardRef, updatedCard)
            }

            batch.commit().await()

            // Save to SharedPreferences as backup
            val cardsWithUpdatedPriorities = userCards.mapIndexed { index, card ->
                card.copy(priority = index)
            }
            saveToSharedPrefs(cardsWithUpdatedPriorities)

            true
        } catch (e: Exception) {
            println("Error updating card priorities: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Clear user data on logout
    fun clearUserData() {
        val userEmail = getCurrentUserEmail() ?: return
        sharedPrefs.edit().remove("cards_$userEmail").apply()
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
    var isNotificationEnabled by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var showLoginPrompt by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }
    // 🔥 FIXED: Use mutableStateOf instead of remember with mutableMapOf
    var disableHorizontalDragMap by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    // Check user authentication on startup
    LaunchedEffect(Unit) {
        currentUserEmail = UserAuthHelper.getCurrentUserEmail()

        if (currentUserEmail == null) {
            showLoginPrompt = true
            isLoading = false
        } else {
            try {
                isNotificationEnabled = notificationManager.isNotificationScheduled()
                cards = repository.loadCards().sortedBy { it.priority }
            } catch (e: Exception) {
                println("Failed to load user data: ${e.message}")
            } finally {
                isLoading = false
                isInitialLoad = false
            }
        }
    }

    // Load data when user email changes (after login)
    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail != null && !showLoginPrompt) {
            try {
                isLoading = true
                isInitialLoad = true
                isNotificationEnabled = notificationManager.isNotificationScheduled()
                cards = repository.loadCards().sortedBy { it.priority }
            } catch (e: Exception) {
                println("Failed to load user data after login: ${e.message}")
            } finally {
                isLoading = false
                isInitialLoad = false
            }
        }
    }

    // SOLUTION 1: Use functional state updates to avoid stale closures
    fun updateCardById(cardId: String, updateFunction: (CardItem) -> CardItem) {
        cards = cards.map { card ->
            if (card.id == cardId) {
                updateFunction(card) // Always uses the latest card state
            } else {
                card
            }
        }
    }

    // SOLUTION 2: Centralized card update function that always uses latest state
    fun saveCardUpdate(cardId: String, updateFunction: (CardItem) -> CardItem) {
        if (!isOperationInProgress && !isInitialLoad) {
            // Update local state immediately using the current card state
            val updatedCard = cards.find { it.id == cardId }?.let(updateFunction)

            if (updatedCard != null) {
                updateCardById(cardId, updateFunction)

                // Save to repository asynchronously
                scope.launch {
                    isOperationInProgress = true
                    try {
                        val success = repository.updateCard(updatedCard)
                        if (!success) {
                            println("Failed to save card value, reverting local changes")
                            cards = repository.loadCards().sortedBy { it.priority }
                        }
                    } catch (e: Exception) {
                        println("Error saving card: ${e.message}")
                        cards = repository.loadCards().sortedBy { it.priority }
                    } finally {
                        isOperationInProgress = false
                    }
                }
            }
        }
    }

    // SOLUTION 3: Get current card state function to avoid stale references
    fun getCurrentCard(cardId: String): CardItem? {
        return cards.find { it.id == cardId }
    }

    fun reorderCards(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || currentUserEmail == null || isOperationInProgress) return

        val reorderedCards = cards.toMutableList()
        val draggedCard = reorderedCards.removeAt(fromIndex)
        reorderedCards.add(toIndex, draggedCard)
        cards = reorderedCards

        scope.launch {
            isOperationInProgress = true
            try {
                val success = repository.updateCardPriorities(reorderedCards)
                if (!success) {
                    println("Failed to save card order, reverting")
                    cards = repository.loadCards().sortedBy { it.priority }
                }
            } catch (e: Exception) {
                println("Error reordering cards: ${e.message}")
                cards = repository.loadCards().sortedBy { it.priority }
            } finally {
                isOperationInProgress = false
            }
        }
    }

    // Show login prompt if user is not authenticated
    if (showLoginPrompt) {
        LoginPromptScreen(
            onLoginSuccess = { email ->
                currentUserEmail = email
                showLoginPrompt = false
            },
            onLoginRequired = {
                println("User needs to login")
            }
        )
        return
    }

    // Show loading screen
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF6200EE))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading your cards...",
                    color = Color.Gray
                )
                currentUserEmail?.let { email ->
                    Text(
                        text = "Signed in as: $email",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        return
    }
    var refreshTrigger by remember { mutableStateOf(0) }
    // Main UI with proper Box layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        count = cards.size,
                        key = { index -> "${cards[index].id}_$refreshTrigger" } // Include refresh trigger in key
                    ) { index ->
                        val card = cards[index]
                        DraggableSwipeableCard(
                            card = card,
                            index = index,
                            getCurrentValue = {
                                getCurrentCard(card.id)?.value ?: card.value
                            },
                            onValueChanged = { newValue ->
                                saveCardUpdate(card.id) { currentCard ->
                                    currentCard.copy(value = newValue)
                                }
                            },
                            onCardClick = {
                                selectedCard = getCurrentCard(card.id)
                                showDetailDialog = true
                            },
                            onDeleteRequest = {
                                cardToDelete = getCurrentCard(card.id)
                                showDeleteDialog = true
                            },
                            onEdit = { updatedCard ->
                                updateCardById(card.id) { _ -> updatedCard }
                                scope.launch {
                                    isOperationInProgress = true
                                    try {
                                        val success = repository.updateCard(updatedCard)
                                        if (!success) {
                                            cards = repository.loadCards().sortedBy { it.priority }
                                        }
                                    } finally {
                                        isOperationInProgress = false
                                    }
                                }
                            },
                            onDragStart = { draggedCard = getCurrentCard(card.id) },
                            onDragEnd = {
                                draggedCard = null
                                draggedOverIndex = -1
                            },
                            onDragOver = { draggedOverIndex = index },
                            onReorder = { fromIndex, toIndex -> reorderCards(fromIndex, toIndex) },
                            isDraggedOver = draggedOverIndex == index,
                            isDragging = draggedCard?.id == card.id,
                            onCheckChanged = { isChecked ->
                                saveCardUpdate(card.id) { currentCard ->
                                    currentCard.copy(check = isChecked)
                                }
                            },

                            // 🔥 FIXED: Updated callback to properly update the map
                            disableHorizontalDrag = card.disableHorizontalDrag,

                            onDisableHorizontalDragChanged = { isDisabled ->
                                val newMap = disableHorizontalDragMap.toMutableMap()
                                newMap[card.id] = isDisabled
                                disableHorizontalDragMap = newMap

                                // Update card's state and persist it
                                saveCardUpdate(card.id) { currentCard ->
                                    currentCard.copy(disableHorizontalDrag = isDisabled)
                                }
                                refreshTrigger++
                            }
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
            // Notification Toggle Button
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
                            try {
                                val success = repository.resetAllCardValues()
                                if (success) {
                                    cards = repository.loadCards().sortedBy { it.priority }
                                    disableHorizontalDragMap = cards.associate { it.id to it.disableHorizontalDrag }.toMutableMap()
                                } else {
                                    println("Failed to reset card values")
                                }
                            } catch (e: Exception) {
                                println("Error resetting cards: ${e.message}")
                                cards = repository.loadCards().sortedBy { it.priority }
                            } finally {
                                isOperationInProgress = false
                            }
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
                    isOperationInProgress = true
                    try {
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
                    } catch (e: Exception) {
                        println("Error creating card: ${e.message}")
                    } finally {
                        isOperationInProgress = false
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Card Detail Dialog
    if (showDetailDialog && selectedCard != null) {
        // FIXED: Always use the latest card state for the dialog
        val currentSelectedCard = getCurrentCard(selectedCard!!.id) ?: selectedCard!!
        CardDetailDialog(
            card = currentSelectedCard,
            onDismiss = { showDetailDialog = false },
            onEdit = { updatedCard ->
                updateCardById(updatedCard.id) { _ -> updatedCard }

                scope.launch {
                    isOperationInProgress = true
                    try {
                        val success = repository.updateCard(updatedCard)
                        if (!success) {
                            println("Failed to update card, reverting changes")
                            cards = repository.loadCards().sortedBy { it.priority }
                        }
                    } catch (e: Exception) {
                        println("Error updating card: ${e.message}")
                        cards = repository.loadCards().sortedBy { it.priority }
                    } finally {
                        isOperationInProgress = false
                    }
                }

                selectedCard = updatedCard
                showDetailDialog = false

            },
            disableHorizontalDrag = currentSelectedCard.disableHorizontalDrag
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && cardToDelete != null) {
        DeleteConfirmationDialog(
            cardName = cardToDelete!!.name,
            onConfirm = {
                scope.launch {
                    isOperationInProgress = true
                    try {
                        val success = repository.deleteCard(cardToDelete!!.id)
                        if (success) {
                            cards = cards.filter { it.id != cardToDelete!!.id }
                        } else {
                            println("Failed to delete card")
                        }
                    } catch (e: Exception) {
                        println("Error deleting card: ${e.message}")
                    } finally {
                        isOperationInProgress = false
                        cardToDelete = null
                        showDeleteDialog = false
                    }
                }
            },
            onDismiss = {
                cardToDelete = null
                showDeleteDialog = false
            }
        )
    }
}
@Composable
fun CounterPopup(
    initialValue: Int,
    onDismiss: (Int) -> Unit
) {
    var count by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = { onDismiss(count) },
        title = { Text("Counter") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current Count: $count", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { count-- }) {
                        Text("➖", fontSize = 24.sp)
                    }
                    Button(onClick = { count++ }) {
                        Text("➕", fontSize = 24.sp)
                    }
                }
            }
        },
        confirmButton = {
            IconButton(onClick = { onDismiss(count) }) {
                Text("❌", fontSize = 24.sp)
            }
        }
    )
}
@Composable
fun TimerPopup(
    initialTime: Long,
    onDismiss: (Long) -> Unit
) {
    var time by remember { mutableStateOf(initialTime) }
    var isRunning by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }

    // Effect to update timer
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            time++
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss(time) // Save current time on popup dismiss
        },
        title = { Text("Timer") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Time: ${time}s",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!isRunning) {
                    Button(onClick = {
                        isRunning = true
                        startTime = System.currentTimeMillis()
                    }) {
                        Text("▶ Start")
                    }
                } else {
                    Button(onClick = {
                        isRunning = false
                    }) {
                        Text("⏹ Stop")
                    }
                }
            }
        },
        confirmButton = {
            IconButton(
                onClick = {
                    isRunning = false
                    onDismiss(time)
                }
            ) {
                Text("❌", fontSize = 24.sp)
            }
        }
    )
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
    isDragging: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    disableHorizontalDrag: Boolean,
    onDisableHorizontalDragChanged: (Boolean) -> Unit
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isHorizontalDragging by remember { mutableStateOf(false) }
    var isVerticalDragging by remember { mutableStateOf(false) }
    var startIndex by remember { mutableIntStateOf(-1) }
    var showTimerPopup by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }
    val reorderThreshold = with(density) { 60.dp.toPx() }
    var dragJob: Job? by remember { mutableStateOf(null) }
    var isDragEnabled by remember { mutableStateOf(false) }
    var pressStartTime by remember { mutableStateOf(0L) }
    var pressStartPosition by remember { mutableStateOf<Offset>(Offset.Zero) }


    val currentValue = getCurrentValue()
    val cardColor = getCardColor(currentValue)
    val animatedColor by animateColorAsState(
        targetValue = if (disableHorizontalDrag) MaterialTheme.colorScheme.onBackground else getCardColor(currentValue),
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
                detectTapGestures(
                    onPress = { offset ->
                        // Cancel any existing drag job
                        dragJob?.cancel()
                        isDragEnabled = false

                        // Start a coroutine to enable drag after 500ms
                        dragJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            isDragEnabled = true
                        }

                        // Wait for release or drag to start
                        val released = tryAwaitRelease()
                        if (released) {
                            // User released before 500ms, cancel drag enabling
                            dragJob?.cancel()
                            isDragEnabled = false

                            // Handle as click if no significant movement occurred
                            if (abs(horizontalDragOffset) < 10f && abs(verticalDragOffset) < 10f) {
                                onCardClick()
                            }
                        }
                    }
                )
            }
            .pointerInput(card.id, currentValue) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for first pointer down
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressStartTime = System.currentTimeMillis()
                        pressStartPosition = down.position
                        isDragEnabled = false

                        // Start delay coroutine
                        dragJob?.cancel()
                        dragJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            isDragEnabled = true
                        }

                        var pointer = down
                        var totalDrag = Offset.Zero

                        do {
                            val event = awaitPointerEvent()
                            val currentPointer = event.changes.firstOrNull { it.id == pointer.id }

                            if (currentPointer != null) {
                                val currentTime = System.currentTimeMillis()
                                val dragDelta = currentPointer.position - pointer.position
                                totalDrag += dragDelta

                                // Check if we should start dragging (after 500ms and with some movement)
                                if (isDragEnabled && !isHorizontalDragging && !isVerticalDragging &&
                                    (abs(totalDrag.x) > 10f || abs(totalDrag.y) > 10f)) {
                                    // First drag movement - call onDragStart and determine direction
                                    startIndex = index
                                    onDragStart()

                                    // Determine drag direction based on total accumulated movement
                                    if (abs(totalDrag.x) > abs(totalDrag.y)) {
                                        isHorizontalDragging = true
                                    } else {
                                        isVerticalDragging = true
                                        onDragOver()
                                    }
                                }

                                // Process all drag movements once dragging has started
                                if (isDragEnabled && (isHorizontalDragging || isVerticalDragging)) {
                                    if (isHorizontalDragging) {
                                        val currentVal = getCurrentValue()
                                        val newOffset = horizontalDragOffset + dragDelta.x

                                        horizontalDragOffset = when {
                                            newOffset > 0 && currentVal >= 5 -> newOffset * 0.2f
                                            newOffset < 0 && currentVal <= 0 -> newOffset * 0.2f
                                            else -> newOffset.coerceIn(-200f, 200f)
                                        }
                                    } else if (isVerticalDragging) {
                                        verticalDragOffset += dragDelta.y
                                    }

                                    currentPointer.consume()
                                }

                                pointer = currentPointer
                            }

                        } while (event.changes.any { it.pressed })

                        // Pointer released - handle end
                        dragJob?.cancel()

                        if (isHorizontalDragging || isVerticalDragging) {
                            // Handle drag end
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
                            } else if (!disableHorizontalDrag && isHorizontalDragging) {
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

                            // Reset drag states
                            horizontalDragOffset = 0f
                            verticalDragOffset = 0f
                            isHorizontalDragging = false
                            isVerticalDragging = false
                            onDragEnd()
                        } else {
                            // Handle click (no drag occurred and released before/without significant movement)
                            val timeDiff = System.currentTimeMillis() - pressStartTime
                            val movementDistance = (pointer.position - pressStartPosition).getDistance()

                            if (timeDiff < 500 && movementDistance < 10f) {
                                onCardClick()
                            }
                        }

                        isDragEnabled = false
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp) // Add spacing to ensure child content isn't clipped
        )
        {
            // Checkbox aligned left-middle
            if (disableHorizontalDrag) {
                Checkbox(
                    checked = card.check,
                    onCheckedChange = { onCheckChanged(it) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )




                    }

                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()  // Take up full height
                            .wrapContentWidth(), // Optional: keeps the width tight to content
                        contentAlignment = Alignment.Center // Align content vertically and horizontally
                    ) {
                    Row(

                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Small Timer Button
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showTimerPopup = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFC8E6C9), shape = CircleShape)
                            ) {
                                Text("⏱", fontSize = 14.sp)
                            }

                            if (showTimerPopup) {
                                TimerPopup(
                                    initialTime = card.timerValue,
                                    onDismiss = { updatedTime ->
                                        onEdit(card.copy(timerValue = updatedTime))
                                        showTimerPopup = false
                                    }
                                )
                            }
                        }

                        // Small Counter Button
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showPopup = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFBBDEFB), shape = CircleShape)
                            ) {
                                Text("🔢", fontSize = 14.sp)
                            }

                            if (showPopup) {
                                CounterPopup(
                                    initialValue = currentValue,
                                    onDismiss = { updatedValue ->
                                        onValueChanged(updatedValue)
                                        showPopup = false
                                    }
                                )
                            }
                        }

                        // Small Dropdown Menu Icon
                        Box {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {

                                DropdownMenuItem(
                                    onClick = { },
                                    text = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (previewValue > 2) Color.White.copy(alpha = 0.2f)
                                                    else Color.Black.copy(alpha = 0.1f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = previewValue.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    onClick = {
                                        showEditDialog = true
                                        expanded = false
                                    },
                                    enabled = !isHorizontalDragging && !isVerticalDragging,
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit", fontSize = 12.sp)
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    onClick = {
                                        onDeleteRequest()
                                        expanded = false
                                    },
                                    enabled = !isHorizontalDragging && !isVerticalDragging,
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", fontSize = 12.sp)
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    onClick = {
                                        onDisableHorizontalDragChanged(!disableHorizontalDrag)
                                        expanded = false
                                    },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = disableHorizontalDrag,
                                                onCheckedChange = { newValue ->
                                                    onDisableHorizontalDragChanged(newValue)
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Disable Drag", fontSize = 12.sp)
                                        }
                                    }
                                )
                            }
                        }
                    }}


                }


                Column {
                    Text(
                        text = card.description
                            .replace("\n", " ")
                            .replace("\t", " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                            .take(50) + if (card.description.length > 50) "..." else "",
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                }
            }
        }
        // Horizontal drag indicators
        if (!disableHorizontalDrag && isHorizontalDragging && abs(horizontalDragOffset) > swipeThreshold / 2) {
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
            onSave = { name, description, youtubeUrl ->
                onEdit(card.copy(name = name, description = description, youtubeUrl = youtubeUrl))
                showEditDialog = false
            }
        )
    }


}



@Composable
fun LoginPromptScreen(
    onLoginSuccess: (String) -> Unit,
    onLoginRequired: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var showAuthScreen by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    // Listen for Firebase auth state changes
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                isLoggedIn = true
                onLoginSuccess(currentUser.uid)
            } else {
                isLoggedIn = false
            }
        }

        auth.addAuthStateListener(authStateListener)

        // Cleanup listener when composable is disposed
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    // Only show UI if user is not logged in
    if (!isLoggedIn && auth.currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when {
                showAuthScreen -> {
                    // Check your AuthScreen's actual callback signature and use one of these:

                    // Option 1: If AuthScreen expects () -> Unit
                    AuthScreen(
                        onLoginSuccess = {
                            showAuthScreen = false
                        }
                    )

                    // Option 2: If AuthScreen expects (String) -> Unit
                    // AuthScreen(
                    //     onLoginSuccess = { userId ->
                    //         showAuthScreen = false
                    //     }
                    // )
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Login",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please sign in to continue",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Your cards are linked to your account",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                showAuthScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Sign In",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
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

@Composable
fun CardDetailDialog(
    card: CardItem,
    onDismiss: () -> Unit,
    onEdit: (CardItem) -> Unit,
    disableHorizontalDrag: Boolean


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
            colors = CardDefaults.cardColors(
                containerColor = if (disableHorizontalDrag) Color.White else getCardColor(card.value)
            )

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
                            color = if (card.value > 2) Color.Black else Color.Black,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (card.value > 2) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.value.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (card.value > 2) Color.Black else Color.Black
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
                            color = if (card.value > 2) Color.Black else Color.Black
                        )

                        Text(
                            text = card.description,
                            fontSize = 14.sp,
                            color = if (card.value > 2) Color.Black.copy(alpha = 0.9f) else Color.Gray,
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
                            color = if (card.value > 2) Color.Black else Color.Black
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
                        color = if (card.value > 2) Color.Black.copy(alpha = 0.7f) else Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground    )
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