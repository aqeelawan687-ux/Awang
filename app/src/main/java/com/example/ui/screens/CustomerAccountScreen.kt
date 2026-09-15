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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Payments
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
import com.example.ui.components.AddEditParcelDialog
import com.example.ui.components.AddEditRideDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedError
import com.example.ui.viewmodel.RiderUiState
import com.example.util.DateTimeUtils
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAccountScreen(
    customerName: String,
    phone: String,
    state: RiderUiState,
    onBack: () -> Unit,
    onRecordPayment: (Double, String) -> Unit,
    onUpdateBakaya: (Double) -> Unit,
    onDeleteBakaya: () -> Unit,
    onDeleteHistoryItem: (CustomerHistoryEntity) -> Unit,
    onAddRideForCustomer: (
        customerName: String,
        phone: String,
        pickup: String,
        dropoff: String,
        fare: Double,
        amountPaid: Double,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onAddParcelForCustomer: (
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        pickupAddress: String,
        deliveryAddress: String,
        deliveryCharges: Double,
        amountPaid: Double,
        isDelivered: Boolean,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current

    val debtor = state.allDebtors.find {
        it.name.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
    }
    val currentBakaya = debtor?.remainingDebt ?: 0.0

    val historyItems = state.customerHistory.filter {
        it.customerName.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
    }

    var showEditBakayaDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddRideDialog by remember { mutableStateOf(false) }
    var showAddParcelDialog by remember { mutableStateOf(false) }
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

            // Outstanding Balance & Customer Financial Summary Card
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

                    // Row 1 Actions: Add Ride & Add Saman / Parcel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddRideDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("customer_add_ride_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Ride", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAddParcelDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("customer_add_parcel_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Saman", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2 Actions: Add Payment & Edit Bakaya & Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddPaymentDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("customer_add_payment_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Payment")
                        }

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
                                Text("Clear Rs. 0")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3 Actions: PDF & WhatsApp Statement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = PdfReportGenerator.generateCustomerLedgerPdf(
                                    context = context,
                                    customerName = customerName,
                                    phone = phone,
                                    remainingDebt = currentBakaya,
                                    history = historyItems
                                )
                                ShareUtil.sharePdf(context, file)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("customer_pdf_report_button")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = RedError, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Statement", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val msg = "Assalam-o-Alaikum $customerName,\nYour current account statement with Aqeel Rider:\nOutstanding Balance (Bakaya): Rs. ${currentBakaya.toInt()}\nTotal activities logged: ${historyItems.size}\nThank you!"
                                ShareUtil.shareViaWhatsApp(context, phone, msg)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("customer_whatsapp_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = GreenPrimary, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp Statement", fontWeight = FontWeight.Bold)
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
                                    text = DateTimeUtils.formatDateTime(item.timestamp),
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

    if (showAddRideDialog) {
        AddEditRideDialog(
            rideToEdit = null,
            onDismiss = { showAddRideDialog = false },
            onConfirm = { cName, cPhone, pickup, dropoff, fare, paid, notes ->
                val finalName = if (cName.isNotBlank() && cName != "Customer") cName else customerName
                val finalPhone = if (cPhone.isNotBlank()) cPhone else phone
                onAddRideForCustomer(finalName, finalPhone, pickup, dropoff, fare, paid, notes)
                showAddRideDialog = false
            }
        )
    }

    if (showAddParcelDialog) {
        AddEditParcelDialog(
            parcelToEdit = null,
            onDismiss = { showAddParcelDialog = false },
            onConfirm = { sName, sPhone, rName, rPhone, pickup, delivery, charges, paid, isDelivered, notes ->
                val finalSender = if (sName.isNotBlank() && sName != "Customer") sName else customerName
                val finalSenderPhone = if (sPhone.isNotBlank()) sPhone else phone
                onAddParcelForCustomer(
                    finalSender,
                    finalSenderPhone,
                    rName,
                    rPhone,
                    pickup,
                    delivery,
                    charges,
                    paid,
                    isDelivered,
                    notes
                )
                showAddParcelDialog = false
            }
        )
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

    if (showAddPaymentDialog) {
        RecordPaymentDialog(
            debtorName = customerName,
            remainingDebt = currentBakaya,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                onRecordPayment(amount, note)
                showAddPaymentDialog = false
            }
        )
    }
}
