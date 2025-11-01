package com.arjundubey.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    frameInfo: Pair<Int, String>? = null,  // NEW: (frameNumber, videoUrl)
    frameInterval: Int = 5  // NEW: interval used for extraction
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

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

    Dialog(
        onDismissRequest = onClose,
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
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)

                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2

                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
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
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // NEW: Web link button (top-left)
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

            // Navigation buttons (unchanged)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = hasPrevious,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (hasPrevious) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    if (hasPrevious) {
                        Text(
                            "◀️",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    enabled = hasNext,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (hasNext) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    if (hasNext) {
                        Text(
                            "▶️",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom info bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
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
            }
        }
    }
}