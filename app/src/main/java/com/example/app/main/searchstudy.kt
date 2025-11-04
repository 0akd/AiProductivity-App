package com.arjundubey.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val android.content.Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "topics")

// Base interface for nested items
interface NestableItem {
    val title: String
    val children: List<NestableItem>
    val isStarred: Boolean
}

// Subtopic now implements NestableItem and can have its own children
data class Subtopic(
    override val title: String = "",
    override val children: List<Subtopic> = emptyList(),
    override val isStarred: Boolean = false
) : NestableItem {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "isStarred" to isStarred,
            "children" to children.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Subtopic {
            return Subtopic(
                title = map["title"] as? String ?: "",
                isStarred = map["isStarred"] as? Boolean ?: false,
                children = (map["children"] as? List<*>)?.mapNotNull { childMap ->
                    (childMap as? Map<String, Any>)?.let { fromMap(it) }
                } ?: emptyList()
            )
        }
    }
}

// Topic also implements NestableItem
data class Topic(
    override val title: String = "",
    override val children: List<Subtopic> = emptyList(),
    override val isStarred: Boolean = false
) : NestableItem {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "isStarred" to isStarred,
            "children" to children.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Topic {
            return Topic(
                title = map["title"] as? String ?: "",
                isStarred = map["isStarred"] as? Boolean ?: false,
                children = (map["children"] as? List<*>)?.mapNotNull { childMap ->
                    (childMap as? Map<String, Any>)?.let { Subtopic.fromMap(it) }
                } ?: emptyList()
            )
        }
    }
}

class DataStoreRepository(private val context: android.content.Context) {
    private val TOPICS_KEY = stringPreferencesKey("topics_data")

