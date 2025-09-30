package com.arjundubey.app
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.delay
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File

data class TileData(
    val id: String = "",
    val title: String = "",
    val steps: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

sealed class PendingOperation {
    data class Add(val tile: TileData) : PendingOperation()
    data class Update(val tile: TileData) : PendingOperation()
    data class Delete(val tileId: String) : PendingOperation()
}

class TileRepository(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("tiles_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TILES_KEY = "cached_tiles"
        private const val PENDING_OPS_KEY = "pending_operations"
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
    }

    // Load tiles from SharedPreferences
    fun loadCachedTiles(): List<TileData> {
        val json = sharedPrefs.getString(TILES_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TileData>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Save tiles to SharedPreferences
    fun cacheTiles(tiles: List<TileData>) {
        val json = gson.toJson(tiles)
        sharedPrefs.edit()
            .putString(TILES_KEY, json)
            .putLong(LAST_SYNC_KEY, System.currentTimeMillis())
            .apply()
    }

    // Load pending operations
    private fun loadPendingOperations(): MutableList<PendingOperation> {
        val json = sharedPrefs.getString(PENDING_OPS_KEY, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<PendingOperation>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    // Save pending operations
    private fun savePendingOperations(operations: List<PendingOperation>) {
        val json = gson.toJson(operations)
        sharedPrefs.edit().putString(PENDING_OPS_KEY, json).apply()
    }

    // Get pending operations count
    fun getPendingOperationsCount(): Int = loadPendingOperations().size

    // Process pending operations when online
    fun processPendingOperations(
        onProgress: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val operations = loadPendingOperations()
        if (operations.isEmpty()) {
            onComplete()
            return
        }

        var processed = 0
        val total = operations.size
        val operationsToRemove = mutableListOf<Int>()

        fun checkCompletion() {
            if (processed >= total) {
                // Remove processed operations
                val remainingOps = operations.filterIndexed { index, _ -> index !in operationsToRemove }
                savePendingOperations(remainingOps)
                onComplete()
            }
        }

        operations.forEachIndexed { index, operation ->
            when (operation) {
                is PendingOperation.Add -> {
                    firestore.collection("tiles")
                        .add(
                            hashMapOf(
                                "title" to operation.tile.title,
                                "steps" to operation.tile.steps,
                                "timestamp" to operation.tile.timestamp
                            )
                        )
                        .addOnSuccessListener { docRef ->
                            // Replace temp ID with real Firebase ID in cache
                            val cachedTiles = loadCachedTiles().toMutableList()
                            val tempIndex = cachedTiles.indexOfFirst { it.id == operation.tile.id }
                            if (tempIndex != -1) {
                                cachedTiles[tempIndex] = cachedTiles[tempIndex].copy(id = docRef.id)
                                cacheTiles(cachedTiles)
                            }

                            processed++
                            operationsToRemove.add(index)
                            onProgress("Synced $processed/$total")
                            checkCompletion()
                        }
                        .addOnFailureListener {
                            processed++
                            onProgress("Synced $processed/$total (with errors)")
                            checkCompletion()
                        }
                }
                is PendingOperation.Update -> {
                    if (!operation.tile.id.startsWith("temp_")) {
                        firestore.collection("tiles")
                            .document(operation.tile.id)
                            .update(
                                hashMapOf(
                                    "title" to operation.tile.title,
                                    "steps" to operation.tile.steps,
                                    "timestamp" to operation.tile.timestamp
                                ) as Map<String, Any>
                            )
                            .addOnSuccessListener {
                                processed++
                                operationsToRemove.add(index)
                                onProgress("Synced $processed/$total")
                                checkCompletion()
                            }
                            .addOnFailureListener {
                                processed++
                                onProgress("Synced $processed/$total (with errors)")
                                checkCompletion()
                            }
                    } else {
                        // Skip temp updates, they'll be handled by Add
                        processed++
                        operationsToRemove.add(index)
                        checkCompletion()
                    }
                }
                is PendingOperation.Delete -> {
                    if (!operation.tileId.startsWith("temp_")) {
                        firestore.collection("tiles")
                            .document(operation.tileId)
                            .delete()
                            .addOnSuccessListener {
                                processed++
                                operationsToRemove.add(index)
                                onProgress("Synced $processed/$total")
                                checkCompletion()
                            }
                            .addOnFailureListener {
                                processed++
                                onProgress("Synced $processed/$total (with errors)")
                                checkCompletion()
                            }
                    } else {
                        // For temp IDs, just remove from pending
                        processed++
                        operationsToRemove.add(index)
                        checkCompletion()
                    }
                }
            }
        }
    }

    // Setup real-time listener with offline cache
    fun setupTilesListener(
        onTilesUpdated: (List<TileData>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Enable offline persistence
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        firestore.collection("tiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("Error: ${error.message}")
                    onTilesUpdated(loadCachedTiles())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tiles = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<TileData>()?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }

                    cacheTiles(tiles)
                    onTilesUpdated(tiles)
                }
            }
    }

    // Add tile (offline-capable)
    fun addTile(tile: TileData, isOnline: Boolean, onSuccess: (TileData) -> Unit, onError: (String) -> Unit) {
        if (isOnline) {
            val tileData = hashMapOf(
                "title" to tile.title,
                "steps" to tile.steps,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("tiles")
                .add(tileData)
                .addOnSuccessListener {
                    val newTile = tile.copy(id = it.id, timestamp = System.currentTimeMillis())
                    // Update cache with real Firebase ID
                    val cachedTiles = loadCachedTiles().toMutableList()
                    cachedTiles.add(0, newTile)
                    cacheTiles(cachedTiles)
                    onSuccess(newTile)
                }
                .addOnFailureListener { e ->
                    // Save offline if failed
                    addOfflineTile(tile, onSuccess)
                }
        } else {
            addOfflineTile(tile, onSuccess)
        }
    }

    private fun addOfflineTile(tile: TileData, onSuccess: (TileData) -> Unit) {
        val cachedTiles = loadCachedTiles().toMutableList()
        val newTile = tile.copy(
            id = "temp_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis()
        )
        cachedTiles.add(0, newTile)
        cacheTiles(cachedTiles)

        // Add to pending operations
        val pendingOps = loadPendingOperations()
        pendingOps.add(PendingOperation.Add(newTile))
        savePendingOperations(pendingOps)

        onSuccess(newTile)
    }

    // Update tile (offline-capable)
    fun updateTile(tile: TileData, isOnline: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val updatedTile = tile.copy(timestamp = System.currentTimeMillis())

        // Update cache immediately
        val cachedTiles = loadCachedTiles().toMutableList()
        val index = cachedTiles.indexOfFirst { it.id == tile.id }
        if (index != -1) {
            cachedTiles[index] = updatedTile
            cacheTiles(cachedTiles)
        }

        // Handle pending operations - remove old operations for this tile
        val pendingOps = loadPendingOperations().toMutableList()
        pendingOps.removeAll { op ->
            when (op) {
                is PendingOperation.Update -> op.tile.id == tile.id
                is PendingOperation.Add -> op.tile.id == tile.id
                else -> false
            }
        }

        if (isOnline && !tile.id.startsWith("temp_")) {
            firestore.collection("tiles")
                .document(tile.id)
                .update(
                    hashMapOf(
                        "title" to updatedTile.title,
                        "steps" to updatedTile.steps,
                        "timestamp" to updatedTile.timestamp
                    ) as Map<String, Any>
                )
                .addOnSuccessListener {
                    savePendingOperations(pendingOps)
                    onSuccess()
                }
                .addOnFailureListener {
                    pendingOps.add(PendingOperation.Update(updatedTile))
                    savePendingOperations(pendingOps)
                    onSuccess()
                }
        } else {
            // Offline or temp tile
            if (tile.id.startsWith("temp_")) {
                // For temp tiles, update the Add operation instead of creating Update
                val addOpIndex = pendingOps.indexOfFirst {
                    it is PendingOperation.Add && it.tile.id == tile.id
                }
                if (addOpIndex != -1) {
                    pendingOps[addOpIndex] = PendingOperation.Add(updatedTile)
                } else {
                    pendingOps.add(PendingOperation.Add(updatedTile))
                }
            } else {
                pendingOps.add(PendingOperation.Update(updatedTile))
            }
            savePendingOperations(pendingOps)
            onSuccess()
        }
    }

    // Delete tile (offline-capable)
    fun deleteTile(tileId: String, isOnline: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Remove from cache immediately
        val cachedTiles = loadCachedTiles().filter { it.id != tileId }
        cacheTiles(cachedTiles)

        // Handle pending operations - remove any operations for this tile
        val pendingOps = loadPendingOperations().toMutableList()
        val wasTemp = tileId.startsWith("temp_")

        // Remove all operations related to this tile
        pendingOps.removeAll { op ->
            when (op) {
                is PendingOperation.Add -> op.tile.id == tileId
                is PendingOperation.Update -> op.tile.id == tileId
                is PendingOperation.Delete -> op.tileId == tileId
            }
        }

        if (isOnline && !wasTemp) {
            firestore.collection("tiles")
                .document(tileId)
                .delete()
                .addOnSuccessListener {
                    savePendingOperations(pendingOps)
                    onSuccess()
                }
                .addOnFailureListener {
                    pendingOps.add(PendingOperation.Delete(tileId))
                    savePendingOperations(pendingOps)
                    onSuccess()
                }
        } else {
            // Offline or temp tile
            if (!wasTemp) {
                // Only add delete operation for real Firebase IDs
                pendingOps.add(PendingOperation.Delete(tileId))
            }
            // For temp tiles, just removing from cache and pending ops is enough
            savePendingOperations(pendingOps)
            onSuccess()
        }
    }

    // Add this extension function for string repetition
    operator fun String.times(n: Int): String {
        return repeat(n)
    }

    // Export tiles to text file
    fun exportTilesToText(tiles: List<TileData>): File {
        val file = File(context.cacheDir, "tiles_export_${System.currentTimeMillis()}.txt")
        val content = buildString {
            appendLine("=" * 50)
            appendLine("TILES EXPORT")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
            appendLine("Total Tiles: ${tiles.size}")
            appendLine("=" * 50)
            appendLine()

            tiles.forEachIndexed { index, tile ->
                appendLine("TILE ${index + 1}: ${tile.title}")
                appendLine("-" * 50)
                appendLine("Steps:")
                tile.steps.forEachIndexed { stepIndex, step ->
                    appendLine("  ${stepIndex + 1}. $step")
                }
                appendLine()
                appendLine()
            }

            appendLine("=" * 50)
            appendLine("END OF EXPORT")
            appendLine("=" * 50)
        }

        file.writeText(content)
        return file
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileScreen() {
    val context = LocalContext.current
    val repository = remember { TileRepository(context) }
    val scope = rememberCoroutineScope()

    var tiles by remember { mutableStateOf<List<TileData>>(emptyList()) }
    var selectedTile by remember { mutableStateOf<TileData?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val isAdmin = currentUserEmail == "r@gmail.com"
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOnline by remember { mutableStateOf(false) }
    var pendingSync by remember { mutableStateOf(false) }

    // Improved network connectivity monitoring with debouncing
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var syncHandler: Handler? = null
        var syncRunnable: Runnable? = null

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                android.os.Handler(Looper.getMainLooper()).post {
                    statusMessage = "Back online - syncing changes..."

                    // Cancel any pending sync
                    syncRunnable?.let { syncHandler?.removeCallbacks(it) }

                    // Debounce sync by 2 seconds
                    syncHandler = Handler(Looper.getMainLooper())
                    syncRunnable = Runnable {
                        repository.processPendingOperations(
                            onProgress = { progress ->
                                statusMessage = progress
                                pendingSync = true
                            },
                            onComplete = {
                                statusMessage = "All changes synced!"
                                pendingSync = false
                                // Reload to get Firebase IDs
                                scope.launch {
                                    delay(500)
                                    tiles = repository.loadCachedTiles()
                                }
                            }
                        )
                    }
                    syncHandler?.postDelayed(syncRunnable!!, 2000)
                }
            }

            override fun onLost(network: Network) {
                isOnline = false
                // Cancel any pending sync
                syncRunnable?.let { syncHandler?.removeCallbacks(it) }

                android.os.Handler(Looper.getMainLooper()).post {
                    statusMessage = "Offline mode - changes will sync when online"
                    pendingSync = false
                }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val newOnlineStatus = hasInternet && isValidated

                if (newOnlineStatus && !isOnline) {
                    // Just became online
                    isOnline = true
                    android.os.Handler(Looper.getMainLooper()).post {
                        statusMessage = "Connected - syncing..."

                        // Cancel any pending sync
                        syncRunnable?.let { syncHandler?.removeCallbacks(it) }

                        // Debounce sync by 2 seconds
                        syncHandler = Handler(Looper.getMainLooper())
                        syncRunnable = Runnable {
                            repository.processPendingOperations(
                                onProgress = { progress ->
                                    statusMessage = progress
                                    pendingSync = true
                                },
                                onComplete = {
                                    statusMessage = "All changes synced!"
                                    pendingSync = false
                                    // Reload to get Firebase IDs
                                    scope.launch {
                                        delay(500)
                                        tiles = repository.loadCachedTiles()
                                    }
                                }
                            )
                        }
                        syncHandler?.postDelayed(syncRunnable!!, 2000)
                    }
                } else if (!newOnlineStatus && isOnline) {
                    // Just went offline
                    isOnline = false
                    // Cancel any pending sync
                    syncRunnable?.let { syncHandler?.removeCallbacks(it) }

                    android.os.Handler(Looper.getMainLooper()).post {
                        statusMessage = "Connection lost - offline mode"
                        pendingSync = false
                    }
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Check initial connectivity
        val currentNetwork = connectivityManager.activeNetwork
        val currentCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
        val currentlyOnline = currentCapabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false

        isOnline = currentlyOnline

        onDispose {
            syncRunnable?.let { syncHandler?.removeCallbacks(it) }
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // Load cached tiles immediately
    LaunchedEffect(Unit) {
        val cachedTiles = repository.loadCachedTiles()
        if (cachedTiles.isNotEmpty()) {
            tiles = cachedTiles
            isLoading = false
        }

        // Setup real-time listener
        repository.setupTilesListener(
            onTilesUpdated = { updatedTiles ->
                tiles = updatedTiles
                isLoading = false
            },
            onError = { error ->
                statusMessage = error
                isLoading = false
            }
        )
    }

    // Show status message temporarily
    LaunchedEffect(statusMessage) {
        if (statusMessage != null && !pendingSync) {
            kotlinx.coroutines.delay(3000)
            statusMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tiles")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Network status indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                            if (pendingSync) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Syncing...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, "Add Tile")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            try {
                                val file = repository.exportTilesToText(tiles)
                                val authority = "${context.packageName}.provider"
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    authority,
                                    file
                                )

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Tiles Export")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(Intent.createChooser(shareIntent, "Export Tiles"))
                                statusMessage = "Tiles exported successfully!"
                            } catch (e: Exception) {
                                statusMessage = "Export failed: ${e.message}"
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Share, "Export Tiles")
                }
            }
        },
        snackbarHost = {
            statusMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        if (!pendingSync) {
                            TextButton(onClick = { statusMessage = null }) {
                                Text("OK")
                            }
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                tiles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tiles available",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first tile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tiles) { tile ->
                            TileCard(
                                tile = tile,
                                onClick = { selectedTile = tile },
                                isPending = tile.id.startsWith("temp_")
                            )
                        }
                    }
                }
            }
        }
    }

    // View/Edit Popup
    selectedTile?.let { tile ->
        StepsDialog(
            tile = tile,
            isAdmin = isAdmin,
            onDismiss = { selectedTile = null },
            onEdit = {
                showEditDialog = true
            },
            onDelete = {
                repository.deleteTile(
                    tileId = tile.id,
                    isOnline = isOnline,
                    onSuccess = {
                        selectedTile = null
                        tiles = repository.loadCachedTiles()
                        statusMessage = if (isOnline) "Tile deleted" else "Tile deleted (will sync when online)"
                    },
                    onError = { error ->
                        statusMessage = error
                    }
                )
            }
        )
    }

    // Add Dialog
    if (showAddDialog) {
        EditTileDialog(
            tile = null,
            onDismiss = { showAddDialog = false },
            onSave = { newTile ->
                repository.addTile(
                    tile = newTile,
                    isOnline = isOnline,
                    onSuccess = { addedTile ->
                        showAddDialog = false
                        tiles = repository.loadCachedTiles()
                        statusMessage = if (isOnline) "Tile added" else "Tile saved (will sync when online)"
                    },
                    onError = { error ->
                        statusMessage = error
                    }
                )
            }
        )
    }

    // Edit Dialog
    if (showEditDialog && selectedTile != null) {
        EditTileDialog(
            tile = selectedTile,
            onDismiss = { showEditDialog = false },
            onSave = { updatedTile ->
                repository.updateTile(
                    tile = updatedTile,
                    isOnline = isOnline,
                    onSuccess = {
                        selectedTile = updatedTile
                        showEditDialog = false
                        tiles = repository.loadCachedTiles()
                        statusMessage = if (isOnline) "Tile updated" else "Tile saved (will sync when online)"
                    },
                    onError = { error ->
                        statusMessage = error
                    }
                )
            }
        )
    }
}

@Composable
fun TileCard(tile: TileData, onClick: () -> Unit, isPending: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                if (isPending) {
                    Text(
                        text = "Pending sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun StepsDialog(
    tile: TileData,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdmin) {
                        Row {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, false)
                ) {
                    tile.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun EditTileDialog(
    tile: TileData?,
    onDismiss: () -> Unit,
    onSave: (TileData) -> Unit
) {
    var title by remember { mutableStateOf(tile?.title ?: "") }
    var stepsText by remember { mutableStateOf(tile?.steps?.joinToString("\n") ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (tile == null) "Add Tile" else "Edit Tile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Steps (one per line)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && stepsText.isNotBlank()) {
                                val steps = stepsText.split("\n").filter { it.isNotBlank() }
                                onSave(
                                    TileData(
                                        id = tile?.id ?: "",
                                        title = title,
                                        steps = steps,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}