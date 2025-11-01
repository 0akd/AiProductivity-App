package com.arjundubey.app


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import java.io.File





@Composable
fun YouTubeDownloaderScreen(
    viewModel: DownloadViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
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
                singleLine = true
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.fileSystemItems) { item ->
                            FileSystemItemView(
                                item = item,
                                onDelete = { viewModel.deleteItem(item.path) },
                                onOpenFolder = {
                                    if (item.isDirectory) {
                                        viewModel.openFolder(item.path)
                                    }
                                },
                                onOpenImage = {
                                    if (!item.isDirectory && item.type == "Image") {
                                        viewModel.openImage(item.path)
                                    }
                                },
                                onOpenAudio = {
                                    if (!item.isDirectory && item.type == "Audio") {
                                        viewModel.openAudio(item.path)
                                    }
                                }
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
                frameInfo = viewModel.selectedFrameInfo,  // NEW: Pass frame info
                frameInterval = viewModel.frameInterval    // NEW: Pass interval
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

