package com.arjundubey.app

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

// ViewModel to retain MediaPlayer across configuration changes
class AudioPlayerViewModel : ViewModel() {
    var mediaPlayer: MediaPlayer? = null
        private set

    var isInitialized by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)

    var currentAudioPath: String? = null
        private set

    var onCompletionCallback: (() -> Unit)? = null

    fun initializePlayer(
        audioPath: String,
        onReady: (Int) -> Unit,
        onError: (String) -> Unit,
        onCompletion: () -> Unit
    ) {
        // Only initialize if this is a new audio file or first time
        if (currentAudioPath == audioPath && mediaPlayer != null) {
            // Already initialized for this file
            mediaPlayer?.let {
                isInitialized = true
                onReady(it.duration)
            }
            return
        }

        onCompletionCallback = onCompletion
        currentAudioPath = audioPath

        // Release old player if exists
        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepareAsync()
                setOnPreparedListener { mp ->
                    isInitialized = true
                    onReady(mp.duration)
                }
                setOnErrorListener { _, what, extra ->
                    onError("MediaPlayer error: $what, $extra")
                    true
                }
                setOnCompletionListener {
                    this@AudioPlayerViewModel.isPlaying = false
                    onCompletionCallback?.invoke()
                }
            }
        } catch (e: Exception) {
            onError("Failed to load audio: ${e.message}")
        }
    }

    fun play() {
        mediaPlayer?.start()
        isPlaying = true
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun restart() {
        mediaPlayer?.seekTo(0)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun AudioPlayerDialog(
    audioPath: String,
    onClose: () -> Unit,
    viewModel: AudioPlayerViewModel = viewModel()
) {
    var duration by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(!viewModel.isInitialized) }
    var error by remember { mutableStateOf<String?>(null) }

    // Separate state for slider - this is the key change
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Initialize player only once per audio file
    LaunchedEffect(audioPath) {
        if (viewModel.currentAudioPath != audioPath || viewModel.mediaPlayer == null) {
            viewModel.initializePlayer(
                audioPath = audioPath,
                onReady = { mediaDuration ->
                    duration = mediaDuration
                    isLoading = false
                },
                onError = { errorMsg ->
                    error = errorMsg
                    isLoading = false
                },
                onCompletion = {
                    sliderPosition = 0f
                }
            )
        } else {
            // Already initialized, just update UI state
            duration = viewModel.mediaPlayer?.duration ?: 0
            isLoading = false
            sliderPosition = viewModel.getCurrentPosition().toFloat()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Just pause if playing when dialog is closed
            if (viewModel.isPlaying) {
                viewModel.pause()
            }
        }
    }

    // Update slider position from MediaPlayer only when NOT dragging
    LaunchedEffect(viewModel.isPlaying) {
        while (isActive && viewModel.isPlaying) {
            if (!isDragging) {
                sliderPosition = viewModel.getCurrentPosition().toFloat()
            }
            delay(100)
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.pause()
            onClose()
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = File(audioPath).name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Text("Loading audio...")
                } else if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // Progress slider - completely independent state
                    Slider(
                        value = sliderPosition,
                        onValueChange = { newValue ->
                            isDragging = true
                            sliderPosition = newValue
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            viewModel.seekTo(sliderPosition.toInt())
                        },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Time labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(sliderPosition.toInt()))
                        Text(formatTime(duration))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.restart()
                                sliderPosition = 0f
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Restart"
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = {
                                if (viewModel.isPlaying) {
                                    viewModel.pause()
                                } else {
                                    viewModel.play()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (viewModel.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = {
                        viewModel.pause()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// Helper function to format milliseconds to MM:SS
private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}