package com.arjundubey.app

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class Song(
    val mp3Link: String,
    val coverName: String
)

// ViewModel to survive configuration changes
class MusicPlayerViewModel : ViewModel() {
    var songs = mutableStateListOf<Song>()
    var isLoading = mutableStateOf(true)
    var errorMessage = mutableStateOf<String?>(null)
    var mediaPlayer: MediaPlayer? = null
    var currentPlayingIndex = mutableStateOf<Int?>(null)
    var isPlaying = mutableStateOf(false)
    var currentPosition = mutableStateOf(0)
    var duration = mutableStateOf(0)

    private var positionUpdateJob: Job? = null

    fun startPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive && isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        currentPosition.value = it.currentPosition
                        duration.value = it.duration
                    }
                }
                delay(100)
            }
        }
    }

    fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        stopPositionUpdate()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MP3PlayerScreen(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val songsFileUrl = "https://raw.githubusercontent.com/0akd/audio/main/1.txt"
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE) }

    // Fetch songs only once
    LaunchedEffect(Unit) {
        if (viewModel.songs.isEmpty() && viewModel.isLoading.value) {
            scope.launch {
                try {
                    val fetchedSongs = fetchSongsFromUrl(songsFileUrl)
                    viewModel.songs.addAll(fetchedSongs)
                    viewModel.isLoading.value = false
                } catch (e: Exception) {
                    viewModel.errorMessage.value = "Failed to load songs: ${e.message}"
                    viewModel.isLoading.value = false
                }
            }
        }
    }

    // Save progress periodically
    LaunchedEffect(viewModel.currentPosition.value, viewModel.currentPlayingIndex.value) {
        viewModel.currentPlayingIndex.value?.let { index ->
            prefs.edit().apply {
                putInt("last_song_index", index)
                putInt("last_position", viewModel.currentPosition.value)
                apply()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MP3 Player",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (viewModel.currentPlayingIndex.value != null) {
                BottomMusicControls(
                    viewModel = viewModel,
                    context = context,
                    prefs = prefs
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                viewModel.isLoading.value -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                viewModel.errorMessage.value != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.errorMessage.value ?: "",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.isLoading.value = true
                            viewModel.errorMessage.value = null
                            scope.launch {
                                try {
                                    val fetchedSongs = fetchSongsFromUrl(songsFileUrl)
                                    viewModel.songs.clear()
                                    viewModel.songs.addAll(fetchedSongs)
                                    viewModel.isLoading.value = false
                                } catch (e: Exception) {
                                    viewModel.errorMessage.value = "Failed to load songs: ${e.message}"
                                    viewModel.isLoading.value = false
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                viewModel.songs.isEmpty() -> {
                    Text(
                        text = "No songs available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = if (viewModel.currentPlayingIndex.value != null) 80.dp else 0.dp)
                    ) {
                        itemsIndexed(viewModel.songs) { index, song ->
                            SongItem(
                                song = song,
                                isPlaying = viewModel.currentPlayingIndex.value == index && viewModel.isPlaying.value,
                                onClick = {
                                    scope.launch {
                                        playOrPauseSong(
                                            context = context,
                                            viewModel = viewModel,
                                            index = index,
                                            song = song,
                                            prefs = prefs
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomMusicControls(
    viewModel: MusicPlayerViewModel,
    context: Context,
    prefs: android.content.SharedPreferences
) {
    val scope = rememberCoroutineScope()
    val currentSong = viewModel.currentPlayingIndex.value?.let {
        viewModel.songs.getOrNull(it)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Song title
            currentSong?.let {
                Text(
                    text = it.coverName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Progress slider
            Slider(
                value = viewModel.currentPosition.value.toFloat(),
                onValueChange = { newPosition ->
                    viewModel.mediaPlayer?.seekTo(newPosition.toInt())
                    viewModel.currentPosition.value = newPosition.toInt()
                },
                valueRange = 0f..viewModel.duration.value.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTim(viewModel.currentPosition.value),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTim(viewModel.duration.value),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = {
                        viewModel.currentPlayingIndex.value?.let { currentIndex ->
                            val prevIndex = (currentIndex - 1).coerceAtLeast(0)
                            val prevSong = viewModel.songs.getOrNull(prevIndex)
                            if (prevSong != null) {
                                scope.launch {
                                    playOrPauseSong(context, viewModel, prevIndex, prevSong, prefs)
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play/Pause button
                IconButton(
                    onClick = {
                        if (viewModel.isPlaying.value) {
                            viewModel.mediaPlayer?.pause()
                            viewModel.isPlaying.value = false
                            viewModel.stopPositionUpdate()
                        } else {
                            viewModel.mediaPlayer?.start()
                            viewModel.isPlaying.value = true
                            viewModel.startPositionUpdate()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (viewModel.isPlaying.value) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Next button
                IconButton(
                    onClick = {
                        viewModel.currentPlayingIndex.value?.let { currentIndex ->
                            val nextIndex = (currentIndex + 1).coerceAtMost(viewModel.songs.size - 1)
                            val nextSong = viewModel.songs.getOrNull(nextIndex)
                            if (nextSong != null) {
                                scope.launch {
                                    playOrPauseSong(context, viewModel, nextIndex, nextSong, prefs)
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

suspend fun playOrPauseSong(
    context: Context,
    viewModel: MusicPlayerViewModel,
    index: Int,
    song: Song,
    prefs: android.content.SharedPreferences
) = withContext(Dispatchers.Main) {
    if (viewModel.currentPlayingIndex.value == index) {
        // Toggle play/pause for current song
        if (viewModel.isPlaying.value) {
            viewModel.mediaPlayer?.pause()
            viewModel.isPlaying.value = false
            viewModel.stopPositionUpdate()
        } else {
            viewModel.mediaPlayer?.start()
            viewModel.isPlaying.value = true
            viewModel.startPositionUpdate()
        }
    } else {
        // Stop current song and play new one
        viewModel.stopPositionUpdate()
        viewModel.mediaPlayer?.release()

        // Get cached file or download
        val audioFile = withContext(Dispatchers.IO) {
            getCachedOrDownloadSong(context, song)
        }

        // Load saved position if returning to a song
        val savedPosition = if (prefs.getInt("last_song_index", -1) == index) {
            prefs.getInt("last_position", 0)
        } else {
            0
        }

        viewModel.mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFile.absolutePath)
            prepareAsync()
            setOnPreparedListener {
                if (savedPosition > 0 && savedPosition < duration) {
                    seekTo(savedPosition)
                }
                start()
                viewModel.isPlaying.value = true
                viewModel.duration.value = duration
                viewModel.startPositionUpdate()
            }
            setOnCompletionListener {
                viewModel.isPlaying.value = false
                viewModel.stopPositionUpdate()
                viewModel.currentPosition.value = 0
            }
        }
        viewModel.currentPlayingIndex.value = index
    }
}

suspend fun getCachedOrDownloadSong(context: Context, song: Song): File = withContext(Dispatchers.IO) {
    val cacheDir = File(context.cacheDir, "music_cache")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }

    val fileName = song.mp3Link.hashCode().toString() + ".mp3"
    val cachedFile = File(cacheDir, fileName)

    if (cachedFile.exists()) {
        return@withContext cachedFile
    }

    // Download and cache
    URL(song.mp3Link).openStream().use { input ->
        FileOutputStream(cachedFile).use { output ->
            input.copyTo(output)
        }
    }

    cachedFile
}

suspend fun fetchSongsFromUrl(url: String): List<Song> = withContext(Dispatchers.IO) {
    val content = URL(url).readText()
    content.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                Song(
                    mp3Link = parts[0].trim(),
                    coverName = parts[1].trim()
                )
            } else {
                null
            }
        }
}

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = song.coverName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

fun formatTim(milliseconds: Int): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}