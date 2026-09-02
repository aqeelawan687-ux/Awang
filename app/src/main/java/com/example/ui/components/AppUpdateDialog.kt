package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.UpdateState

@Composable
fun AppUpdateDialog(
    updateState: UpdateState,
    onStartDownload: (String) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            val info = updateState.info
            AlertDialog(
                onDismissRequest = { if (!info.isMandatory) onDismiss() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(text = "App Update Available (v${info.versionName})", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "A new version of Aqeel Rider is available with improvements and fixes.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "What's New:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { onStartDownload(info.downloadUrl) }) {
                        Text("Download & Install")
                    }
                },
                dismissButton = {
                    if (!info.isMandatory) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                }
            )
        }
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Downloading Update...", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { (updateState.progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${updateState.progress}% completed", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {}
            )
        }
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = "Update Ready to Install", fontWeight = FontWeight.Bold) },
                text = {
                    Text(text = "The update has downloaded successfully. Tap install to finish.")
                },
                confirmButton = {
                    Button(onClick = { onInstall(updateState.file) }) {
                        Text("Install Now")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = "Update Failed", fontWeight = FontWeight.Bold) },
                text = {
                    Text(text = updateState.message)
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }
}
