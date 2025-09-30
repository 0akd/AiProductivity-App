package com.arjundubey.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

// Data classes for API response
data class ApiResponse(
    val success: Boolean,
    val count: Int,
    val files: Map<String, FileItem>
)

data class FileItem(
    val name: String,
    val path: String,
    val url: String,
    val download_url: String,
    val size: Int,
    val sha: String,
    val type: String
)

// Enhanced file system data structures
data class FileSystemNode(
    val name: String,
    val children: MutableMap<String, FileSystemNode> = mutableMapOf(),
    var fileItem: FileItem? = null,
    val isDirectory: Boolean = true,
    var displayTitle: String? = null // Fovr txt files, this will store the first line content
)

// API Service
interface ApiService {
    @GET("files")
    suspend fun getFiles(): ApiResponse
}

// Enhanced ViewModel with txt content fetching
class FileSystemViewModel : ViewModel() {
    private val _apiResponse = MutableStateFlow<ApiResponse?>(null)
    val apiResponse: StateFlow<ApiResponse?> = _apiResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _txtContentCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val txtContentCache: StateFlow<Map<String, String>> = _txtContentCache.asStateFlow()

    private val apiService: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        Retrofit.Builder()
            .baseUrl("https://app-server-bsdr.onrender.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun fetchFiles() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = withTimeout(150000) {
                    apiService.getFiles()
                }
                _apiResponse.value = response
                // Fetch titles for all txt files
                fetchTxtTitles(response.files)
            } catch (e: TimeoutCancellationException) {
                _error.value = "Request timed out. Render servers may be cold-starting (this can take 1-2 minutes). Please try again."
            } catch (e: UnknownHostException) {
                _error.value = "No internet connection. Please check your network and try again."
            } catch (e: SocketTimeoutException) {
                _error.value = "Server is taking too long to respond. Render free tier servers may be sleeping. Please wait and try again."
            } catch (e: ConnectException) {
                _error.value = "Cannot connect to server. The Render server might be down or restarting."
            } catch (e: HttpException) {
                when (e.code()) {
                    503 -> _error.value = "Server temporarily unavailable. Render server might be starting up."
                    502, 504 -> _error.value = "Server gateway error. Please try again in a moment."
                    else -> _error.value = "Server error: ${e.code()} ${e.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch files: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchTxtTitles(files: Map<String, FileItem>) {
        val txtFiles = files.values.filter { it.name.endsWith(".txt", ignoreCase = true) }
        val titleMap = mutableMapOf<String, String>()

        txtFiles.forEach { file ->
            try {
                val content = withContext(Dispatchers.IO) {
                    // Use GitHub raw URL for direct access
                    val rawUrl = file.download_url
                    URL(rawUrl).readText()
                }
                val firstLine = content.lines().firstOrNull { it.trim().isNotEmpty() }?.trim()
                if (!firstLine.isNullOrEmpty()) {
                    // Clean up the first line (remove markdown formatting)
                    val cleanTitle = cleanMarkdownTitle(firstLine)
                    titleMap[file.path] = cleanTitle
                }
            } catch (e: Exception) {
                // If we can't fetch the content, use the filename
                titleMap[file.path] = file.name.removeSuffix(".txt")
            }
        }

        _txtContentCache.value = titleMap
    }

    private fun cleanMarkdownTitle(title: String): String {
        return title
            .replace(Regex("^#+\\s*"), "") // Remove markdown headers
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold formatting
            .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic formatting
            .trim()
    }

    fun retryFetch() {
        fetchFiles()
    }
}

// Navigation callback interface - matches your MainActivity exactly
interface FileNavigationCallback {
    fun navigateToTextFile(courseName: String, subjectName: String, chapterName: String, fileName: String)
    fun navigateBack()
}

