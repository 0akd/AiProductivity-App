package com.arjundubey.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileSystemItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val type: String,
    val isDirectory: Boolean,
    val itemCount: Int = 0,
    val frameNumber: Int? = null,
    val videoUrl: String? = null
)

@Composable
fun FileSystemItemView(
    item: FileSystemItem,
    onDelete: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenAudio: () -> Unit,
    onCut: (FileSystemItem) -> Unit,
    onPaste: (FileSystemItem, String) -> Unit,
    currentPath: String,
    isCut: Boolean = false // Pass this as parameter instead of checking inside
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }

    // Pre-compute values outside of composition
    val emoji = remember(item.isDirectory, item.type) {
        if (item.isDirectory) "📁" else when (item.type) {
            "Video" -> "🎥"
            "Audio" -> "🎵"
            "Image" -> "🖼️"
            else -> "📄"
        }
    }

    val formattedSize = remember(item.size) { formatFileSize(item.size) }
    val formattedDate = remember(item.lastModified) { formatDate(item.lastModified) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = {
            when {
                item.isDirectory -> onOpenFolder()
                item.type == "Image" -> onOpenImage()
                item.type == "Audio" -> onOpenAudio()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (isCut) {
                        Text(
                            text = "✂️",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isDirectory) {
                        Text(
                            text = "Folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${item.itemCount} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onCut(item) }) {
                    Text("✂️")
                }

                IconButton(
                    onClick = { showPasteDialog = true },
                    enabled = ClipboardManager.hasItem() && item.isDirectory
                ) {
                    Text(
                        "📋",
                        color = if (ClipboardManager.hasItem() && item.isDirectory)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Text("🗑️")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${if (item.isDirectory) "Folder" else "File"}") },
            text = {
                Text(
                    if (item.isDirectory) {
                        "Are you sure you want to delete the folder '${item.name}' and all its contents?"
                    } else {
                        "Are you sure you want to delete '${item.name}'?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Item") },
            text = {
                Column {
                    Text("Move '${ClipboardManager.cutItem?.name}' to '${item.name}'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will move the item from its original location to this folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ClipboardManager.cutItem?.let { cutItem ->
                            onPaste(cutItem, item.path)
                        }
                        showPasteDialog = false
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PasteButton(
    modifier: Modifier = Modifier,
    currentPath: String,
    onPaste: (FileSystemItem, String) -> Unit
) {
    if (ClipboardManager.hasItem()) {
        TextButton(
            onClick = {
                ClipboardManager.cutItem?.let { cutItem ->
                    onPaste(cutItem, currentPath)
                }
            },
            modifier = modifier,
            enabled = ClipboardManager.hasItem()
        ) {
            Text("📋 Paste to Current Folder")
        }
    }
}

// Cache formatted values
private val sizeCache = mutableMapOf<Long, String>()
private val dateCache = mutableMapOf<Long, String>()

fun formatFileSize(size: Long): String {
    return sizeCache.getOrPut(size) {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$size B"
        }
    }
}

fun formatDate(timestamp: Long): String {
    return dateCache.getOrPut(timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    }
}