package com.arjundubey.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FullScreenImageViewer(
    imagePath: String,
    currentIndex: Int,
    totalImages: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    hasNext: Boolean,
    hasPrevious: Boolean,
    frameInfo: Pair<Int, String>? = null,
    frameInterval: Int = 5
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Navigation state variables
    var isLongPressingNext by remember { mutableStateOf(false) }
    var isLongPressingPrevious by remember { mutableStateOf(false) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }

    // Calculate timestamp from frame number
    val timestampSeconds = frameInfo?.let { (frameNum, _) ->
        (frameNum - 1) * frameInterval
    }

    // Reset zoom and pan when image changes
    LaunchedEffect(imagePath) {
        scale = 1f
        offset = Offset.Zero

        // Load bitmap in background
        withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                }
                bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            } catch (e: Exception) {
                bitmap = null
            }
        }
    }

    // Handle auto-navigation when long pressing
    LaunchedEffect(isLongPressingNext, isLongPressingPrevious, hasNext, hasPrevious) {
        // Cancel any existing navigation
        navigationJob?.cancel()

        if (isLongPressingNext && hasNext) {
            navigationJob = startExponentialNavigation(
                initialDelay = 500L, // Initial delay before starting
                onNavigate = onNext,
                isActive = { isLongPressingNext && hasNext }

            )
        } else if (isLongPressingPrevious && hasPrevious) {
            navigationJob = startExponentialNavigation(
                initialDelay = 500L,
                onNavigate = onPrevious,

                isActive = { isLongPressingPrevious && hasPrevious }
            )
        }
    }

    // Clean up when composable is disposed
    LaunchedEffect(Unit) {
        try {
            awaitCancellation()
        } finally {
            navigationJob?.cancel()
        }
    }

    Dialog(
        onDismissRequest = {
            // Only close if not zoomed/panned
            if (scale == 1f && offset == Offset.Zero) {
                onClose()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image with zoom and pan
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Full screen image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                val newScale = (scale * zoom).coerceIn(0.5f, 5f) // Increased minimum scale to 0.5f

                                if (newScale >= 0.5f) {
                                    scale = newScale

                                    // Calculate max offset based on current scale and image size
                                    val maxOffsetX = (size.width * (scale - 1)) / 2
                                    val maxOffsetY = (size.height * (scale - 1)) / 2

                                    // Handle offset constraints properly for both zoom in and zoom out
                                    offset = if (scale > 1f) {
                                        // When zoomed in, allow panning within bounds
                                        Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    } else {
                                        // When zoomed out, reset to center or handle minimal offset
                                        Offset.Zero
                                    }
                                }
                            }
                        }
                )
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            // Top bar with web link, close button and info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Web link button (top-left)
                    if (frameInfo != null && timestampSeconds != null) {
                        IconButton(
                            onClick = {
                                val (_, videoUrl) = frameInfo
                                val urlWithTimestamp = if (videoUrl.contains("&t=") || videoUrl.contains("?t=")) {
                                    // Replace existing timestamp
                                    videoUrl.replaceFirst(Regex("[?&]t=\\d+"), "?t=$timestampSeconds")
                                } else if (videoUrl.contains("?")) {
                                    "$videoUrl&t=$timestampSeconds"
                                } else {
                                    "$videoUrl?t=$timestampSeconds"
                                }

                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(urlWithTimestamp)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Text("🌐", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    Text(
                        text = "${currentIndex + 1} / $totalImages",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Show timestamp if available
                    if (timestampSeconds != null) {
                        Text(
                            text = formatTimestamp(timestampSeconds),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset zoom button
                    if (scale != 1f || offset != Offset.Zero) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Text("🔄", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    IconButton(onClick = onClose) {
                        Text("✖️", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            // Navigation buttons with long-press support
            // Replace the navigation buttons section with this simpler version:
            // Navigation buttons with proper long-press release detection
            // Navigation buttons with proper long-press release detection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous button - FIXED VERSION
                if (hasPrevious) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .pointerInput(hasPrevious) {
                                detectTapGestures(
                                    onPress = { pressOffset ->
                                        // This will wait for release OR long press
                                        val pressResult = tryAwaitRelease()
                                        // If we reach here, press was released
                                        isLongPressingPrevious = false
                                    },
                                    onLongPress = {
                                        // Start long press navigation
                                        isLongPressingPrevious = true
                                    },
                                    onTap = {
                                        // Single tap - navigate once
                                        onPrevious()
                                    }
                                )
                            }
                    ) {
                        Text(
                            "◀️",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (isLongPressingPrevious) Color.Yellow else Color.White
                        )
                    }
                } else {
                    // Placeholder for spacing when no previous
                    Box(modifier = Modifier.size(64.dp))
                }

                // Next button - FIXED VERSION
                if (hasNext) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .pointerInput(hasNext) {
                                detectTapGestures(
                                    onPress = { pressOffset ->
                                        // This will wait for release OR long press
                                        val pressResult = tryAwaitRelease()
                                        // If we reach here, press was released
                                        isLongPressingNext = false
                                    },
                                    onLongPress = {
                                        // Start long press navigation
                                        isLongPressingNext = true
                                    },
                                    onTap = {
                                        // Single tap - navigate once
                                        onNext()
                                    }
                                )
                            }
                    ) {
                        Text(
                            "▶️",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (isLongPressingNext) Color.Yellow else Color.White
                        )
                    }
                } else {
                    // Placeholder for spacing when no next
                    Box(modifier = Modifier.size(64.dp))
                }
            }

            // Bottom info bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(12.dp)
            ) {
                Text(
                    text = File(imagePath).name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Zoom: ${String.format("%.1f", scale)}x",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                // Show auto-navigation indicator
                if (isLongPressingNext || isLongPressingPrevious) {
                    Text(
                        text = "Auto-navigation: ${if (isLongPressingNext) "Next" else "Previous"}",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Starts exponential navigation with increasing speed
 * @param initialDelay Initial delay before first navigation in milliseconds
 * @param onNavigate Callback to execute for each navigation step
 * @param isActive Lambda to check if navigation should continue
 */
private fun startExponentialNavigation(
    initialDelay: Long = 500L,
    onNavigate: () -> Unit,
    isActive: () -> Boolean
): Job {
    return kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
        var currentDelay = initialDelay
        var navigationCount = 0

        // Initial delay before starting
        delay(initialDelay)

        while (isActive()) { // Fixed: only call the function, don't reference the property
            onNavigate()
            navigationCount++

            // Exponential speed increase: reduce delay by 15% each step, with minimum of 50ms
            currentDelay = (currentDelay * 0.85f).toLong().coerceAtLeast(50L)

            // Also cap the maximum speed after 20 steps to prevent excessive speed
            if (navigationCount > 20) {
                currentDelay = currentDelay.coerceAtLeast(30L)
            }

            delay(currentDelay)
        }
    }
}

// Helper function for timestamp formatting
fun formatTimestamp(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}