package com.arjundubey.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farimarwat.commons.VideoInfo
import com.farimarwat.commons.YoutubeDLRequest
import com.farimarwat.commons.YoutubeDLResponse
import com.farimarwat.library.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ViewModel
class YoutubeFrameExtractorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState.asStateFlow()

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _extractedFrames = MutableStateFlow<List<Bitmap>>(emptyList())
    val extractedFrames: StateFlow<List<Bitmap>> = _extractedFrames.asStateFlow()

    private var youtubeDl: YoutubeDL? = null
    private var currentDownloadJob: kotlinx.coroutines.Job? = null
    private var downloadedVideoPath: String? = null

    fun initializeYoutubeDL(context: Context) {
        viewModelScope.launch {
            _uiState.value = DownloadState.Initializing
            YoutubeDL.init(
                appContext = context,
                withFfmpeg = true,
                withAria2c = false,
                onSuccess = {
                    youtubeDl = it
                    _uiState.value = DownloadState.Idle
                },
                onError = { error ->
                    _uiState.value = DownloadState.Error("Failed to initialize: $error")
                }
            )
        }
    }

    fun getVideoInfo(url: String) {
        if (youtubeDl == null) {
            _uiState.value = DownloadState.Error("YouTubeDL not initialized")
            return
        }

        viewModelScope.launch {
            _uiState.value = DownloadState.FetchingInfo
            youtubeDl?.getInfo(
                url = url,
                onSuccess = { info ->
                    _videoInfo.value = info
                    _uiState.value = DownloadState.InfoReady(info)
                },
                onError = { error ->
                    _uiState.value = DownloadState.Error("Failed to get video info: $error")
                }
            )
        }
    }

    fun downloadAndExtractFrames(context: Context, url: String) {
        if (youtubeDl == null) {
            _uiState.value = DownloadState.Error("YouTubeDL not initialized")
            return
        }

        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = DownloadState.Downloading(0)

            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val request = YoutubeDLRequest(url)
            request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
            request.addOption("--no-part")

            youtubeDl?.download(
                request = request,
                progressCallBack = { percentage, _, _ ->
                    _uiState.value = DownloadState.Downloading(percentage.toInt())
                },
                onStartProcess = { },
                onEndProcess = { response ->
                    // Download complete, now extract frames
                    handleDownloadComplete(context, response)
                },
                onError = { error ->
                    _uiState.value = DownloadState.Error("Download failed: $error")
                }
            )
        }
    }

    private fun handleDownloadComplete(context: Context, response: YoutubeDLResponse) {
        viewModelScope.launch {
            try {
                _uiState.value = DownloadState.ExtractingFrames

                // Try multiple methods to find the video file
                val videoFile = findDownloadedVideo(response.out)

                if (videoFile == null || !videoFile.exists()) {
                    _uiState.value = DownloadState.Error(
                        "Video file not found. Output: ${response.out}"
                    )
                    return@launch
                }

                downloadedVideoPath = videoFile.absolutePath

                // Extract frames
                val frames = extractFrames(context, Uri.fromFile(videoFile))
                _extractedFrames.value = frames

                // Delete the video file
                val deleted = videoFile.delete()

                _uiState.value = DownloadState.Complete(
                    framesCount = frames.size,
                    videoDeleted = deleted,
                    downloadTime = response.elapsedTime
                )

            } catch (e: Exception) {
                _uiState.value = DownloadState.Error("Error processing video: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun findDownloadedVideo(output: String): File? {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // Method 1: Parse from output using various patterns
        val patterns = listOf(
            """\[download\]\s+Destination:\s+(.+)""".toRegex(),
            """\[download\]\s+(.+?)\s+has already been downloaded""".toRegex(),
            """\[Merger\]\s+Merging formats into\s+"(.+?)"""".toRegex(),
            """\[download\]\s+(.+?\.(?:mp4|webm|mkv))""".toRegex(),
            """Destination:\s+(.+?\.(?:mp4|webm|mkv))""".toRegex()
        )

        for (pattern in patterns) {
            pattern.find(output)?.let { match ->
                val path = match.groupValues[1].trim().replace("\"", "")
                val file = File(path)
                if (file.exists()) {
                    return file
                }
            }
        }

        // Method 2: Look for most recent video file in Downloads directory
        val videoFiles = downloadDir.listFiles { file ->
            file.isFile && file.name.matches(""".*\.(mp4|webm|mkv)$""".toRegex(RegexOption.IGNORE_CASE))
        }?.sortedByDescending { it.lastModified() }

        // Return the most recently modified video file
        return videoFiles?.firstOrNull()
    }

    private suspend fun extractFrames(context: Context, videoUri: Uri): List<Bitmap> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val frames = mutableListOf<Bitmap>()

            try {
                retriever.setDataSource(context, videoUri)

                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L

                if (durationMs == 0L) {
                    return@withContext emptyList()
                }

                val intervalMs = 5000L
                var currentTimeMs = 0L

                while (currentTimeMs <= durationMs) {
                    try {
                        val currentTimeMicros = currentTimeMs * 1000
                        val frame = retriever.getFrameAtTime(
                            currentTimeMicros,
                            MediaMetadataRetriever.OPTION_CLOSEST
                        )
                        frame?.let { frames.add(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    currentTimeMs += intervalMs
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            frames
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        _uiState.value = DownloadState.Idle
    }

    fun resetState() {
        _uiState.value = DownloadState.Idle
        _videoInfo.value = null
        _extractedFrames.value = emptyList()
        downloadedVideoPath = null
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Initializing : DownloadState()
    object FetchingInfo : DownloadState()
    data class InfoReady(val videoInfo: VideoInfo) : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object ExtractingFrames : DownloadState()
    data class Complete(
        val framesCount: Int,
        val videoDeleted: Boolean,
        val downloadTime: Long
    ) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

// UI Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeFrameExtractorScreen() {
    val viewModel: YoutubeFrameExtractorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val videoInfo by viewModel.videoInfo.collectAsState()
    val frames by viewModel.extractedFrames.collectAsState()
    val context = LocalContext.current

    var youtubeUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.initializeYoutubeDL(context)
    }

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
            Text(
                text = "Download & Extract Frames",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it },
                label = { Text("YouTube URL") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (youtubeUrl.isNotBlank()) {
                            viewModel.getVideoInfo(youtubeUrl)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = youtubeUrl.isNotBlank() && uiState !is DownloadState.Downloading
                ) {
                    Text("Get Info")
                }

                Button(
                    onClick = {
                        viewModel.resetState()
                        youtubeUrl = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset")
                }
            }

            videoInfo?.let { info ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Video Information", style = MaterialTheme.typography.titleMedium)
                        Text("Title: ${info.title}")
                        Text("Duration: ${info.duration} seconds")

                        Button(
                            onClick = {
                                viewModel.downloadAndExtractFrames(context, youtubeUrl)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState !is DownloadState.Downloading
                        ) {
                            Text("Download & Extract Frames")
                        }
                    }
                }
            }

            when (val currentState = uiState) {
                is DownloadState.Initializing -> {
                    StatusCard("Initializing YouTubeDL...", isLoading = true)
                }

                is DownloadState.FetchingInfo -> {
                    StatusCard("Fetching video information...", isLoading = true)
                }

                is DownloadState.Downloading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(progress = { currentState.progress / 100f })
                            Text("Downloading: ${currentState.progress}%")
                            Button(
                                onClick = { viewModel.cancelDownload() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                is DownloadState.ExtractingFrames -> {
                    StatusCard("Extracting frames from video...", isLoading = true)
                }

                is DownloadState.Complete -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Process Complete!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Frames extracted: ${currentState.framesCount}")
                            Text("Video deleted: ${if (currentState.videoDeleted) "Yes" else "No"}")
                            Text("Time taken: ${currentState.downloadTime} ms")
                        }
                    }
                }

                is DownloadState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                currentState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                else -> {}
            }

            if (frames.isNotEmpty()) {
                Text(
                    "Extracted ${frames.size} frames (5 sec intervals)",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(600.dp)
                ) {
                    items(frames.size) { index ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = frames[index].asImageBitmap(),
                                    contentDescription = "Frame at ${index * 5}s",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "${index * 5}s",
                                        modifier = Modifier.padding(4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, isLoading: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}