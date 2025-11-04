package com.arjundubey.app

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

// Add ClipboardManager at the top level
object ClipboardManager {
    var cutItem: FileSystemItem? = null
    var originalPath: String? = null

    fun cut(item: FileSystemItem) {
        cutItem = item
        originalPath = item.path
    }

    fun clear() {
        cutItem = null
        originalPath = null
    }

    fun hasItem(): Boolean = cutItem != null
}

@Composable
fun YouTubeDownloaderScreen(
    viewModel: DownloadViewModel = viewModel(),
    initialUrl: String? = null
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    if (viewModel.showFolderSelection) {
        FolderSelectionDialog(
            viewModel = viewModel,
            onConfirm = { viewModel.confirmDownload(context) },
            onDismiss = {
                viewModel.showFolderSelection = false
                viewModel.newFolderName = ""
                viewModel.folderSelectionHistory = emptyList()
            }
        )
    }

    // Set initial URL when shared
    LaunchedEffect(initialUrl) {
        if (initialUrl != null && initialUrl.isNotEmpty()) {
            viewModel.url = initialUrl
            Log.d("YouTubeDownloader", "Received shared URL: $initialUrl")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Update Button
            Button(
                onClick = { viewModel.updateYoutubeDL(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.isInitialized && !viewModel.isUpdating
            ) {
                Text(if (viewModel.isUpdating) "Updating..." else "Update")
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isDownloading,
                singleLine = true,
                placeholder = { Text("Paste or share YouTube link") }
            )

            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.downloadType = "video" },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.downloadType == "video") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Video")
                }

                Button(
                    onClick = { viewModel.downloadType = "audio" },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.downloadType == "audio") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Audio")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frame Extraction Settings
            if (viewModel.downloadType == "video") {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Extract Frames")
                        Switch(
                            checked = viewModel.extractFrames,
                            onCheckedChange = { viewModel.extractFrames = it },
                            enabled = !viewModel.isDownloading
                        )
                    }

                    if (viewModel.extractFrames) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Interval: ${viewModel.frameInterval}s")
                        Slider(
                            value = viewModel.frameInterval.toFloat(),
                            onValueChange = { viewModel.frameInterval = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 28,
                            enabled = !viewModel.isDownloading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-delete Video")
                            Checkbox(
                                checked = viewModel.autoDeleteVideo,
                                onCheckedChange = { viewModel.autoDeleteVideo = it },
                                enabled = !viewModel.isDownloading
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Download Button
            Button(
                onClick = { viewModel.startDownload(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.isInitialized && !viewModel.isDownloading && !viewModel.isUpdating && viewModel.url.isNotBlank()
            ) {
                Text(if (viewModel.isDownloading) "Downloading..." else "Download")
            }

            if (viewModel.downloadedVideoPath != null && !viewModel.isExtractingFrames && viewModel.downloadType == "video") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.extractFramesManually() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isDownloading && !viewModel.isExtractingFrames
                ) {
                    Text("Extract Frames Again")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress
            if (viewModel.isDownloading || viewModel.downloadProgress > 0f) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = viewModel.downloadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(viewModel.downloadStatus)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Frame Extraction Status
            if (viewModel.downloadType == "video" && (viewModel.isExtractingFrames || viewModel.extractionStatus.isNotEmpty())) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isExtractingFrames) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(viewModel.extractionStatus)

                    if (viewModel.extractedFramesCount > 0) {
                        Text("Frames extracted: ${viewModel.extractedFramesCount}")
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Global Paste Button (NEW)
        if (ClipboardManager.hasItem()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cut: ${ClipboardManager.cutItem?.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    Button(
                        onClick = {
                            ClipboardManager.cutItem?.let { cutItem ->
                                viewModel.pasteItem(cutItem, viewModel.currentPath ?: "")
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("📋 Paste Here")
                    }

                    Button(
                        onClick = { ClipboardManager.clear() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Downloaded Files Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.currentPath != null) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Text("⬅️")
                    }
                }
                Column {
                    Text(
                        text = "Files & Folders (${viewModel.fileSystemItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (viewModel.currentPath != null) {
                        Text(
                            text = File(viewModel.currentPath!!).name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = { viewModel.refreshFileList() }) {
                    Text("🔄")
                }
                IconButton(onClick = { viewModel.showFileList = !viewModel.showFileList }) {
                    Text(if (viewModel.showFileList) "▼" else "▶")
                }
            }
        }

        if (viewModel.showFileList) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (viewModel.fileSystemItems.isEmpty()) {
                    Text(
                        text = "No files or folders found",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Replace your LazyColumn section with this optimized version

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = viewModel.fileSystemItems,
                            key = { it.path } // CRITICAL: Stable keys prevent full recomposition
                        ) { item ->
                            val isCut = ClipboardManager.cutItem?.path == item.path
                            FileSystemItemView(
                                item = item,
                                isCut = ClipboardManager.cutItem?.path == item.path, // Pass as parameter
                                onDelete = {
                                    viewModel.deleteItem(item.path)
                                },
                                onOpenFolder = {
                                    viewModel.openFolder(item.path)
                                },
                                onOpenImage = {
                                    viewModel.openImage(item.path)
                                },
                                onOpenAudio = {
                                    viewModel.openAudio(item.path)
                                },
                                onCut = { cutItem ->
                                    viewModel.cutItem(cutItem)
                                },
                                onPaste = { pasteItem, destinationPath ->
                                    viewModel.pasteItem(pasteItem, destinationPath)
                                },
                                currentPath = viewModel.currentPath ?: ""
                            )
                        }

                    }
                }
            }
        }

        // Image Viewer Dialog
        if (viewModel.selectedImagePath != null) {
            FullScreenImageViewer(
                imagePath = viewModel.selectedImagePath!!,
                currentIndex = viewModel.currentImageIndex,
                totalImages = viewModel.currentImageList.size,
                onClose = { viewModel.closeImage() },
                onNext = { viewModel.nextImage() },
                onPrevious = { viewModel.previousImage() },
                hasNext = viewModel.currentImageIndex < viewModel.currentImageList.size - 1,
                hasPrevious = viewModel.currentImageIndex > 0,
                frameInfo = viewModel.selectedFrameInfo,
                frameInterval = viewModel.frameInterval
            )
        }

        // Audio Player Dialog
        if (viewModel.selectedAudioPath != null) {
            AudioPlayerDialog(
                audioPath = viewModel.selectedAudioPath!!,
                onClose = { viewModel.closeAudio() }
            )
        }
    }
}



@Composable
fun MediaPlayer() {
    TODO("Not yet implemented")
}