package com.arjundubey.app

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DownloadViewModel : ViewModel() {
    var url by mutableStateOf("")
    var downloadProgress by mutableStateOf(0f)
    var isDownloading by mutableStateOf(false)
    var isExtractingFrames by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    var downloadStatus by mutableStateOf("")
    var extractionStatus by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var isInitialized by mutableStateOf(false)
    var extractFrames by mutableStateOf(true)
    var frameInterval by mutableStateOf(5)
    var downloadedVideoPath by mutableStateOf<String?>(null)
    var extractedFramesCount by mutableStateOf(0)
    var autoDeleteVideo by mutableStateOf(false)
    var downloadType by mutableStateOf("video")
    var fileSystemItems by mutableStateOf<List<FileSystemItem>>(emptyList())
    var showFileList by mutableStateOf(false)
    var currentPath by mutableStateOf<String?>(null)
    var pathHistory by mutableStateOf<List<String>>(emptyList())
    var selectedImagePath by mutableStateOf<String?>(null)
    var currentImageList by mutableStateOf<List<String>>(emptyList())
    var currentImageIndex by mutableStateOf(0)
    var selectedAudioPath by mutableStateOf<String?>(null)
    var showPasteDialog by mutableStateOf(false)
    var pasteDestination by mutableStateOf<String?>(null)
    var showFolderSelection by mutableStateOf(false)
    var availableFolders by mutableStateOf<List<FileSystemItem>>(emptyList())
    var selectedFolder by mutableStateOf<FileSystemItem?>(null)
    var newFolderName by mutableStateOf("")
    var currentFolderSelectionPath by mutableStateOf<String?>(null)
    var folderSelectionHistory by mutableStateOf<List<String>>(emptyList())
    var currentVideoUrl by mutableStateOf("")
    var selectedFrameInfo by mutableStateOf<Pair<Int, String>?>(null)

    // CRITICAL: Debounce refresh to prevent rapid file scans
    private var refreshJob: Job? = null


    fun initialize(context: android.content.Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(context)
                }
                isInitialized = true
                downloadStatus = "Ready to download"
                refreshFileList()
            } catch (e: Exception) {
                errorMessage = "Failed to initialize: ${e.message}"
                isInitialized = false
            }
        }
    }

    // OPTIMIZED: Instant UI update for cut
    fun cutItem(item: FileSystemItem) {
        ClipboardManager.cut(item)
        // Force instant recomposition without refreshing files
        fileSystemItems = fileSystemItems.toList()
    }

    // OPTIMIZED: Instant UI update for paste with background operation
    fun pasteItem(item: FileSystemItem, destinationPath: String) {
        // 1. Update UI IMMEDIATELY - remove from current list
        fileSystemItems = fileSystemItems.filter { it.path != item.path }
        ClipboardManager.clear()

        // 2. Do actual file move in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(item.path)
                val destDir = File(destinationPath)

                if (!destDir.exists()) {
                    destDir.mkdirs()
                }

                val destFile = File(destDir, sourceFile.name)

                // Try fast rename first
                val moved = sourceFile.renameTo(destFile)

                if (!moved) {
                    // Fallback to copy+delete for cross-device moves
                    if (sourceFile.isDirectory) {
                        sourceFile.copyRecursively(destFile, overwrite = true)
                        sourceFile.deleteRecursively()
                    } else {
                        sourceFile.copyTo(destFile, overwrite = true)
                        sourceFile.delete()
                    }
                }

                // Refresh to show in new location (debounced)
                debouncedRefresh()
            } catch (e: Exception) {
                errorMessage = "Failed to move item: ${e.message}"
                // Revert UI on error
                withContext(Dispatchers.Main) {
                    refreshFileList()
                }
            }
        }
    }

    fun pasteToCurrentDirectory() {
        ClipboardManager.cutItem?.let { cutItem ->
            val destination = currentPath ?: getDefaultDownloadDirectory()
            pasteItem(cutItem, destination)
        }
    }

    private fun getDefaultDownloadDirectory(): String {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "youtubedl-android"
        ).absolutePath
    }

    fun clearClipboard() {
        ClipboardManager.clear()
        fileSystemItems = fileSystemItems.toList() // Force recomposition
    }

    // OPTIMIZED: Debounced refresh prevents rapid consecutive scans
    private fun debouncedRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(200) // Wait 200ms for multiple operations to complete
            refreshFileList()
        }
    }

    // OPTIMIZED: Background refresh with minimal blocking
    fun refreshFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetDir = if (currentPath != null) {
                    File(currentPath!!)
                } else {
                    if (selectedFolder != null) {
                        File(selectedFolder!!.path)
                    } else {
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "youtubedl-android"
                        )
                    }
                }

                if (!targetDir.exists()) {
                    withContext(Dispatchers.Main) {
                        fileSystemItems = emptyList()
                    }
                    return@launch
                }

                // Load metadata once
                val metadataFile = File(targetDir, ".video_metadata")
                val metadata = if (metadataFile.exists()) {
                    try {
                        val lines = metadataFile.readLines()
                        val url = lines.find { it.startsWith("URL=") }?.substringAfter("URL=")
                        val interval = lines.find { it.startsWith("INTERVAL=") }?.substringAfter("INTERVAL=")?.toIntOrNull()
                        Pair(url, interval)
                    } catch (e: Exception) {
                        Pair(null, null)
                    }
                } else {
                    Pair(null, null)
                }

                // Fast file scan
                val files = targetDir.listFiles() ?: emptyArray()
                val items = files
                    .filter { !it.name.startsWith(".") }
                    .map { file ->
                        if (file.isDirectory) {
                            val fileCount = try {
                                file.listFiles()?.size ?: 0
                            } catch (e: Exception) {
                                0
                            }

                            FileSystemItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = 0L, // Don't calculate folder size - it's slow
                                lastModified = file.lastModified(),
                                type = "Folder",
                                isDirectory = true,
                                itemCount = fileCount
                            )
                        } else {
                            val extension = file.extension.lowercase()
                            val type = when (extension) {
                                "mp4", "mkv", "avi", "mov" -> "Video"
                                "mp3", "m4a", "opus", "ogg", "wav", "webm" -> "Audio"
                                "jpg", "jpeg", "png" -> "Image"
                                else -> "File"
                            }

                            val frameNumber = if (type == "Image" && file.name.startsWith("frame_")) {
                                file.nameWithoutExtension.substringAfter("frame_").toIntOrNull()
                            } else null

                            FileSystemItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                type = type,
                                isDirectory = false,
                                frameNumber = frameNumber,
                                videoUrl = if (frameNumber != null) metadata.first else null
                            )
                        }
                    }
                    .sortedWith(
                        compareByDescending<FileSystemItem> { it.isDirectory }
                            .thenByDescending { it.lastModified }
                    )

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    fileSystemItems = items
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error refreshing file list", e)
            }
        }
    }

    // OPTIMIZED: Instant navigation with background load
    fun openFolder(folderPath: String) {
        pathHistory = pathHistory + (currentPath ?: "")
        currentPath = folderPath
        fileSystemItems = emptyList() // Clear immediately to show loading state

        viewModelScope.launch(Dispatchers.IO) {
            refreshFileList()
        }
    }

    fun openFolderSelection() {
        loadAvailableFolders()
        folderSelectionHistory = emptyList()
        showFolderSelection = true
    }

    // OPTIMIZED: Instant back navigation
    fun goBack() {
        if (pathHistory.isNotEmpty()) {
            currentPath = pathHistory.lastOrNull()?.takeIf { it.isNotEmpty() }
            pathHistory = pathHistory.dropLast(1)
            fileSystemItems = emptyList() // Clear for instant feedback

            viewModelScope.launch(Dispatchers.IO) {
                refreshFileList()
            }
        }
    }

    fun openImage(imagePath: String) {
        selectedImagePath = imagePath
        val imageFile = File(imagePath)
        val parentDir = imageFile.parentFile

        if (parentDir != null && parentDir.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                val imageExtensions = listOf("jpg", "jpeg", "png")

                val metadataFile = File(parentDir, ".video_metadata")
                val metadata = if (metadataFile.exists()) {
                    try {
                        val lines = metadataFile.readLines()
                        val url = lines.find { it.startsWith("URL=") }?.substringAfter("URL=")
                        val interval = lines.find { it.startsWith("INTERVAL=") }?.substringAfter("INTERVAL=")?.toIntOrNull() ?: frameInterval
                        Pair(url, interval)
                    } catch (e: Exception) {
                        Pair(null, frameInterval)
                    }
                } else {
                    Pair(null, frameInterval)
                }

                val images = parentDir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in imageExtensions }
                    ?.sortedBy { it.name }
                    ?.map { it.absolutePath }
                    ?: emptyList()

                val index = images.indexOf(imagePath).coerceAtLeast(0)
                val frameNumber = imageFile.nameWithoutExtension.substringAfter("frame_").toIntOrNull()
                val frameInfo = if (frameNumber != null && metadata.first != null) {
                    Pair(frameNumber, metadata.first!!)
                } else null

                withContext(Dispatchers.Main) {
                    currentImageList = images
                    currentImageIndex = index
                    selectedFrameInfo = frameInfo
                }
            }
        }
    }

    fun closeImage() {
        selectedImagePath = null
        currentImageList = emptyList()
        currentImageIndex = 0
    }

    fun openAudio(audioPath: String) {
        selectedAudioPath = audioPath
    }

    fun closeAudio() {
        selectedAudioPath = null
    }

    private fun updateFrameInfo() {
        selectedImagePath?.let { path ->
            val imageFile = File(path)
            val parentDir = imageFile.parentFile

            if (parentDir != null && parentDir.exists()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val metadataFile = File(parentDir, ".video_metadata")
                    val url = if (metadataFile.exists()) {
                        metadataFile.readLines().find { it.startsWith("URL=") }?.substringAfter("URL=")
                    } else null

                    val frameNumber = imageFile.nameWithoutExtension.substringAfter("frame_").toIntOrNull()

                    withContext(Dispatchers.Main) {
                        selectedFrameInfo = if (frameNumber != null && url != null) {
                            Pair(frameNumber, url)
                        } else null
                    }
                }
            }
        }
    }

    fun nextImage() {
        if (currentImageList.isNotEmpty() && currentImageIndex < currentImageList.size - 1) {
            currentImageIndex++
            selectedImagePath = currentImageList[currentImageIndex]
            updateFrameInfo()
        }
    }

    fun previousImage() {
        if (currentImageList.isNotEmpty() && currentImageIndex > 0) {
            currentImageIndex--
            selectedImagePath = currentImageList[currentImageIndex]
            updateFrameInfo()
        }
    }

    private fun calculateFolderSize(folder: File): Long {
        var size = 0L
        try {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateFolderSize(file)
                } else {
                    file.length()
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        return size
    }

    // OPTIMIZED: Instant delete with background operation
    fun deleteItem(itemPath: String) {
        // 1. Update UI IMMEDIATELY
        fileSystemItems = fileSystemItems.filter { it.path != itemPath }

        // 2. Do actual deletion in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(itemPath)
                if (file.exists()) {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error deleting file", e)
                // Revert UI on error
                withContext(Dispatchers.Main) {
                    refreshFileList()
                }
            }
        }
    }

    fun updateYoutubeDL(context: android.content.Context) {
        viewModelScope.launch {
            isUpdating = true
            downloadStatus = "Updating..."
            errorMessage = ""

            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().updateYoutubeDL(context)
                }
                downloadStatus = "Updated successfully"
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Update failed: ${e.message}"
                downloadStatus = "Update failed"
            } finally {
                isUpdating = false
            }
        }
    }

    fun startDownload(context: android.content.Context) {
        if (url.isBlank()) {
            errorMessage = "Please enter a URL"
            return
        }

        if (!isInitialized) {
            errorMessage = "Not initialized"
            return
        }

        currentVideoUrl = url
        openFolderSelection()
    }

    fun createNewFolder() {
        if (newFolderName.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val baseDir = if (currentFolderSelectionPath != null) {
                    File(currentFolderSelectionPath!!)
                } else {
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "youtubedl-android"
                    )
                }

                val newFolder = File(baseDir, newFolderName)
                if (!newFolder.exists()) {
                    newFolder.mkdirs()

                    withContext(Dispatchers.Main) {
                        selectedFolder = FileSystemItem(
                            name = newFolder.name,
                            path = newFolder.absolutePath,
                            size = 0,
                            lastModified = newFolder.lastModified(),
                            type = "Folder",
                            isDirectory = true,
                            itemCount = 0
                        )
                        newFolderName = ""
                    }

                    loadAvailableFolders(baseDir.absolutePath)
                }
            }
        }
    }

    fun confirmDownload(context: android.content.Context) {
        showFolderSelection = false
        val selectedFolderPath = selectedFolder?.path

        viewModelScope.launch {
            isDownloading = true
            errorMessage = ""
            downloadStatus = "Starting download..."
            downloadProgress = 0f
            downloadedVideoPath = null
            extractedFramesCount = 0

            try {
                if (downloadType == "audio") {
                    downloadAudioOnly(context)
                } else {
                    downloadVideo(context)
                }
                debouncedRefresh() // Use debounced refresh
            } catch (e: Exception) {
                handleDownloadError(e)
            } finally {
                isDownloading = false
            }
        }
    }

    private suspend fun downloadVideo(context: android.content.Context) {
        val videoPath = withContext(Dispatchers.IO) {
            val downloadDir = if (selectedFolder != null) {
                File(selectedFolder!!.path)
            } else {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "youtubedl-android"
                )
            }

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            var downloadSuccess = false
            var selectedQuality = ""

            try {
                downloadStatus = "Downloading 1080p..."
                val request1080 = YoutubeDLRequest(url)
                request1080.addOption("-f", "best[height<=1080]")
                request1080.addOption("--merge-output-format", "mp4")
                request1080.addOption("--no-check-certificate")
                request1080.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(request1080) { progress, eta, line ->
                    downloadProgress = progress / 100f
                    downloadStatus = "Downloading: ${progress}%"
                }
                selectedQuality = "1080p"
                downloadSuccess = true
                downloadStatus = "Downloaded 1080p"
            } catch (e: Exception) {
                try {
                    downloadStatus = "Downloading 720p..."
                    val request720 = YoutubeDLRequest(url)
                    request720.addOption("-f", "best[height<=720]")
                    request720.addOption("--merge-output-format", "mp4")
                    request720.addOption("--no-check-certificate")
                    request720.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                    YoutubeDL.getInstance().execute(request720) { progress, eta, line ->
                        downloadProgress = progress / 100f
                        downloadStatus = "Downloading: ${progress}%"
                    }
                    selectedQuality = "720p"
                    downloadSuccess = true
                    downloadStatus = "Downloaded 720p"
                } catch (e2: Exception) {
                    downloadStatus = "Downloading best quality..."
                    val requestBest = YoutubeDLRequest(url)
                    requestBest.addOption("-f", "best")
                    requestBest.addOption("--merge-output-format", "mp4")
                    requestBest.addOption("--no-check-certificate")
                    requestBest.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                    YoutubeDL.getInstance().execute(requestBest) { progress, eta, line ->
                        downloadProgress = progress / 100f
                        downloadStatus = "Downloading: ${progress}%"
                    }
                    selectedQuality = "best available"
                    downloadSuccess = true
                    downloadStatus = "Downloaded best quality"
                }
            }

            val downloadedFile = downloadDir.listFiles()
                ?.filter { it.extension in listOf("mp4", "mkv", "webm") }
                ?.maxByOrNull { it.lastModified() }
                ?: throw Exception("Video file not found")

            downloadedFile.absolutePath
        }

        downloadProgress = 1f
        downloadedVideoPath = videoPath

        if (extractFrames && videoPath != null) {
            extractFramesFromVideo(videoPath)
        }
    }

