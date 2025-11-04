package com.arjundubey.app

import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun FolderSelectionDialog(
    viewModel: DownloadViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Download Folder")
                // Show current path
                viewModel.currentFolderSelectionPath?.let { path ->
                    Text(
                        text = File(path).name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column {
                // Navigation header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home button (go to root)
                    IconButton(
                        onClick = { viewModel.goToRootInFolderSelection() }
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Go to root")
                    }

                    // Back button
                    IconButton(
                        onClick = { viewModel.goBackInFolderSelection() },
                        enabled = viewModel.folderSelectionHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    // Current path info
                    Text(
                        text = viewModel.currentFolderSelectionPath?.let { path ->
                            File(path).absolutePath.replace(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                                "Downloads"
                            )
                        } ?: "Select folder",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // New folder creation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.newFolderName,
                        onValueChange = { viewModel.newFolderName = it },
                        label = { Text("New folder name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.createNewFolder() },
                        enabled = viewModel.newFolderName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick action: Select current folder button
                Button(
                    onClick = {
                        viewModel.currentFolderSelectionPath?.let { path ->
                            viewModel.selectedFolder = FileSystemItem(
                                name = File(path).name,
                                path = path,
                                size = 0L,
                                lastModified = File(path).lastModified(),
                                type = "Folder",
                                isDirectory = true,
                                itemCount = File(path).listFiles()?.size ?: 0
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✓ Select This Folder")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Folder list
                Text("Folders:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.availableFolders.isEmpty()) {
                    Text(
                        "No subfolders found. You can create one or select this folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(viewModel.availableFolders) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.selectedFolder?.path == folder.path)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onClick = {
                                    // Navigate into this folder to see its subfolders
                                    viewModel.navigateToFolderInSelection(folder.path)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            folder.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${folder.itemCount} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Selection and navigation indicators
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Select button (non-propagating click)
                                        IconButton(
                                            onClick = {
                                                viewModel.selectedFolder = folder
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (viewModel.selectedFolder?.path == folder.path)
                                                    Icons.Default.Check
                                                else
                                                    Icons.Default.CheckCircle,
                                                contentDescription = "Select this folder",
                                                tint = if (viewModel.selectedFolder?.path == folder.path)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Navigate arrow
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Open folder",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Current selection info
                viewModel.selectedFolder?.let { selected ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected")
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Selected folder:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    selected.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    selected.path.replace(
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                                        "Downloads"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = viewModel.selectedFolder != null
            ) {
                Text("Download Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}