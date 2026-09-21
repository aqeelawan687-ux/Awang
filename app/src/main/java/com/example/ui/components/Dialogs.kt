package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedError
import com.example.util.DateTimeUtils
import com.example.util.DistanceCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRideDialog(
    rideToEdit: RideEntity? = null,
    existingCustomers: List<DebtorEntity> = emptyList(),
    preselectedCustomerId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        customerName: String,
        phone: String,
        pickup: String,
        dropoff: String,
        fare: Double,
        amountPaid: Double,
        notes: String,
        customerId: Long?,
        rideDate: Long
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedCustomerId by remember {
        mutableStateOf(rideToEdit?.customerId ?: preselectedCustomerId)
    }
    var name by remember { mutableStateOf(rideToEdit?.customerName ?: "") }
    // Phone Number free-text entry removed from this screen on purpose: it is replaced
    // by the Notes / Tafseel field below. The phone value itself is still carried from
    // a linked customer account (or preserved unchanged on edit) so nothing about the
    // customer's existing phone number system elsewhere in the app is affected.
    var phone by remember { mutableStateOf(rideToEdit?.phone ?: "") }
    var pickup by remember { mutableStateOf(rideToEdit?.pickupLocation ?: "") }
    var dropoff by remember { mutableStateOf(rideToEdit?.dropoffLocation ?: "") }
    var fareStr by remember { mutableStateOf(rideToEdit?.fare?.toInt()?.toString() ?: "") }
    var notes by remember { mutableStateOf(rideToEdit?.notes ?: "") }
    var rideDateMillis by remember { mutableStateOf(rideToEdit?.rideDate ?: System.currentTimeMillis()) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val fare = fareStr.toDoubleOrNull() ?: 0.0
    // Paid (Rs.) field removed on purpose: same as Saman/Parcel, any amount already
    // recorded as paid against this ride is preserved untouched (0 for a brand-new
    // ride), and the full Duty Charges amount flows into the Bakaya calculation.
    val existingAmountPaid = rideToEdit?.amountPaid ?: 0.0
    val remainingBakaya = (fare - existingAmountPaid).coerceAtLeast(0.0)

    val calendar = Calendar.getInstance().apply { timeInMillis = rideDateMillis }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = rideDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                rideDateMillis = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = rideDateMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                rideDateMillis = cal.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (rideToEdit != null) "Edit Ride / Duty" else "Add New Ride / Duty",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer Selector (if existing customers exist)
                if (existingCustomers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        val selectedCustomer = existingCustomers.find { it.id == selectedCustomerId }
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Select Existing Customer (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Link Customer Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("-- New / Unlinked Customer --") },
                                onClick = {
                                    selectedCustomerId = null
                                    customerDropdownExpanded = false
                                }
                            )
                            existingCustomers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text("${customer.name} - ${customer.phone}") },
                                    onClick = {
                                        selectedCustomerId = customer.id
                                        name = customer.name
                                        phone = customer.phone
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Tafseel (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ride_notes_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickup,
                    onValueChange = {
                        pickup = it
                        if (fareStr.isEmpty() && pickup.isNotEmpty() && dropoff.isNotEmpty()) {
                            fareStr = DistanceCalculator.calculateFare(pickup, dropoff).toInt().toString()
                        }
                    },
                    label = { Text("Pickup Location") },
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
                        }
                    },
                    label = { Text("Drop-off Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fareStr,
                    onValueChange = { fareStr = it },
                    label = { Text("Duty Charges (Rs.) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ride_fare_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Auto-calculated Bakaya indicator
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (remainingBakaya > 0) RedError.copy(alpha = 0.12f) else GreenPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining Bakaya:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. ${remainingBakaya.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBakaya > 0) RedError else GreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date & Time Picker Row
                Text(
                    text = "Date & Time: ${DateTimeUtils.formatDateTime(rideDateMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = showDatePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Date", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = showTimePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Time", fontSize = 12.sp)
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFare = fareStr.toDoubleOrNull() ?: 0.0
                    val finalPaid = existingAmountPaid
                    val finalName = name.ifBlank { "Customer" }
                    val finalPickup = pickup.ifBlank { "Pickup Location" }
                    val finalDropoff = dropoff.ifBlank { "Drop-off Location" }
                    if (finalFare > 0) {
                        onConfirm(
                            finalName.trim(),
                            phone.trim(),
                            finalPickup.trim(),
                            finalDropoff.trim(),
                            finalFare,
                            finalPaid,
                            notes.trim(),
                            selectedCustomerId,
                            rideDateMillis
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditParcelDialog(
    parcelToEdit: ParcelEntity? = null,
    existingCustomers: List<DebtorEntity> = emptyList(),
    preselectedCustomerId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        pickup: String,
        delivery: String,
        shopName: String,
        samanCharges: Double,
        deliveryCharges: Double,
        amountPaid: Double,
        isDelivered: Boolean,
        notes: String,
        customerId: Long?,
        date: Long
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedCustomerId by remember {
        mutableStateOf(parcelToEdit?.customerId ?: preselectedCustomerId)
    }
    var senderName by remember { mutableStateOf(parcelToEdit?.senderName ?: "") }
    var senderPhone by remember { mutableStateOf(parcelToEdit?.senderPhone ?: "") }
    var shopName by remember { mutableStateOf(parcelToEdit?.shopName ?: "") }
    var receiverName by remember { mutableStateOf(parcelToEdit?.receiverName ?: "") }
    var receiverPhone by remember { mutableStateOf(parcelToEdit?.receiverPhone ?: "") }
    var pickup by remember { mutableStateOf(parcelToEdit?.pickupAddress ?: "") }
    var delivery by remember { mutableStateOf(parcelToEdit?.deliveryAddress ?: "") }
    var samanChargesStr by remember { mutableStateOf(parcelToEdit?.samanCharges?.toInt()?.toString() ?: "") }
    var deliveryChargesStr by remember { mutableStateOf(parcelToEdit?.deliveryCharges?.toInt()?.toString() ?: "") }
    // Amount Paid removed from this screen on purpose: advance/payment against a customer
    // is recorded once from the main Customer Account screen, not per Saman/Parcel entry.
    // A new parcel's full charges become Bakaya; when editing, whatever was already
    // recorded as paid on that parcel is preserved untouched.
    val existingAmountPaid = parcelToEdit?.amountPaid ?: 0.0
    var isDelivered by remember { mutableStateOf(parcelToEdit?.isDelivered ?: false) }
    var notes by remember { mutableStateOf(parcelToEdit?.notes ?: "") }
    var parcelDateMillis by remember { mutableStateOf(parcelToEdit?.date ?: System.currentTimeMillis()) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val samanCharges = samanChargesStr.toDoubleOrNull() ?: 0.0
    val deliveryCharges = deliveryChargesStr.toDoubleOrNull() ?: 0.0
    val totalCharges = samanCharges + deliveryCharges
    val remainingBakaya = (totalCharges - existingAmountPaid).coerceAtLeast(0.0)

    val calendar = Calendar.getInstance().apply { timeInMillis = parcelDateMillis }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = parcelDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                parcelDateMillis = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = parcelDateMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                parcelDateMillis = cal.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (parcelToEdit != null) "Edit Saman / Parcel" else "Add New Saman / Parcel",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer Selector
                if (existingCustomers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        val selectedCustomer = existingCustomers.find { it.id == selectedCustomerId }
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Select Sender / Customer Account (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Link Customer Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("-- New / Unlinked Sender --") },
                                onClick = {
                                    selectedCustomerId = null
                                    customerDropdownExpanded = false
                                }
                            )
                            existingCustomers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text("${customer.name} - ${customer.phone}") },
                                    onClick = {
                                        selectedCustomerId = customer.id
                                        senderName = customer.name
                                        senderPhone = customer.phone
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name / Dukaan (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
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
                    label = { Text("Pickup Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = delivery,
                    onValueChange = { delivery = it },
                    label = { Text("Delivery Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Financials Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = samanChargesStr,
                        onValueChange = { samanChargesStr = it },
                        label = { Text("Saman Charges (Rs.)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = deliveryChargesStr,
                        onValueChange = { deliveryChargesStr = it },
                        label = { Text("Duty Charges (Rs.) *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Total and Bakaya Summary Surface
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Charges:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Rs. ${totalCharges.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Bakaya:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Rs. ${remainingBakaya.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBakaya > 0) RedError else GreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date & Time Picker Row
                Text(
                    text = "Date & Time: ${DateTimeUtils.formatDateTime(parcelDateMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = showDatePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Date", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = showTimePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Time", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivered to Receiver")
                    Switch(checked = isDelivered, onCheckedChange = { isDelivered = it })
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
                    val sCharges = samanChargesStr.toDoubleOrNull() ?: 0.0
                    val dCharges = deliveryChargesStr.toDoubleOrNull() ?: 0.0
                    val total = sCharges + dCharges
                    val finalPaid = existingAmountPaid
                    val finalSender = senderName.ifBlank { "Customer" }
                    val finalReceiver = receiverName.ifBlank { "Receiver" }
                    val finalPickup = pickup.ifBlank { "Pickup Address" }
                    val finalDelivery = delivery.ifBlank { "Delivery Address" }
                    if (total > 0 || dCharges > 0) {
                        onConfirm(
                            finalSender.trim(),
                            senderPhone.trim(),
                            finalReceiver.trim(),
                            receiverPhone.trim(),
                            finalPickup.trim(),
                            finalDelivery.trim(),
                            shopName.trim(),
                            sCharges,
                            dCharges,
                            finalPaid,
                            isDelivered,
                            notes.trim(),
                            selectedCustomerId,
                            parcelDateMillis
                        )
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
    onConfirm: (
        name: String,
        phone: String,
        totalDebt: Double,
        notes: String,
        address: String,
        photoUri: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(debtorToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(debtorToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(debtorToEdit?.address ?: debtorToEdit?.location ?: "") }
    var debtStr by remember { mutableStateOf(debtorToEdit?.remainingDebt?.toInt()?.toString() ?: "") }
    var notes by remember { mutableStateOf(debtorToEdit?.notes ?: "") }
    var photoUri by remember { mutableStateOf(debtorToEdit?.photoUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (debtorToEdit != null) "Edit Customer Profile" else "Add Customer / Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Selection Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Customer Photo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GreenPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (photoUri.isNullOrBlank()) "Select Photo" else "Change Photo", fontSize = 12.sp)
                        }
                        if (!photoUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Remove Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = RedError,
                                modifier = Modifier
                                    .clickable { photoUri = null }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = debtStr,
                    onValueChange = { debtStr = it },
                    label = { Text("Initial Balance / Bakaya (Optional, Default Rs. 0)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Details (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debt = debtStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), phone.trim(), debt, notes.trim(), address.trim(), photoUri)
                    }
                }
            ) {
                Text("Save Customer")
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