// Add these methods to your DownloadViewModel class

    fun navigateToFolderInSelection(folderPath: String) {
        // Add current path to history before navigating
        currentFolderSelectionPath?.let { currentPath ->
            folderSelectionHistory = folderSelectionHistory + currentPath
        }
        loadAvailableFolders(folderPath)
    }

    fun goBackInFolderSelection() {
        if (folderSelectionHistory.isNotEmpty()) {
            val previousPath = folderSelectionHistory.last()
            folderSelectionHistory = folderSelectionHistory.dropLast(1)
            loadAvailableFolders(previousPath)
        }
    }

    fun goToRootInFolderSelection() {
        // Clear history and go to root
        folderSelectionHistory = emptyList()
        loadAvailableFolders()
    }

    fun loadAvailableFolders(targetPath: String? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val baseDir = if (targetPath != null) {
                    File(targetPath)
                } else {
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "youtubedl-android"
                    )
                }

                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }

                val folders = baseDir.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.map { file ->
                        FileSystemItem(
                            name = file.name,
                            path = file.absolutePath,
                            size = 0L,
                            lastModified = file.lastModified(),
                            type = "Folder",
                            isDirectory = true,
                            itemCount = file.listFiles()?.size ?: 0
                        )
                    }
                    ?.sortedBy { it.name }
                    ?: emptyList()

                withContext(Dispatchers.Main) {
                    availableFolders = folders
                    currentFolderSelectionPath = baseDir.absolutePath

                    // Only auto-select if no folder is selected yet or we explicitly changed path
                    if (selectedFolder == null || targetPath != null) {
                        selectedFolder = FileSystemItem(
                            name = baseDir.name,
                            path = baseDir.absolutePath,
                            size = 0L,
                            lastModified = baseDir.lastModified(),
                            type = "Folder",
                            isDirectory = true,
                            itemCount = baseDir.listFiles()?.size ?: 0
                        )
                    }
                }
            }
        }
    }



    private suspend fun downloadAudioOnly(context: android.content.Context) {
        withContext(Dispatchers.IO) {
            val downloadDir = if (selectedFolder != null) {
                File(selectedFolder!!.path)
            } else {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "youtubedl-android"
                )
            }

            try {
                downloadStatus = "Downloading audio..."
                val requestAudio = YoutubeDLRequest(url)
                requestAudio.addOption("-x")
                requestAudio.addOption("--audio-format", "mp3")
                requestAudio.addOption("--audio-quality", "0")
                requestAudio.addOption("--no-check-certificate")
                requestAudio.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(requestAudio) { progress, eta, line ->
                    downloadProgress = progress / 100f
                    downloadStatus = "Downloading: ${progress}%"
                }
                downloadStatus = "Audio downloaded"
            } catch (e: Exception) {
                val requestDirectAudio = YoutubeDLRequest(url)
                requestDirectAudio.addOption("-f", "bestaudio/best")
                requestDirectAudio.addOption("--no-check-certificate")
                requestDirectAudio.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(requestDirectAudio) { progress, eta, line ->
                    downloadProgress = progress / 100f
                    downloadStatus = "Downloading: ${progress}%"
                }
                downloadStatus = "Audio downloaded"
            }

            downloadProgress = 1f
            downloadStatus = "Audio download complete"
        }
    }

    private fun handleDownloadError(e: Exception) {
        val errorMsg = e.message ?: "Unknown error"
        errorMessage = "Download failed: $errorMsg"
        downloadStatus = "Download failed"
    }

    private suspend fun extractFramesFromVideo(videoPath: String) {
        isExtractingFrames = true
        extractionStatus = "Extracting frames..."

        try {
            withContext(Dispatchers.IO) {
                val videoFile = File(videoPath)
                val baseFramesDir = if (selectedFolder != null) {
                    File(selectedFolder!!.path)
                } else {
                    videoFile.parentFile
                }

                val framesDir = File(baseFramesDir, "${videoFile.nameWithoutExtension}_frames")

                if (!framesDir.exists()) {
                    framesDir.mkdirs()
                }

                val metadataFile = File(framesDir, ".video_metadata")
                metadataFile.writeText("URL=$currentVideoUrl\nINTERVAL=$frameInterval")

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoPath)

                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    val durationSeconds = durationMs / 1000

                    extractionStatus = "Extracting frames..."

                    var frameCount = 0
                    var currentSecond = 0L

                    while (currentSecond < durationSeconds) {
                        try {
                            val timeUs = currentSecond * 1_000_000L
                            val bitmap = retriever.getFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST
                            )

                            if (bitmap != null) {
                                frameCount++
                                val outputFile = File(framesDir, "frame_${String.format("%04d", frameCount)}.jpg")

                                FileOutputStream(outputFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }

                                bitmap.recycle()

                                withContext(Dispatchers.Main) {
                                    extractionStatus = "Extracted $frameCount frames"
                                }
                            }
                        } catch (e: Exception) {
                            // Skip this frame
                        }

                        currentSecond += frameInterval
                    }

                    withContext(Dispatchers.Main) {
                        extractedFramesCount = frameCount
                        extractionStatus = "Extracted $extractedFramesCount frames"
                    }

                    if (autoDeleteVideo && frameCount > 0) {
                        try {
                            videoFile.delete()
                            withContext(Dispatchers.Main) {
                                downloadedVideoPath = null
                            }
                        } catch (e: Exception) {
                            // Ignore deletion errors
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        extractionStatus = "Extraction error: ${e.message}"
                    }
                } finally {
                    retriever.release()
                }
            }
        } catch (e: Exception) {
            extractionStatus = "Extraction failed: ${e.message}"
        } finally {
            isExtractingFrames = false
            debouncedRefresh() // Use debounced refresh
        }
    }

    fun extractFramesManually() {
        downloadedVideoPath?.let { path ->
            viewModelScope.launch {
                extractFramesFromVideo(path)
            }
        }
    }
}