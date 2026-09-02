package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedError
import com.example.util.LicenseData
import com.example.util.LicenseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    licenseState: LicenseState,
    onCheckUpdate: () -> Unit,
    onResetLicense: () -> Unit,
    onSimulateBlock: () -> Unit,
    onSimulateUnblock: () -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & License", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // License Information Card
            Text(text = "License Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Aqeel Rider License", fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GreenPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "LIFETIME ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (licenseState is LicenseState.Active) {
                        val lic = licenseState.license
                        Text(text = "License Key: ${lic.code}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Device ID: ${lic.deviceId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (lic.customerName.isNotEmpty()) {
                            Text(text = "Licensed To: ${lic.customerName}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Updates
            Text(text = "Software Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Aqeel Rider Dispatch v1.3.3", fontWeight = FontWeight.Bold)
                            Text(text = "Build: Release v1.3.3 (Lifetime Edition)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckUpdate,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selector
            Text(text = "Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(AppThemeMode.SYSTEM) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SettingsSystemDaydream, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "System Default", modifier = Modifier.weight(1f))
                        if (currentTheme == AppThemeMode.SYSTEM) {
                            Text(text = "✓", color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(AppThemeMode.LIGHT) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.LightMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Light Mode", modifier = Modifier.weight(1f))
                        if (currentTheme == AppThemeMode.LIGHT) {
                            Text(text = "✓", color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(AppThemeMode.DARK) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Dark Mode", modifier = Modifier.weight(1f))
                        if (currentTheme == AppThemeMode.DARK) {
                            Text(text = "✓", color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin & Testing Controls
            Text(text = "License & Testing Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onSimulateBlock,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = RedError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Block", color = RedError)
                        }
                        OutlinedButton(
                            onClick = onSimulateUnblock,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Unblock", color = GreenPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onResetLicense,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset License Activation")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danger Zone
            Text(text = "Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RedError)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = RedError),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All App Records")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Records?") },
            text = { Text("This will permanently clear all rides, parcels, debtors, and ledger entries.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
