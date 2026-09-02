package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GreenPrimary
import com.example.util.LicenseManager
import kotlinx.coroutines.launch

@Composable
fun LicenseActivationScreen(
    licenseManager: LicenseManager,
    onActivated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var licenseKey by remember { mutableStateOf("AR-2026-LIVE") }
    var customerName by remember { mutableStateOf("Aqeel Rider") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val deviceId = remember { licenseManager.getDeviceId() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("license_activation_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.aqeel_rider_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aqeel Rider Activation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter your Lifetime License Key to activate full app features on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = licenseKey,
                    onValueChange = {
                        licenseKey = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("License Key (e.g. AR-XXXX-XXXX)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_key_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GreenPrimary)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Rider / Business Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Device ID: $deviceId (1 Device Binding)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = licenseManager.activateLicense(
                                licenseKey = licenseKey,
                                customerName = customerName
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                onActivated()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Activation failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_license_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = !isLoading && licenseKey.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activate Lifetime License", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
