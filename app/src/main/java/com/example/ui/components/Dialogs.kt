package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.util.DistanceCalculator

@Composable
fun AddEditRideDialog(
    rideToEdit: RideEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        customerName: String,
        phone: String,
        pickup: String,
        dropoff: String,
        fare: Double,
        amountPaid: Double,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(rideToEdit?.customerName ?: "") }
    var phone by remember { mutableStateOf(rideToEdit?.phone ?: "") }
    var pickup by remember { mutableStateOf(rideToEdit?.pickupLocation ?: "") }
    var dropoff by remember { mutableStateOf(rideToEdit?.dropoffLocation ?: "") }
    var fareStr by remember { mutableStateOf(rideToEdit?.fare?.toInt()?.toString() ?: "") }
    var paidStr by remember { mutableStateOf(rideToEdit?.amountPaid?.toInt()?.toString() ?: "") }
    var notes by remember { mutableStateOf(rideToEdit?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (rideToEdit != null) "Edit Ride" else "Add New Ride",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ride_name_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickup,
                    onValueChange = {
                        pickup = it
                        if (fareStr.isEmpty() && pickup.isNotEmpty() && dropoff.isNotEmpty()) {
                            fareStr = DistanceCalculator.calculateFare(pickup, dropoff).toInt().toString()
                            if (paidStr.isEmpty()) paidStr = fareStr
                        }
                    },
                    label = { Text("Pickup Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dropoff,
                    onValueChange = {
                        dropoff = it
                        if (fareStr.isEmpty() && pickup.isNotEmpty() && dropoff.isNotEmpty()) {
                            fareStr = DistanceCalculator.calculateFare(pickup, dropoff).toInt().toString()
                            if (paidStr.isEmpty()) paidStr = fareStr
                        }
                    },
                    label = { Text("Drop-off Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fareStr,
                        onValueChange = {
                            fareStr = it
                            if (paidStr.isEmpty() || paidStr == "0") paidStr = it
                        },
                        label = { Text("Total Fare (Rs.) *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ride_fare_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it },
                        label = { Text("Paid Cash (Rs.)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ride_paid_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                val fareVal = fareStr.toDoubleOrNull() ?: 0.0
                val paidVal = paidStr.toDoubleOrNull() ?: 0.0
                val bakaya = (fareVal - paidVal).coerceAtLeast(0.0)
                if (bakaya > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bakaya (Udhaar): Rs. ${bakaya.toInt()} will be added to debtor ledger.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fare = fareStr.toDoubleOrNull() ?: 0.0
                    val paid = paidStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && pickup.isNotBlank() && dropoff.isNotBlank() && fare > 0) {
                        onConfirm(name, phone, pickup, dropoff, fare, paid, notes)
                    }
                },
                modifier = Modifier.testTag("save_ride_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditParcelDialog(
    parcelToEdit: ParcelEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        pickup: String,
        delivery: String,
        charges: Double,
        amountPaid: Double,
        isDelivered: Boolean,
        notes: String
    ) -> Unit
) {
    var senderName by remember { mutableStateOf(parcelToEdit?.senderName ?: "") }
    var senderPhone by remember { mutableStateOf(parcelToEdit?.senderPhone ?: "") }
    var receiverName by remember { mutableStateOf(parcelToEdit?.receiverName ?: "") }
    var receiverPhone by remember { mutableStateOf(parcelToEdit?.receiverPhone ?: "") }
    var pickup by remember { mutableStateOf(parcelToEdit?.pickupAddress ?: "") }
    var delivery by remember { mutableStateOf(parcelToEdit?.deliveryAddress ?: "") }
    var chargesStr by remember { mutableStateOf(parcelToEdit?.deliveryCharges?.toInt()?.toString() ?: "") }
    var paidStr by remember { mutableStateOf(parcelToEdit?.amountPaid?.toInt()?.toString() ?: "") }
    var isDelivered by remember { mutableStateOf(parcelToEdit?.isDelivered ?: false) }
    var notes by remember { mutableStateOf(parcelToEdit?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (parcelToEdit != null) "Edit Parcel" else "Add New Parcel Delivery",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { senderPhone = it },
                    label = { Text("Sender Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text("Receiver Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = receiverPhone,
                    onValueChange = { receiverPhone = it },
                    label = { Text("Receiver Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Pickup Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = delivery,
                    onValueChange = { delivery = it },
                    label = { Text("Delivery Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = chargesStr,
                        onValueChange = {
                            chargesStr = it
                            if (paidStr.isEmpty() || paidStr == "0") paidStr = it
                        },
                        label = { Text("Charges (Rs.) *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it },
                        label = { Text("Paid Cash (Rs.)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Item Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val charges = chargesStr.toDoubleOrNull() ?: 0.0
                    val paid = paidStr.toDoubleOrNull() ?: 0.0
                    if (senderName.isNotBlank() && receiverName.isNotBlank() && delivery.isNotBlank() && charges > 0) {
                        onConfirm(senderName, senderPhone, receiverName, receiverPhone, pickup, delivery, charges, paid, isDelivered, notes)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditDebtorDialog(
    debtorToEdit: DebtorEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, totalDebt: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(debtorToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(debtorToEdit?.phone ?: "") }
    var debtStr by remember { mutableStateOf(debtorToEdit?.remainingDebt?.toInt()?.toString() ?: "") }
    var notes by remember { mutableStateOf(debtorToEdit?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (debtorToEdit != null) "Edit Debtor Account" else "Add New Debtor (Khata)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = debtStr,
                    onValueChange = { debtStr = it },
                    label = { Text("Debt Amount (Rs.) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debt = debtStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && debt > 0) {
                        onConfirm(name, phone, debt, notes)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecordPaymentDialog(
    debtorName: String,
    remainingDebt: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(remainingDebt.toInt().toString()) }
    var note by remember { mutableStateOf("Cash Vasooli") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Record Vasooli (Payment)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Customer: $debtorName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current Due: Rs. ${remainingDebt.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Received Amount (Rs.) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Payment Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, note)
                    }
                }
            ) {
                Text("Confirm Vasooli")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
