package com.arjundubey.app
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.view.Surface
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeFrameExtractorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var youtubeUrl by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("1") }
    var frameInterval by remember { mutableStateOf("5") }
    var isProcessing by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf(0f) }
    var totalFrames by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Frame Extractor") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // YouTube URL Input
            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = {
                    youtubeUrl = it
                    errorMessage = null
                    successMessage = null
                },
                label = { Text("YouTube URL") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                singleLine = true,
                placeholder = { Text("https://youtube.com/watch?v=...") }
            )

            // Subject Name
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                singleLine = true,
                supportingText = { Text("Default: 1") }
            )

            // Frame Interval
            OutlinedTextField(
                value = frameInterval,
                onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) frameInterval = it },
                label = { Text("Frame Interval (seconds)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                singleLine = true,
                supportingText = { Text("📊 Extract 1 frame every N seconds") }
            )

            // Extract Button
            Button(
                onClick = {
                    if (youtubeUrl.isNotBlank()) {
                        scope.launch {
                            extractFramesFromDirectVideoUrl(
                                context = context,
                                videoUrl = youtubeUrl,
                                subjectName = subjectName.ifBlank { "1" },
                                intervalSeconds = frameInterval.toIntOrNull() ?: 5,
                                onProgress = { message, frames, progress ->
                                    progressMessage = message
                                    totalFrames = frames
                                    downloadProgress = progress
                                },
                                onStart = {
                                    isProcessing = true
                                    errorMessage = null
                                    successMessage = null
                                    totalFrames = 0
                                    downloadProgress = 0f
                                },
                                onComplete = { isProcessing = false },
                                onError = { error ->
                                    isProcessing = false
                                    errorMessage = error
                                },
                                onSuccess = { message ->
                                    successMessage = message
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && youtubeUrl.isNotBlank() && frameInterval.isNotBlank()
            ) {
                Text("Extract Frames")
            }

            // Progress/Status
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (downloadProgress > 0 && downloadProgress < 100) {
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${downloadProgress.toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            CircularProgressIndicator()
                        }

                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (totalFrames > 0) {
                            Text(
                                text = "Frames extracted: $totalFrames",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Success Message
            successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "❌ $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ℹ️ Information",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• Uses Android MediaMetadataRetriever",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Works with direct video URLs",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Frames saved to app's private storage",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• No external dependencies",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Video URL Parser for direct video links
object DirectVideoUrlParser {
    fun isDirectVideoUrl(url: String): Boolean {
        return url.endsWith(".mp4") || url.endsWith(".webm") || url.endsWith(".mkv") ||
                url.contains("/video/") || url.contains("/v/")
    }

    fun extractVideoIdFromYouTube(url: String): String? {
        val patterns = listOf(
            "youtube\\.com/watch\\?v=([^&]+)",
            "youtu\\.be/([^?]+)",
            "youtube\\.com/embed/([^?]+)"
        )

        for (pattern in patterns) {
            val regex = pattern.toRegex()
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
}

private suspend fun extractFramesFromDirectVideoUrl(
    context: Context,
    videoUrl: String,
    subjectName: String,
    intervalSeconds: Int,
    onProgress: (String, Int, Float) -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        onStart()

        // Create output directory
        val baseFolder = File(context.getExternalFilesDir(null), "examinepictures/$subjectName")
        if (!baseFolder.exists()) {
            baseFolder.mkdirs()
        }

        withContext(Dispatchers.Main) {
            onProgress("🔍 Processing video URL...", 0, 10f)
        }

        // For YouTube URLs, we need to get direct video URL first
        val finalVideoUrl = if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            // Try to get direct URL or use alternative approach
            getYouTubeDirectUrl(videoUrl) ?: videoUrl
        } else {
            videoUrl
        }

        withContext(Dispatchers.Main) {
            onProgress("🎞️ Extracting frames...", 0, 30f)
        }

        // Use MediaMetadataRetriever for frame extraction
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(finalVideoUrl, HashMap())
        } catch (e: Exception) {
            // If network source fails, try downloading first
            try {
                withContext(Dispatchers.Main) {
                    onProgress("⬇️ Downloading video...", 0, 50f)
                }

                val tempFile = downloadVideoToTemp(context, finalVideoUrl)
                if (tempFile != null && tempFile.exists()) {
                    retriever.setDataSource(tempFile.absolutePath)
                } else {
                    throw Exception("Cannot access video source")
                }
            } catch (downloadError: Exception) {
                throw Exception("Cannot access video: ${downloadError.message}")
            }
        }

        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationString?.toLongOrNull() ?: 0L

        if (durationMs <= 0) {
            retriever.release()
            withContext(Dispatchers.Main) {
                onError("Unable to read video duration")
            }
            return@withContext
        }

        val intervalMs = intervalSeconds * 1000L
        var frameCount = 0
        var currentTimeMs = 0L

        while (currentTimeMs < durationMs) {
            try {
                val frame = retriever.getFrameAtTime(
                    currentTimeMs * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                frame?.let { bitmap ->
                    frameCount++
                    val outputFile = File(baseFolder, "frame_%04d.jpg".format(frameCount))

                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }

                    bitmap.recycle()

                    // Update progress every 5 frames
                    if (frameCount % 5 == 0) {
                        val progress = 30f + (70f * currentTimeMs / durationMs)
                        withContext(Dispatchers.Main) {
                            onProgress("🎞️ Extracted $frameCount frames...", frameCount, progress)
                        }
                    }
                }

                currentTimeMs += intervalMs
            } catch (e: Exception) {
                // Continue with next frame if this one fails
                currentTimeMs += intervalMs
            }
        }

        retriever.release()

        // Clean up temp files
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("temp_video_")) {
                file.delete()
            }
        }

        withContext(Dispatchers.Main) {
            if (frameCount > 0) {
                onSuccess("✅ Frames saved in ${baseFolder.absolutePath}\nTotal frames: $frameCount")
            } else {
                onError("No frames were extracted. The video might be protected or inaccessible.")
            }
            onComplete()
        }

    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError("Error: ${e.message ?: "Unknown error occurred"}")
            onComplete()
        }
    }
}

private suspend fun downloadVideoToTemp(context: Context, videoUrl: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")

            val url = URL(videoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(4 * 1024) // 4KB buffer
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            null
        }
    }
}

private fun getYouTubeDirectUrl(youtubeUrl: String): String? {
    // This is a simplified version - in practice, you'd need to:
    // 1. Use YouTube Data API
    // 2. Or use a web service that provides direct URLs
    // 3. Or parse the YouTube page HTML

    val videoId = DirectVideoUrlParser.extractVideoIdFromYouTube(youtubeUrl)
    return if (videoId != null) {
        // These are thumbnail URLs, not video URLs
        // For actual video URLs, you'd need proper extraction
        "https://img.youtube.com/vi/$videoId/0.jpg"
    } else {
        null
    }
}

// Alternative: Extract thumbnails from YouTube
private suspend fun extractYouTubeThumbnails(
    context: Context,
    youtubeUrl: String,
    subjectName: String,
    onProgress: (String, Int, Float) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val videoId = DirectVideoUrlParser.extractVideoIdFromYouTube(youtubeUrl)
        if (videoId == null) {
            withContext(Dispatchers.Main) {
                onError("Invalid YouTube URL")
            }
            return@withContext
        }

        val baseFolder = File(context.getExternalFilesDir(null), "examinepictures/$subjectName")
        if (!baseFolder.exists()) {
            baseFolder.mkdirs()
        }

        // YouTube provides multiple thumbnail qualities
        val thumbnailUrls = listOf(
            "https://img.youtube.com/vi/$videoId/maxresdefault.jpg", // Highest quality
            "https://img.youtube.com/vi/$videoId/sddefault.jpg",    // Standard quality
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg",    // High quality
            "https://img.youtube.com/vi/$videoId/mqdefault.jpg",    // Medium quality
            "https://img.youtube.com/vi/$videoId/default.jpg"       // Default quality
        )

        var downloadedCount = 0

        thumbnailUrls.forEachIndexed { index, url ->
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val outputFile = File(baseFolder, "thumbnail_${index + 1}.jpg")
                    val outputStream = FileOutputStream(outputFile)

                    inputStream.copyTo(outputStream)
                    outputStream.close()
                    inputStream.close()

                    downloadedCount++
                }
            } catch (e: Exception) {
                // Continue with next thumbnail
            }
        }

        withContext(Dispatchers.Main) {
            if (downloadedCount > 0) {
                onSuccess("✅ Downloaded $downloadedCount thumbnails to ${baseFolder.absolutePath}")
            } else {
                onError("No thumbnails could be downloaded")
            }
        }

    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError("Error extracting thumbnails: ${e.message}")
        }
    }
}