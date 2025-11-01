package com.arjundubey.app

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
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

    fun refreshFileList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val targetDir = if (currentPath != null) {
                        File(currentPath!!)
                    } else {
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "youtubedl-android"
                        )
                    }

                    if (targetDir.exists()) {
                        // Check if current directory has metadata file
                        val metadataFile = File(targetDir, ".video_metadata")
                        val metadata = if (metadataFile.exists()) {
                            val lines = metadataFile.readLines()
                            val url = lines.find { it.startsWith("URL=") }?.substringAfter("URL=")
                            val interval = lines.find { it.startsWith("INTERVAL=") }?.substringAfter("INTERVAL=")?.toIntOrNull()
                            Pair(url, interval)
                        } else {
                            Pair(null, null)
                        }

                        val items = targetDir.listFiles()
                            ?.filter { !it.name.startsWith(".") } // Hide metadata files
                            ?.map { file ->
                                if (file.isDirectory) {
                                    val fileCount = file.listFiles()?.size ?: 0
                                    FileSystemItem(
                                        name = file.name,
                                        path = file.absolutePath,
                                        size = calculateFolderSize(file),
                                        lastModified = file.lastModified(),
                                        type = "Folder",
                                        isDirectory = true,
                                        itemCount = fileCount
                                    )
                                } else {
                                    val type = when (file.extension.lowercase()) {
                                        "mp4", "mkv", "avi", "mov" -> "Video"
                                        "mp3", "m4a", "opus", "ogg", "wav", "webm" -> "Audio"
                                        "jpg", "jpeg", "png" -> "Image"
                                        else -> "File"
                                    }

                                    // Extract frame number from filename
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
                            ?.sortedWith(compareByDescending<FileSystemItem> { it.isDirectory }.thenByDescending { it.lastModified })
                            ?: emptyList()

                        fileSystemItems = items
                    }
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }

    fun openFolder(folderPath: String) {
        pathHistory = pathHistory + (currentPath ?: "")
        currentPath = folderPath
        refreshFileList()
    }

    fun goBack() {
        if (pathHistory.isNotEmpty()) {
            currentPath = pathHistory.lastOrNull()?.takeIf { it.isNotEmpty() }
            pathHistory = pathHistory.dropLast(1)
            refreshFileList()
        }
    }

    fun openImage(imagePath: String) {
        selectedImagePath = imagePath

        // Get all images in the same directory
        val imageFile = File(imagePath)
        val parentDir = imageFile.parentFile

        if (parentDir != null && parentDir.exists()) {
            val imageExtensions = listOf("jpg", "jpeg", "png")

            // Load metadata
            val metadataFile = File(parentDir, ".video_metadata")
            val metadata = if (metadataFile.exists()) {
                val lines = metadataFile.readLines()
                val url = lines.find { it.startsWith("URL=") }?.substringAfter("URL=")
                val interval = lines.find { it.startsWith("INTERVAL=") }?.substringAfter("INTERVAL=")?.toIntOrNull() ?: frameInterval
                Pair(url, interval)
            } else {
                Pair(null, frameInterval)
            }

            currentImageList = parentDir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in imageExtensions }
                ?.sortedBy { it.name }
                ?.map { it.absolutePath }
                ?: emptyList()

            currentImageIndex = currentImageList.indexOf(imagePath).coerceAtLeast(0)

            // Extract frame number and set frame info
            val frameNumber = imageFile.nameWithoutExtension.substringAfter("frame_").toIntOrNull()
            if (frameNumber != null && metadata.first != null) {
                selectedFrameInfo = Pair(frameNumber, metadata.first!!)
            } else {
                selectedFrameInfo = null
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
                val metadataFile = File(parentDir, ".video_metadata")
                val url = if (metadataFile.exists()) {
                    metadataFile.readLines().find { it.startsWith("URL=") }?.substringAfter("URL=")
                } else null

                val frameNumber = imageFile.nameWithoutExtension.substringAfter("frame_").toIntOrNull()
                if (frameNumber != null && url != null) {
                    selectedFrameInfo = Pair(frameNumber, url)
                } else {
                    selectedFrameInfo = null
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
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun deleteItem(itemPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(itemPath)
                    if (file.exists()) {
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                        refreshFileList()
                    }
                } catch (e: Exception) {
                    // Handle error
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

        currentVideoUrl = url  // NEW: Save the URL

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
                refreshFileList()
            } catch (e: Exception) {
                handleDownloadError(e)
            } finally {
                isDownloading = false
            }
        }
    }


    private suspend fun downloadVideo(context: android.content.Context) {
        val videoPath = withContext(Dispatchers.IO) {
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "youtubedl-android"
            )

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            var downloadSuccess = false
            var selectedQuality = ""

            // Try 1080p
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
                // Try 720p
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
                    // Try best available
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

    private suspend fun downloadAudioOnly(context: android.content.Context) {
        withContext(Dispatchers.IO) {
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "youtubedl-android"
            )

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
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

    var currentVideoUrl by mutableStateOf("")  // NEW: Store current video URL
    var selectedFrameInfo by mutableStateOf<Pair<Int, String>?>(null)  // NEW: (frameNumber, videoUrl)

    // Modify extractFramesFromVideo to include metadata
    private suspend fun extractFramesFromVideo(videoPath: String) {
        isExtractingFrames = true
        extractionStatus = "Extracting frames..."

        try {
            withContext(Dispatchers.IO) {
                val videoFile = File(videoPath)
                val framesDir = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_frames")

                if (!framesDir.exists()) {
                    framesDir.mkdirs()
                }

                // NEW: Save video URL to metadata file
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
                                extractionStatus = "Extracted $frameCount frames"
                            }
                        } catch (e: Exception) {
                            // Skip this frame
                        }

                        currentSecond += frameInterval
                    }

                    extractedFramesCount = frameCount
                    extractionStatus = "Extracted $extractedFramesCount frames"

                    if (autoDeleteVideo && frameCount > 0) {
                        try {
                            videoFile.delete()
                            downloadedVideoPath = null
                        } catch (e: Exception) {
                            // Ignore deletion errors
                        }
                    }

                } catch (e: Exception) {
                    extractionStatus = "Extraction error: ${e.message}"
                } finally {
                    retriever.release()
                }
            }
        } catch (e: Exception) {
            extractionStatus = "Extraction failed: ${e.message}"
        } finally {
            isExtractingFrames = false
            refreshFileList()
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