package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RedError
import com.example.util.LicenseManager
import com.example.util.ShareUtil
import kotlinx.coroutines.launch

@Composable
fun LicenseBlockedScreen(
    licenseManager: LicenseManager,
    onUnblocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceId = licenseManager.getDeviceId()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("license_blocked_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = "Blocked",
            tint = RedError,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "License Suspended / Blocked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RedError,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your access to Aqeel Rider has been temporarily blocked by administrator.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Data Safe Notice",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your records, ledger history, rides and debts remain 100% intact in local storage and will be instantly available once unblocked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device ID: $deviceId",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val msg = "Hello Admin, my Aqeel Rider app is showing Blocked.\nDevice ID: $deviceId\nPlease verify and unblock."
                ShareUtil.shareViaWhatsApp(context, "+923000000000", msg)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedError)
        ) {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Contact Administrator")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    val state = licenseManager.verifyLicense()
                    if (state !is com.example.util.LicenseState.Blocked) {
                        onUnblocked()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Check Status Again")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Demo testing unblock button
        Button(
            onClick = {
                licenseManager.unblockLocally()
                onUnblocked()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Demo: Unlock Locally")
        }
    }
}