    suspend fun saveTopics(topics: List<Topic>) {
        try {
            val jsonString = exportToJson(topics)
            context.dataStore.edit { preferences ->
                preferences[TOPICS_KEY] = jsonString
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun loadTopics(): List<Topic> {
        return try {
            val jsonString = context.dataStore.data.map { preferences ->
                preferences[TOPICS_KEY] ?: ""
            }.first()

            if (jsonString.isEmpty()) {
                emptyList()
            } else {
                importFromJson(jsonString)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportToJson(topics: List<Topic>): String {
        val jsonArray = JSONArray()
        topics.forEach { topic ->
            jsonArray.put(JSONObject(topic.toMap()))
        }
        return jsonArray.toString(2)
    }

    fun importFromJson(jsonString: String): List<Topic> {
        val jsonArray = JSONArray(jsonString)
        val topics = mutableListOf<Topic>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val map = jsonObject.toMap()
            topics.add(Topic.fromMap(map))
        }

        return topics
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        keys().forEach { key ->
            val value = get(key)
            map[key] = when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            }
        }
        return map
    }

    private fun JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until length()) {
            val value = get(i)
            list.add(when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            })
        }
        return list
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsCRUDScreen() {
    var topics by remember { mutableStateOf(emptyList<Topic>()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<NestableItem?>(null) }
    var parentItem by remember { mutableStateOf<NestableItem?>(null) }
    var itemType by remember { mutableStateOf<ItemType>(ItemType.TOPIC) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedItemForSearch by remember { mutableStateOf<NestableItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val repository = remember { DataStoreRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (jsonString != null) {
                    topics = repository.importFromJson(jsonString)
                    scope.launch {
                        repository.saveTopics(topics)
                        snackbarHostState.showSnackbar("Data imported successfully!")
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }

    // Export function
    fun exportData() {
        try {
            val jsonString = repository.exportToJson(topics)
            val fileName = "topics_export_${System.currentTimeMillis()}.json"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar("Exported to Downloads/$fileName")
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                java.io.FileOutputStream(file).use { it.write(jsonString.toByteArray()) }
                scope.launch {
                    snackbarHostState.showSnackbar("Exported to Downloads/$fileName")
                }
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    // Load topics from DataStore
    LaunchedEffect(Unit) {
        topics = repository.loadTopics()
        isLoading = false
    }

    // Function to save topics to DataStore
    fun saveToDataStore() {
        scope.launch {
            try {
                repository.saveTopics(topics)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving: ${e.message}")
            }
        }
    }

    // Function to recursively find and update items
    fun updateNestedItems(
        items: List<NestableItem>,
        targetTitle: String,
        updateFn: (NestableItem) -> NestableItem
    ): List<NestableItem> {
        return items.map { item ->
            if (item.title == targetTitle) {
                updateFn(item)
            } else {
                when (item) {
                    is Topic -> item.copy(children = updateNestedItems(item.children, targetTitle, updateFn) as List<Subtopic>)
                    is Subtopic -> item.copy(children = updateNestedItems(item.children, targetTitle, updateFn) as List<Subtopic>)
                    else -> item
                }
            }
        }
    }

    // Function to recursively find and delete items
    fun deleteNestedItems(
        items: List<NestableItem>,
        targetTitle: String
    ): List<NestableItem> {
        return items.flatMap { item ->
            if (item.title == targetTitle) {
                emptyList()
            } else {
                when (item) {
                    is Topic -> listOf(item.copy(children = deleteNestedItems(item.children, targetTitle) as List<Subtopic>))
                    is Subtopic -> listOf(item.copy(children = deleteNestedItems(item.children, targetTitle) as List<Subtopic>))
                    else -> listOf(item)
                }
            }
        }
    }

    // Function to recursively find parent items
    fun findParentItem(items: List<NestableItem>, childTitle: String): NestableItem? {
        items.forEach { item ->
            if (item.children.any { it.title == childTitle }) {
                return item
            }
            val foundInChildren = findParentItem(item.children, childTitle)
            if (foundInChildren != null) {
                return foundInChildren
            }
        }
        return null
    }

    // Function to get full hierarchy path for an item
    fun getFullPath(item: NestableItem, allItems: List<NestableItem>): String {
        val path = mutableListOf(item.title)
        var currentItem: NestableItem? = item
        var parent: NestableItem?

        while (currentItem != null) {
            parent = findParentItem(allItems, currentItem.title)
            if (parent != null) {
                path.add(0, parent.title)
                currentItem = parent
            } else {
                break
            }
        }

        return path.joinToString(" > ")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Topics Manager")
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Data") },
                                onClick = {
                                    exportData()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, "Export")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Data") },
                                onClick = {
                                    importLauncher.launch("application/json")
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FileUpload, "Import")
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    parentItem = null
                    itemType = ItemType.TOPIC
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (topics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No topics yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click + to add or import data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topics) { topic ->
                        NestableItemComposable(
                            item = topic,
                            level = 0,
                            onEditItem = { item ->
                                editingItem = item
                                parentItem = findParentItem(topics, item.title)
                                itemType = if (item is Topic) ItemType.TOPIC else ItemType.SUBTOPIC
                                showDialog = true
                            },
                            onDeleteItem = { itemTitle ->
                                itemToDelete = itemTitle
                                showDeleteDialog = true
                            },
                            onAddChild = { parent ->
                                editingItem = null
                                parentItem = parent
                                itemType = ItemType.SUBTOPIC
                                showDialog = true
                            },
                            onWebSearch = { item ->
                                selectedItemForSearch = item
                                showSearchDialog = true
                            },
                            onToggleStar = { itemTitle ->
                                topics = updateNestedItems(topics, itemTitle) { item ->
                                    when (item) {
                                        is Topic -> item.copy(isStarred = !item.isStarred)
                                        is Subtopic -> item.copy(isStarred = !item.isStarred)
                                        else -> item
                                    }
                                } as List<Topic>
                                saveToDataStore()
                            }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    itemToDelete = null
                },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete this item and all its nested items?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            topics = deleteNestedItems(topics, itemToDelete!!) as List<Topic>
                            saveToDataStore()
                            showDeleteDialog = false
                            itemToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            itemToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Universal Dialog for all item types
        if (showDialog) {
            UniversalItemDialog(
                item = editingItem,
                itemType = itemType,
                onDismiss = { showDialog = false },
                onSave = { newItem ->
                    if (editingItem != null) {
                        // Update existing item
                        topics = updateNestedItems(topics, editingItem!!.title) { newItem } as List<Topic>
                    } else {
                        // Add new item
                        if (parentItem == null) {
                            // Adding a new topic
                            topics = topics + (newItem as Topic)
                        } else {
                            // Adding a new subtopic to a parent
                            topics = updateNestedItems(topics, parentItem!!.title) { parent ->
                                when (parent) {
                                    is Topic -> parent.copy(children = parent.children + (newItem as Subtopic))
                                    is Subtopic -> parent.copy(children = parent.children + (newItem as Subtopic))
                                    else -> parent
                                }
                            } as List<Topic>
                        }
                    }
                    showDialog = false
                    saveToDataStore()
                }
            )
        }

        // Search Dialog
        if (showSearchDialog && selectedItemForSearch != null) {
            SearchOptionsDialog(
                item = selectedItemForSearch!!,
                fullPath = getFullPath(selectedItemForSearch!!, topics),
                onDismiss = {
                    showSearchDialog = false
                    selectedItemForSearch = null
                }
            )
        }
    }
}

enum class ItemType {
    TOPIC, SUBTOPIC
}

@Composable
fun NestableItemComposable(
    item: NestableItem,
    level: Int,
    onEditItem: (NestableItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onAddChild: (NestableItem) -> Unit,
    onWebSearch: (NestableItem) -> Unit,
    onToggleStar: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level < 2) }

    val leftBorderColor = when (level % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left border indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isExpanded && item.children.isNotEmpty()) 120.dp else 60.dp)
                    .background(leftBorderColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (item.children.isNotEmpty()) isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Star Icon
                    IconButton(
                        onClick = { onToggleStar(item.title) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (item.isStarred) "Unstar" else "Star",
                            tint = if (item.isStarred) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title
                    Text(
                        text = item.title,
                        style = when (level) {
                            0 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Expand/Collapse Icon
                    if (item.children.isNotEmpty()) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (item.children.isNotEmpty()) {
                    Text(
                        text = "${item.children.size} nested item${if (item.children.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row (Icon Only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Web Search Button
                    OutlinedButton(
                        onClick = { onWebSearch(item) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Child Button
                    OutlinedButton(
                        onClick = { onAddChild(item) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit Button
                    OutlinedButton(
                        onClick = { onEditItem(item) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button
                    OutlinedButton(
                        onClick = { onDeleteItem(item.title) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Children List (shown when expanded)
                if (isExpanded && item.children.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.children.forEach { child ->
                            NestableItemComposable(
                                item = child,
                                level = level + 1,
                                onEditItem = onEditItem,
                                onDeleteItem = onDeleteItem,
                                onAddChild = onAddChild,
                                onWebSearch = onWebSearch,
                                onToggleStar = onToggleStar
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOptionsDialog(
    item: NestableItem,
    fullPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun performSearch(searchType: String, query: String) {
        val searchQuery = when (searchType) {
            "pdf" -> "$query filetype:pdf"
            "youtube" -> "$query site:youtube.com"
            "problem" -> "$query problems exercises"
            "qa" -> "$query questions answers"
            else -> query
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(searchQuery)}"))
        context.startActivity(intent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Search Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fullPath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            val searchOptions = listOf(
                SearchOption("General Search", Icons.Default.Language, "Search general information"),
                SearchOption("PDF Documents", Icons.Default.FileDownload, "Find PDF documents"),
                SearchOption("YouTube Videos", Icons.Default.Language, "Search on YouTube"),
                SearchOption("Practice Problems", Icons.Default.Edit, "Find practice problems"),
                SearchOption("Q&A Forums", Icons.Default.Language, "Find questions and answers")
            )

            searchOptions.forEachIndexed { index, searchOption ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (index) {
                                0 -> performSearch("general", fullPath)
                                1 -> performSearch("pdf", fullPath)
                                2 -> performSearch("youtube", fullPath)
                                3 -> performSearch("problem", fullPath)
                                4 -> performSearch("qa", fullPath)
                            }
                            onDismiss()
                        }
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = searchOption.icon,
                            contentDescription = searchOption.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = searchOption.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = searchOption.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class SearchOption(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalItemDialog(
    item: NestableItem?,
    itemType: ItemType,
    onDismiss: () -> Unit,
    onSave: (NestableItem) -> Unit
) {
    var title by remember { mutableStateOf(item?.title ?: "") }

    val dialogTitle = when {
        item == null && itemType == ItemType.TOPIC -> "Add Topic"
        item == null && itemType == ItemType.SUBTOPIC -> "Add Subtopic"
        item != null && itemType == ItemType.TOPIC -> "Edit Topic"
        item != null && itemType == ItemType.SUBTOPIC -> "Edit Subtopic"
        else -> "Edit Item"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val newItem: NestableItem = when (itemType) {
                                ItemType.TOPIC -> Topic(
                                    title = title,
                                    children = (item as? Topic)?.children ?: emptyList(),
                                    isStarred = item?.isStarred ?: false
                                )
                                ItemType.SUBTOPIC -> Subtopic(
                                    title = title,
                                    children = (item as? Subtopic)?.children ?: emptyList(),
                                    isStarred = item?.isStarred ?: false
                                )
                            }
                            onSave(newItem)
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text(if (item == null) "Add" else "Update")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}