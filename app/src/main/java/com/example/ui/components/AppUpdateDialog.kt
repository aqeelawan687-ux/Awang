package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.util.ApkUpdateManager
import com.example.util.UpdateInfo
import com.example.util.UpdateState
import kotlinx.coroutines.launch

/**
 * Material 3 Auto-Update dialog for Aqeel Rider.
 */
@Composable
fun AppUpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            val info = updateState.info
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("app_update_dialog"),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "New Update Available!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "نئی اپڈیٹ دستیاب ہے",
                            fontSize = 13.sp,
                            color = EmeraldGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Version comparison card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Current / موجودہ",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "v${info.currentVersionName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = "➔",
                                    fontSize = 16.sp,
                                    color = EmeraldGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "New / نیا ورژن",
                                        fontSize = 11.sp,
                                        color = EmeraldGreenPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "v${info.latestVersionName}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = EmeraldGreenPrimary
                                    )
                                }
                            }
                        }

                        // Release Notes if present
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "What's New / تبدیلی اور فیچرز:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = info.releaseNotes,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                if (!ApkUpdateManager.canInstallUnknownApps(context)) {
                                    Toast.makeText(
                                        context,
                                        "Please allow 'Install Unknown Apps' for Aqeel Rider",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    ApkUpdateManager.openUnknownAppSourcesSettings(context)
                                }
                                val apk = ApkUpdateManager.downloadApk(context, info)
                                if (apk != null) {
                                    ApkUpdateManager.installApk(context, apk)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        modifier = Modifier.testTag("update_now_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Now / اپڈیٹ کریں")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            ApkUpdateManager.dismissForSession()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text("Later / بعد میں")
                    }
                }
            )
        }

        is UpdateState.Downloading -> {
            val downloading = updateState
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss during download unless cancelled */ },
                modifier = Modifier.testTag("downloading_update_dialog"),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloading",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Downloading Update...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloading.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress: ${downloading.progressPercent}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary
                            )
                            if (downloading.totalBytes > 0) {
                                val mbDownloaded = downloading.bytesDownloaded / (1024 * 1024f)
                                val mbTotal = downloading.totalBytes / (1024 * 1024f)
                                Text(
                                    text = String.format("%.1f MB / %.1f MB", mbDownloaded, mbTotal),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            ApkUpdateManager.resetState()
                            onDismiss()
                        }
                    ) {
                        Text("Cancel / منسوخ")
                    }
                }
            )
        }

        is UpdateState.Downloaded -> {
            val downloaded = updateState
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("downloaded_update_dialog"),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Download Complete!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "APK download ho chuki hai. Naya version install karne ke liye button par click karein.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!ApkUpdateManager.canInstallUnknownApps(context)) {
                                ApkUpdateManager.openUnknownAppSourcesSettings(context)
                            }
                            ApkUpdateManager.installApk(context, downloaded.apkFile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install Now / انسٹال کریں")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        ApkUpdateManager.resetState()
                        onDismiss()
                    }) {
                        Text("Later / بعد میں")
                    }
                }
            )
        }

        is UpdateState.Error -> {
            val error = updateState
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("error_update_dialog"),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AccentRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = AccentRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Update Failed / خرابی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AccentRed
                    )
                },
                text = {
                    Text(
                        text = error.message,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    if (error.canRetry && error.info != null) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val apk = ApkUpdateManager.downloadApk(context, error.info)
                                    if (apk != null) {
                                        ApkUpdateManager.installApk(context, apk)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry / دوبارہ کوشش")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        ApkUpdateManager.resetState()
                        onDismiss()
                    }) {
                        Text("Close / بند کریں")
                    }
                }
            )
        }

        else -> {
            // Idle, Checking, UpToDate - No pop-up dialog required
        }
    }
}
