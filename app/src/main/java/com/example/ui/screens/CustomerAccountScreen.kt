package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.CustomerHistoryEntity
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedError
import com.example.ui.viewmodel.RiderUiState
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAccountScreen(
    customerName: String,
    phone: String,
    state: RiderUiState,
    onBack: () -> Unit,
    onUpdateBakaya: (Double) -> Unit,
    onDeleteBakaya: () -> Unit,
    onDeleteHistoryItem: (CustomerHistoryEntity) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    val debtor = state.debtors.find {
        it.name.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
    }
    val currentBakaya = debtor?.remainingDebt ?: 0.0

    val historyItems = state.customerHistory.filter {
        it.customerName.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
    }

    var showEditBakayaDialog by remember { mutableStateOf(false) }
    var newBakayaStr by remember { mutableStateOf(currentBakaya.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customerName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val file = PdfReportGenerator.generateCustomerLedgerPdf(
                            context = context,
                            customerName = customerName,
                            phone = phone,
                            remainingDebt = currentBakaya,
                            history = historyItems
                        )
                        ShareUtil.sharePdf(context, file)
                    }) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = RedError)
                    }
                    IconButton(onClick = {
                        val msg = "Assalam-o-Alaikum $customerName,\nYour current account statement with Aqeel Rider:\nOutstanding Balance (Bakaya): Rs. ${currentBakaya.toInt()}\nTotal activities logged: ${historyItems.size}\nThank you!"
                        ShareUtil.shareViaWhatsApp(context, phone, msg)
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", tint = GreenPrimary)
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
                .testTag("customer_account_screen")
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Outstanding Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Customer Phone: ${phone.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current Outstanding Bakaya: Rs. ${currentBakaya.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBakaya > 0) RedError else GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                newBakayaStr = currentBakaya.toInt().toString()
                                showEditBakayaDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Bakaya")
                        }

                        if (currentBakaya > 0) {
                            OutlinedButton(
                                onClick = onDeleteBakaya,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear to Rs. 0")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Transaction & Activity History (${historyItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history records for this customer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.activityType,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onDeleteHistoryItem(item) },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedError, modifier = Modifier.height(16.dp))
                                    }
                                }
                                Text(text = item.details, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditBakayaDialog) {
        AlertDialog(
            onDismissRequest = { showEditBakayaDialog = false },
            title = { Text("Update Customer Bakaya") },
            text = {
                OutlinedTextField(
                    value = newBakayaStr,
                    onValueChange = { newBakayaStr = it },
                    label = { Text("New Bakaya Amount (Rs.)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amount = newBakayaStr.toDoubleOrNull() ?: 0.0
                    onUpdateBakaya(amount)
                    showEditBakayaDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditBakayaDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
