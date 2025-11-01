package com.arjundubey.app

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var etaSeconds by mutableStateOf(0L)
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
    var extractionMethod by mutableStateOf("MediaRetriever")
    var autoDeleteVideo by mutableStateOf(false)

    fun initialize(context: android.content.Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(context)
                }
                isInitialized = true
                downloadStatus = "Ready to download. Update yt-dlp first!"
            } catch (e: Exception) {
                errorMessage = "Failed to initialize: ${e.message}"
                isInitialized = false
            }
        }
    }

    fun updateYoutubeDL(context: android.content.Context) {
        viewModelScope.launch {
            isUpdating = true
            downloadStatus = "Updating yt-dlp..."
            errorMessage = ""

            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().updateYoutubeDL(context)
                }
                downloadStatus = "yt-dlp updated successfully! Ready to download."
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Update failed: ${e.message}. Try downloading anyway."
                downloadStatus = "Update failed, but you can try downloading"
            } finally {
                isUpdating = false
            }
        }
    }

    fun startDownload(context: android.content.Context) {
        if (url.isBlank()) {
            errorMessage = "Please enter a valid URL"
            return
        }

        if (!isInitialized) {
            errorMessage = "Libraries not initialized. Please wait..."
            return
        }

        viewModelScope.launch {
            isDownloading = true
            errorMessage = ""
            downloadStatus = "Starting download..."
            downloadProgress = 0f
            downloadedVideoPath = null
            extractedFramesCount = 0

            try {
                val videoPath = withContext(Dispatchers.IO) {
                    val downloadDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "youtubedl-android"
                    )

                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs()
                    }

                    // Try 1080p first, then 720p, then best (like Mac script)
                    var downloadSuccess = false
                    var selectedQuality = ""

                    // Try 1080p
                    try {
                        downloadStatus = "Attempting to download 1080p..."
                        val request1080 = YoutubeDLRequest(url)
                        request1080.addOption("-f", "best[height<=1080]")
                        request1080.addOption("--merge-output-format", "mp4")
                        request1080.addOption("--no-check-certificate")
                        request1080.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                        YoutubeDL.getInstance().execute(request1080) { progress, eta, line ->
                            downloadProgress = progress / 100f
                            etaSeconds = eta
                            downloadStatus = "Downloading 1080p: ${progress}%"
                        }
                        selectedQuality = "1080p"
                        downloadSuccess = true
                        downloadStatus = "✅ Successfully downloaded 1080p"
                    } catch (e: Exception) {
                        android.util.Log.w("YouTubeDownloader", "1080p not available: ${e.message}")
                        downloadStatus = "1080p not available, trying 720p..."

                        // Try 720p
                        try {
                            val request720 = YoutubeDLRequest(url)
                            request720.addOption("-f", "best[height<=720]")
                            request720.addOption("--merge-output-format", "mp4")
                            request720.addOption("--no-check-certificate")
                            request720.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                            YoutubeDL.getInstance().execute(request720) { progress, eta, line ->
                                downloadProgress = progress / 100f
                                etaSeconds = eta
                                downloadStatus = "Downloading 720p: ${progress}%"
                            }
                            selectedQuality = "720p"
                            downloadSuccess = true
                            downloadStatus = "✅ Successfully downloaded 720p"
                        } catch (e2: Exception) {
                            android.util.Log.w("YouTubeDownloader", "720p not available: ${e2.message}")
                            downloadStatus = "720p not available, downloading best quality..."

                            // Try best available
                            val requestBest = YoutubeDLRequest(url)
                            requestBest.addOption("-f", "best")
                            requestBest.addOption("--merge-output-format", "mp4")
                            requestBest.addOption("--no-check-certificate")
                            requestBest.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                            YoutubeDL.getInstance().execute(requestBest) { progress, eta, line ->
                                downloadProgress = progress / 100f
                                etaSeconds = eta
                                downloadStatus = "Downloading best available: ${progress}%"
                            }
                            selectedQuality = "best available"
                            downloadSuccess = true
                            downloadStatus = "✅ Downloaded best available quality"
                        }
                    }

                    val downloadedFile = downloadDir.listFiles()
                        ?.filter { it.extension in listOf("mp4", "mkv", "webm") }
                        ?.maxByOrNull { it.lastModified() }
                        ?: throw Exception("Video file not found after download")

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(downloadedFile.absolutePath)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0

                        val resolutionLabel = when {
                            height >= 2160 -> "4K"
                            height >= 1440 -> "2K"
                            height >= 1080 -> "Full HD (1080p)"
                            height >= 720 -> "HD (720p)"
                            else -> "SD (${height}p)"
                        }

                        downloadStatus = "✅ Downloaded: ${width}x${height}\n" +
                                "Quality: $resolutionLabel\n" +
                                "Selected: $selectedQuality\n" +
                                "File: ${downloadedFile.name}\n" +
                                "Bitrate: ${bitrate / 1000} kbps"

                        android.util.Log.d("YouTubeDownloader", "Final resolution: ${width}x${height}, Requested: $selectedQuality")

                    } catch (e: Exception) {
                        android.util.Log.e("YouTubeDownloader", "Could not check resolution", e)
                        downloadStatus = "✅ Downloaded successfully\nFile: ${downloadedFile.name}"
                    } finally {
                        retriever.release()
                    }

                    downloadedFile.absolutePath
                }

                downloadProgress = 1f
                downloadedVideoPath = videoPath

                if (extractFrames && videoPath != null) {
                    extractFramesFromVideo(videoPath)
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"

                errorMessage = when {
                    errorMsg.contains("403") || errorMsg.contains("Forbidden") -> {
                        "YouTube blocked the request. Update yt-dlp and try again."
                    }
                    errorMsg.contains("nsig") -> {
                        "YouTube signature error. Please update yt-dlp using the Update button."
                    }
                    errorMsg.contains("SABR") -> {
                        "YouTube streaming format issue. Update yt-dlp and try again."
                    }
                    errorMsg.contains("formats") -> {
                        "Format selection error. Try updating yt-dlp or the video might not have HD."
                    }
                    errorMsg.contains("Requested format is not available") -> {
                        "HD format not available for this video. Downloading best available quality."
                    }
                    else -> {
                        "Download failed: $errorMsg"
                    }
                }
                downloadStatus = "Download failed"
                android.util.Log.e("YouTubeDownloader", "Download error", e)
            } finally {
                isDownloading = false
            }
        }
    }

    private suspend fun extractFramesFromVideo(videoPath: String) {
        isExtractingFrames = true
        extractionStatus = "Extracting frames using Android MediaRetriever..."

        try {
            withContext(Dispatchers.IO) {
                val videoFile = File(videoPath)
                val framesDir = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_frames")

                if (!framesDir.exists()) {
                    framesDir.mkdirs()
                }

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoPath)

                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    val durationSeconds = durationMs / 1000

                    extractionStatus = "Video duration: ${durationSeconds}s. Extracting frames..."

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
                                extractionStatus = "Extracted $frameCount frames..."
                            }
                        } catch (e: Exception) {
                            // Skip this frame and continue
                        }

                        currentSecond += frameInterval
                    }

                    extractedFramesCount = frameCount
                    extractionStatus = "✓ Extracted $extractedFramesCount frames to:\n${framesDir.absolutePath}"

                    // ===== AUTO-DELETE VIDEO AFTER EXTRACTION =====
                    if (autoDeleteVideo && frameCount > 0) {
                        try {
                            if (videoFile.delete()) {
                                extractionStatus += "\n✓ Video deleted automatically"
                                downloadedVideoPath = null
                            } else {
                                extractionStatus += "\n⚠️ Could not delete video file"
                            }
                        } catch (e: Exception) {
                            extractionStatus += "\n⚠️ Error deleting video: ${e.message}"
                        }
                    }

                } catch (e: Exception) {
                    extractionStatus = "Frame extraction error: ${e.message}"
                    errorMessage = "Could not extract frames: ${e.message}"
                } finally {
                    retriever.release()
                }
            }
        } catch (e: Exception) {
            extractionStatus = "Frame extraction error: ${e.message}"
            errorMessage = "Frame extraction failed: ${e.message}"
        } finally {
            isExtractingFrames = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeDownloaderScreen(
    viewModel: DownloadViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Downloader + Frame Extractor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Update Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ Important: Update First",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YouTube frequently changes their API. Update yt-dlp before downloading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.updateYoutubeDL(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.isInitialized && !viewModel.isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isUpdating) "Updating..." else "Update yt-dlp"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URL Input
            OutlinedTextField(
                value = viewModel.url,
                onValueChange = {
                    viewModel.url = it
                    viewModel.errorMessage = ""
                },
                label = { Text("YouTube URL") },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isDownloading,
                singleLine = true,
                isError = viewModel.errorMessage.isNotEmpty()
            )

            if (viewModel.errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frame Extraction Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extract Frames",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            checked = viewModel.extractFrames,
                            onCheckedChange = { viewModel.extractFrames = it },
                            enabled = !viewModel.isDownloading
                        )
                    }

                    if (viewModel.extractFrames) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Frame Interval: ${viewModel.frameInterval} seconds",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Slider(
                            value = viewModel.frameInterval.toFloat(),
                            onValueChange = { viewModel.frameInterval = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 28,
                            enabled = !viewModel.isDownloading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Extracts 1 frame every ${viewModel.frameInterval} seconds using Android MediaRetriever (no FFmpeg needed!)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-delete Video",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Delete video after frames extracted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Checkbox(
                                checked = viewModel.autoDeleteVideo,
                                onCheckedChange = { viewModel.autoDeleteVideo = it },
                                enabled = !viewModel.isDownloading
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Button
            Button(
                onClick = { viewModel.startDownload(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.isInitialized && !viewModel.isDownloading && !viewModel.isUpdating && viewModel.url.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !viewModel.isInitialized -> "Initializing..."
                        viewModel.isDownloading -> "Downloading..."
                        else -> "Download Video"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (viewModel.downloadedVideoPath != null && !viewModel.isExtractingFrames) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.extractFramesManually() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !viewModel.isDownloading && !viewModel.isExtractingFrames
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extract Frames Again",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Download Progress
            if (viewModel.isDownloading || viewModel.downloadProgress > 0f) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Download Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = viewModel.downloadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = viewModel.downloadStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        if (viewModel.isDownloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Progress: ${(viewModel.downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Frame Extraction Progress
            if (viewModel.isExtractingFrames || viewModel.extractionStatus.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Frame Extraction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewModel.isExtractingFrames) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = viewModel.extractionStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        if (viewModel.extractedFramesCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ Successfully extracted ${viewModel.extractedFramesCount} frames",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Uses Android's built-in MediaRetriever (No FFmpeg required!)\n" +
                                "• Videos saved to: Downloads/youtubedl-android/\n" +
                                "• Frames saved in: [video_name]_frames/ folder\n" +
                                "• Frame format: frame_0001.jpg, frame_0002.jpg, etc.\n" +
                                "• Always update yt-dlp before downloading\n" +
                                "• Requires storage and internet permissions\n" +
                                "• Lighter and faster than FFmpeg\n" +
                                "• Enable 'Auto-delete Video' to save storage space",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Troubleshooting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔧 Not Getting 1080p?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. UPDATE yt-dlp first (most important!)\n" +
                                "2. Check if video has 1080p on YouTube\n" +
                                "3. Try again after a few minutes\n" +
                                "4. Check logcat for format details\n" +
                                "5. Some videos have region/device restrictions\n" +
                                "6. YouTube may be rate-limiting your IP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}