// Main Screen Composable with navigation callback
// Main Screen Composable with navigation callback and initial path support
@Composable
fun FileSystemScreen(
    viewModel: FileSystemViewModel = viewModel(),
    navigationCallback: FileNavigationCallback? = null,
    initialPath: List<String> = emptyList() // Add initial path parameter
) {
    val apiResponse by viewModel.apiResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val txtContentCache by viewModel.txtContentCache.collectAsState()

    // Fetch data when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.fetchFiles()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back button at the top level
        if (navigationCallback != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigationCallback.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to previous screen")
                    }
                    Text(
                        text = "File Browser",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    // ... loading state ...
                }
                error != null -> {
                    // ... error state ...
                }
                apiResponse != null && apiResponse!!.success -> {
                    FileSystemBrowser(
                        files = apiResponse!!.files,
                        txtContentCache = txtContentCache,
                        navigationCallback = navigationCallback,
                        initialPath = initialPath // Pass initial path to browser
                    )
                }
                else -> {
                    Text(
                        text = "No data available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// Enhanced File System Browser Composable
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun FileSystemBrowser(
    files: Map<String, FileItem>,
    txtContentCache: Map<String, String>,
    navigationCallback: FileNavigationCallback? = null,
    initialPath: List<String> = emptyList() // Add initial path parameter
) {
    val rootNode = remember(files, txtContentCache) {
        buildEnhancedFileSystemTree(files, txtContentCache)
    }

    // Initialize navigation state with initial path
    val (currentPath, setCurrentPath) = remember { mutableStateOf(initialPath) }
    val (currentNode, setCurrentNode) = remember { mutableStateOf(rootNode) }
    val navigationStack = remember { mutableStateListOf(rootNode) }

    // Navigate to initial path when component is first created or when initialPath changes
    LaunchedEffect(initialPath, rootNode) {
        if (initialPath.isNotEmpty()) {
            // Navigate through the path to reach the target directory
            var tempNode = rootNode
            val tempStack = mutableListOf(rootNode)
            val tempPath = mutableListOf<String>()

            for (pathSegment in initialPath) {
                tempNode.children[pathSegment]?.let { nextNode ->
                    tempNode = nextNode
                    tempStack.add(nextNode)
                    tempPath.add(pathSegment)
                } ?: break // Stop if path segment doesn't exist
            }

            // Update the state if we successfully navigated
            if (tempPath.size == initialPath.size) {
                navigationStack.clear()
                navigationStack.addAll(tempStack)
                setCurrentNode(tempNode)
                setCurrentPath(tempPath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("File Browser", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (navigationStack.size > 1) {
                        IconButton(
                            onClick = {
                                navigationStack.removeAt(navigationStack.lastIndex)
                                setCurrentNode(navigationStack.last())
                                setCurrentPath(currentPath.dropLast(1))
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CurrentPathDisplay(currentPath)
            FileList(
                node = currentNode,
                onFolderClick = { folderName, folderNode ->
                    navigationStack.add(folderNode)
                    setCurrentNode(folderNode)
                    setCurrentPath(currentPath + folderName)
                },
                onFileClick = { fileItem ->
                    onFileClicked(fileItem, currentPath, navigationCallback)
                }
            )
        }
    }
}
// Current Path Display Composable
@Composable
fun CurrentPathDisplay(currentPath: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Path: ",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (currentPath.isEmpty()) "root" else currentPath.joinToString("/"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Enhanced File List Composable
@Composable
fun FileList(
    node: FileSystemNode,
    onFolderClick: (String, FileSystemNode) -> Unit,
    onFileClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Display directories first
        val directories = node.children.values.filter { it.isDirectory }.sortedBy { it.name }
        val files = node.children.values.filter { !it.isDirectory }.sortedBy { it.name }

        items(directories) { directory ->
            DirectoryItem(
                directory = directory,
                onClick = { onFolderClick(directory.name, directory) }
            )
        }

        if (files.isNotEmpty() && directories.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        items(files) { fileNode ->
            FileItemDisplay(
                fileNode = fileNode,
                onClick = { onFileClick(fileNode.fileItem!!) }
            )
        }
    }
}

// Enhanced Directory Item Composable
@Composable
fun DirectoryItem(directory: FileSystemNode, onClick: () -> Unit) {
    val fileCount = countItems(directory)
    val txtCount = countTxtFiles(directory)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = directory.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = buildString {
                        append("$fileCount items")
                        if (txtCount > 0) {
                            append(" • $txtCount txt files")
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Enhanced File Item Display Composable with title support
@Composable
fun FileItemDisplay(fileNode: FileSystemNode, onClick: () -> Unit) {
    val fileItem = fileNode.fileItem!!
    val isTextFile = fileItem.name.endsWith(".txt", ignoreCase = true)
    val displayName = if (isTextFile && !fileNode.displayTitle.isNullOrEmpty()) {
        fileNode.displayTitle!!
    } else {
        fileItem.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = "File",
                tint = if (isTextFile) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = buildString {
                        if (isTextFile && displayName != fileItem.name) {
                            append("${fileItem.name} • ")
                        }
                        append("Size: ${fileItem.size} bytes")
                        if (isTextFile) {
                            append(" • Tap to view")
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Enhanced utility function to build file system tree with titles
fun buildEnhancedFileSystemTree(
    files: Map<String, FileItem>,
    txtContentCache: Map<String, String>
): FileSystemNode {
    val root = FileSystemNode("root")

    files.forEach { (path, fileItem) ->
        val pathParts = path.split("/")
        var currentNode = root

        // Navigate through the directory structure
        for (i in 0 until pathParts.size - 1) {
            val part = pathParts[i]
            currentNode = currentNode.children.getOrPut(part) {
                FileSystemNode(part)
            }
        }

        // Add the file as a leaf node
        val fileName = pathParts.last()
        val displayTitle = if (fileItem.name.endsWith(".txt", ignoreCase = true)) {
            txtContentCache[path] ?: fileName.removeSuffix(".txt")
        } else {
            null
        }

        val fileNode = FileSystemNode(
            name = fileName,
            isDirectory = false,
            fileItem = fileItem,
            displayTitle = displayTitle
        )
        currentNode.children[fileName] = fileNode
    }

    return root
}

// Helper functions for counting items
fun countItems(node: FileSystemNode): Int {
    return node.children.size
}

fun countTxtFiles(node: FileSystemNode): Int {
    var count = 0
    fun countRecursive(currentNode: FileSystemNode) {
        currentNode.children.values.forEach { child ->
            if (!child.isDirectory && child.fileItem?.name?.endsWith(".txt", ignoreCase = true) == true) {
                count++
            } else if (child.isDirectory) {
                countRecursive(child)
            }
        }
    }
    countRecursive(node)
    return count
}

// File click handler that works with your existing MainActivity structure
private fun onFileClicked(
    fileItem: FileItem,
    currentPath: List<String>,
    navigationCallback: FileNavigationCallback?
) {
    println("File clicked: ${fileItem.name}")
    println("Download URL: ${fileItem.download_url}")
    println("Size: ${fileItem.size} bytes")
    println("Path: ${fileItem.path}")
    println("Current navigation path: ${currentPath.joinToString("/")}")

    // Check if it's a text file
    if (fileItem.name.endsWith(".txt", ignoreCase = true)) {
        navigationCallback?.let { callback ->
            // Convert dynamic path to the structure your MainActivity expects
            val courseName = currentPath.getOrNull(0) ?: "default_course"
            val subjectName = currentPath.getOrNull(1) ?: "default_subject"
            val chapterName = currentPath.getOrNull(2) ?: "default_chapter"

            callback.navigateToTextFile(courseName, subjectName, chapterName, fileItem.name)
        }
    }
}