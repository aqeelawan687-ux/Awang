package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.components.AddEditParcelDialog
import com.example.ui.components.AddEditRideDialog
import com.example.ui.components.CustomerPdfSelectionDialog
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
        notes: String,
        customerId: Long?,
        rideDate: Long
    ) -> Unit,
    onAddParcelForCustomer: (
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        pickupAddress: String,
        deliveryAddress: String,
        shopName: String,
        samanCharges: Double,
        deliveryCharges: Double,
        amountPaid: Double,
        isDelivered: Boolean,
        notes: String,
        customerId: Long?,
        date: Long
    ) -> Unit,
    onUpdateRide: ((RideEntity) -> Unit)? = null,
    onUpdateParcel: ((ParcelEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    // Strict customer resolution by ID first, then fallback
    val debtor = state.allDebtors.find {
        (state.selectedCustomerId != null && it.id == state.selectedCustomerId) ||
        it.name.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
    }

    val customerId = debtor?.id ?: state.selectedCustomerId
    val displayName = debtor?.name ?: customerName
    val displayPhone = debtor?.phone ?: phone
    val displayAddress = debtor?.address?.ifBlank { debtor.location } ?: ""
    val photoUri = debtor?.photoUri
    val currentBakaya = debtor?.remainingDebt ?: 0.0

    // Strict customer-isolated history
    val historyItems = state.customerHistory.filter {
        if (customerId != null && it.customerId != null) {
            it.customerId == customerId
        } else {
            it.customerName.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
        }
    }

    // Customer specific stats
    val customerRides = state.allRides.filter {
        if (customerId != null && it.customerId != null) {
            it.customerId == customerId
        } else {
            it.customerName.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone)
        }
    }

    val customerParcels = state.allParcels.filter {
        if (customerId != null && it.customerId != null) {
            it.customerId == customerId
        } else {
            it.senderName.equals(customerName, ignoreCase = true) || (phone.isNotEmpty() && it.senderPhone == phone)
        }
    }

    var showEditBakayaDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddRideDialog by remember { mutableStateOf(false) }
    var showAddParcelDialog by remember { mutableStateOf(false) }
    var rideToEdit by remember { mutableStateOf<RideEntity?>(null) }
    var parcelToEdit by remember { mutableStateOf<ParcelEntity?>(null) }
    var showPdfSelectionDialog by remember { mutableStateOf(false) }
    var newBakayaStr by remember { mutableStateOf(currentBakaya.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showPdfSelectionDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = RedError)
                    }
                    IconButton(onClick = {
                        val msg = "Assalam-o-Alaikum $displayName,\nYour current account statement with Aqeel Rider:\nOutstanding Balance (Bakaya): Rs. ${currentBakaya.toInt()}\nTotal activities logged: ${historyItems.size}\nThank you!"
                        ShareUtil.shareViaWhatsApp(context, displayPhone, msg)
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

            // Outstanding Balance & Customer Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "$displayName photo",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GreenPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (displayPhone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(text = displayPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (displayAddress.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(text = displayAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Outstanding Bakaya Badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = when {
                                    currentBakaya > 0 -> "BAKAYA"
                                    currentBakaya < 0 -> "ADVANCE"
                                    else -> "CLEARED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentBakaya > 0) RedError else GreenPrimary
                            )
                            Text(
                                text = "Rs. ${kotlin.math.abs(currentBakaya.toInt())}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentBakaya > 0) RedError else GreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats row: Rides, Parcels, History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${customerRides.size} Rides", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp), tint = BluePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${customerParcels.size} Parcels", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${historyItems.size} Records", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                showPdfSelectionDialog = true
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
                                val msg = "Assalam-o-Alaikum $displayName,\nYour current account statement with Aqeel Rider:\nOutstanding Balance (Bakaya): Rs. ${currentBakaya.toInt()}\nTotal activities logged: ${historyItems.size}\nThank you!"
                                ShareUtil.shareViaWhatsApp(context, displayPhone, msg)
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
                                    Row {
                                        if (item.activityType == "RIDE") {
                                            val rideMatch = (item.referenceId?.let { refId -> customerRides.find { it.id == refId } })
                                                ?: customerRides.find { r -> item.details.contains(r.pickupLocation) || item.amount == r.fare }
                                            if (rideMatch != null) {
                                                IconButton(
                                                    onClick = {
                                                        rideToEdit = rideMatch
                                                        showAddRideDialog = true
                                                    },
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Ride",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.height(16.dp)
                                                    )
                                                }
                                            }
                                        } else if (item.activityType == "PARCEL") {
                                            val parcelMatch = (item.referenceId?.let { refId -> customerParcels.find { it.id == refId } })
                                                ?: customerParcels.find { p -> item.details.contains(p.deliveryAddress) || item.amount == p.totalCharges }
                                            if (parcelMatch != null) {
                                                IconButton(
                                                    onClick = {
                                                        parcelToEdit = parcelMatch
                                                        showAddParcelDialog = true
                                                    },
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Parcel",
                                                        tint = BluePrimary,
                                                        modifier = Modifier.height(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteHistoryItem(item) },
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedError, modifier = Modifier.height(16.dp))
                                        }
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
            rideToEdit = rideToEdit,
            existingCustomers = state.allDebtors,
            preselectedCustomerId = customerId,
            onDismiss = {
                showAddRideDialog = false
                rideToEdit = null
            },
            onConfirm = { cName, cPhone, pickup, dropoff, fare, paid, notes, cId, rideDate ->
                val finalName = if (cName.isNotBlank() && cName != "Customer") cName else displayName
                val finalPhone = if (cPhone.isNotBlank()) cPhone else displayPhone
                if (rideToEdit != null) {
                    val remaining = (fare - paid).coerceAtLeast(0.0)
                    val paymentStatus = when {
                        remaining <= 0 -> "PAID"
                        paid > 0 -> "PARTIAL"
                        else -> "UNPAID"
                    }
                    val updatedRide = rideToEdit!!.copy(
                        customerId = cId ?: customerId ?: rideToEdit!!.customerId,
                        customerName = finalName,
                        phone = finalPhone,
                        pickupLocation = pickup,
                        dropoffLocation = dropoff,
                        fare = fare,
                        paymentStatus = paymentStatus,
                        amountPaid = paid,
                        remainingBakaya = remaining,
                        rideDate = rideDate,
                        notes = notes
                    )
                    onUpdateRide?.invoke(updatedRide)
                } else {
                    onAddRideForCustomer(finalName, finalPhone, pickup, dropoff, fare, paid, notes, cId ?: customerId, rideDate)
                }
                showAddRideDialog = false
                rideToEdit = null
            }
        )
    }

    if (showAddParcelDialog) {
        AddEditParcelDialog(
            parcelToEdit = parcelToEdit,
            existingCustomers = state.allDebtors,
            preselectedCustomerId = customerId,
            onDismiss = {
                showAddParcelDialog = false
                parcelToEdit = null
            },
            onConfirm = { sName, sPhone, rName, rPhone, pickup, delivery, shopName, sCharges, dCharges, paid, isDelivered, notes, cId, pDate ->
                val finalSender = if (sName.isNotBlank() && sName != "Customer") sName else displayName
                val finalSenderPhone = if (sPhone.isNotBlank()) sPhone else displayPhone
                if (parcelToEdit != null) {
                    val total = sCharges + dCharges
                    val remaining = (total - paid).coerceAtLeast(0.0)
                    val updatedParcel = parcelToEdit!!.copy(
                        customerId = cId ?: customerId ?: parcelToEdit!!.customerId,
                        senderName = finalSender,
                        senderPhone = finalSenderPhone,
                        receiverName = rName,
                        receiverPhone = rPhone,
                        pickupAddress = pickup,
                        deliveryAddress = delivery,
                        shopName = shopName,
                        samanCharges = sCharges,
                        deliveryCharges = dCharges,
                        amountPaid = paid,
                        remainingBakaya = remaining,
                        isDelivered = isDelivered,
                        isPaid = remaining <= 0,
                        date = pDate,
                        notes = notes
                    )
                    onUpdateParcel?.invoke(updatedParcel)
                } else {
                    onAddParcelForCustomer(
                        finalSender,
                        finalSenderPhone,
                        rName,
                        rPhone,
                        pickup,
                        delivery,
                        shopName,
                        sCharges,
                        dCharges,
                        paid,
                        isDelivered,
                        notes,
                        cId ?: customerId,
                        pDate
                    )
                }
                showAddParcelDialog = false
                parcelToEdit = null
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
            debtorName = displayName,
            remainingDebt = currentBakaya,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                onRecordPayment(amount, note)
                showAddPaymentDialog = false
            }
        )
    }

    if (showPdfSelectionDialog) {
        // Individual Payment/Vasooli entries are excluded here since they no
        // longer appear as separate rows in the generated PDF statement.
        val otherHistory = historyItems.filter {
            it.activityType != "RIDE" && it.activityType != "PARCEL" && it.activityType != "PAYMENT"
        }
        CustomerPdfSelectionDialog(
            customerName = displayName,
            customerRides = customerRides,
            customerParcels = customerParcels,
            otherHistory = otherHistory,
            onDismiss = { showPdfSelectionDialog = false },
            onGenerate = { selectedRideIds, selectedParcelIds, selectedOtherIds ->
                // Map marked rides and parcels to their respective CustomerHistoryEntity IDs
                val markedHistoryIds = mutableSetOf<Long>()

                // Add explicitly marked other items (like direct payments or adjustments)
                markedHistoryIds.addAll(selectedOtherIds)

                // Match history items to rides and parcels
                for (item in historyItems) {
                    when (item.activityType) {
                        "RIDE" -> {
                            if (item.referenceId != null) {
                                if (selectedRideIds.contains(item.referenceId)) {
                                    markedHistoryIds.add(item.id)
                                }
                            } else {
                                // If referenceId is null, match via details or check if any selected ride matches
                                val rideMatch = customerRides.find { ride ->
                                    selectedRideIds.contains(ride.id) &&
                                    (item.details.contains(ride.pickupLocation) || item.amount == ride.fare)
                                }
                                if (rideMatch != null) {
                                    markedHistoryIds.add(item.id)
                                }
                            }
                        }
                        "PARCEL" -> {
                            if (item.referenceId != null) {
                                if (selectedParcelIds.contains(item.referenceId)) {
                                    markedHistoryIds.add(item.id)
                                }
                            } else {
                                val parcelMatch = customerParcels.find { parcel ->
                                    selectedParcelIds.contains(parcel.id) &&
                                    (item.details.contains(parcel.deliveryAddress) || item.amount == parcel.totalCharges)
                                }
                                if (parcelMatch != null) {
                                    markedHistoryIds.add(item.id)
                                }
                            }
                        }
                    }
                }

                val file = PdfReportGenerator.generateCustomerLedgerPdf(
                    context = context,
                    customerName = displayName,
                    phone = displayPhone,
                    remainingDebt = currentBakaya,
                    history = historyItems,
                    markedItemIds = markedHistoryIds,
                    rides = customerRides,
                    parcels = customerParcels
                )
                showPdfSelectionDialog = false
                ShareUtil.sharePdf(context, file)
            }
        )
    }
}
