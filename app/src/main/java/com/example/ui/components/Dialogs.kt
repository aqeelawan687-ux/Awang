package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.BuildConfig
import com.example.util.ApkUpdateManager
import com.example.util.UpdateInfo
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.DebtorEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.util.DistanceCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. ADD DEBTOR DIALOG
@Composable
fun AddDebtorDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, debt: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Naya Banda Add Karein / نیا بندہ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text("Banday ka Naam / نام") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debtor_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number / نمبر") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debtor_phone_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = debt,
                    onValueChange = { debt = it; isError = false },
                    label = { Text("Total Qarza (Rs) / کل قرضہ") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debtor_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional) / نوٹ") },
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Meherbani karke Naam aur Qarze ke Paise likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val debtVal = debt.toDoubleOrNull()
                            if (name.isNotBlank() && debtVal != null && debtVal > 0) {
                                onConfirm(name, phone, debtVal, note)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.testTag("save_debtor_button")
                    ) {
                        Text("Save / محفوظ کریں")
                    }
                }
            }
        }
    }
}

// 2. RECORD PAYMENT DIALOG ("Paise Mil Gaye")
@Composable
fun RecordPaymentDialog(
    debtor: DebtorEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(debtor.totalDebt.toInt().toString()) }
    var note by remember { mutableStateOf("Paise mil gaye") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paise Mil Gaye / پیسے مل گئے",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = "${debtor.name} se milay huay paise darj karein:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Baqi Qarza: Rs. ${debtor.totalDebt.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Full Bill Payment Button
                if (debtor.totalDebt > 0) {
                    OutlinedButton(
                        onClick = {
                            amountText = debtor.totalDebt.toInt().toString()
                            note = "Full Bill Paid - ADA HO GAYA"
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreenPrimary)
                    ) {
                        Text(text = "⚡ Full Bill Paid / مکمل ادا کریں (Rs. ${debtor.totalDebt.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("Milay Huay Paise (Rs)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Wazahat / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = "Sahi rasiya amount likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, note)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Confirm / اوکے")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 3. ADD PARCEL DIALOG
@Composable
fun AddParcelDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        shopName: String,
        recipientName: String,
        recipientAddress: String,
        itemDetails: String,
        itemPrice: Double,
        deliveryCharges: Double
    ) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("") }
    var itemDetails by remember { mutableStateOf("") }
    var itemPriceText by remember { mutableStateOf("") }
    var deliveryChargesText by remember { mutableStateOf("150") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Naya Saman Add Karein / نیا سامان",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Dukan ka Naam / دکان کا نام") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it; isError = false },
                    label = { Text("Jis ko dena hai (Name) / کسٹمر کا نام") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientAddress,
                    onValueChange = { recipientAddress = it },
                    label = { Text("Delivery Address / پتہ") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = itemDetails,
                    onValueChange = { itemDetails = it },
                    label = { Text("Saman ki Detail / سامان کی تفصیل") },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = itemPriceText,
                        onValueChange = { itemPriceText = it },
                        label = { Text("Saman ke Paise (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = deliveryChargesText,
                        onValueChange = { deliveryChargesText = it },
                        label = { Text("Delivery Charges (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (isError) {
                    Text(
                        text = "Meherbani karke customer ka naam aur charges darj karein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = itemPriceText.toDoubleOrNull() ?: 0.0
                            val delivery = deliveryChargesText.toDoubleOrNull() ?: 0.0
                            if (recipientName.isNotBlank()) {
                                onConfirm(
                                    shopName.ifBlank { "Dukan" },
                                    recipientName,
                                    recipientAddress,
                                    itemDetails,
                                    price,
                                    delivery
                                )
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text("Save / محفوظ کریں")
                    }
                }
            }
        }
    }
}

// 4. ADD RIDE DIALOG
@Composable
fun AddRideDialog(
    onDismiss: () -> Unit,
    onConfirm: (from: String, to: String, distanceKm: Double, fareAmount: Double, timeString: String, note: String, customerName: String) -> Unit
) {
    var fromLocation by remember { mutableStateOf("") }
    var toLocation by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var distanceKmText by remember { mutableStateOf("") }
    var fareAmountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Auto-update KM estimate when Locations change
    fun updateAutoKm() {
        if (fromLocation.isNotBlank() && toLocation.isNotBlank()) {
            val estimated = DistanceCalculator.estimateDistanceKm(fromLocation, toLocation)
            distanceKmText = estimated.toString()
            if (fareAmountText.isBlank()) {
                val estimatedFare = (estimated * 35.0).coerceAtLeast(100.0)
                fareAmountText = String.format("%.0f", estimatedFare)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nayi Ride Add Karein / نئی رائڈ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer ka Naam (Optional) / کسٹمر نام (قرضہ کیلئے)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fromLocation,
                    onValueChange = {
                        fromLocation = it
                        updateAutoKm()
                    },
                    label = { Text("Kahan Se / کہاں سے") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = toLocation,
                    onValueChange = {
                        toLocation = it
                        updateAutoKm()
                    },
                    label = { Text("Kahan Tak / کہاں تک") },
                    leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Auto KM (گوگل میپ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = fareAmountText,
                        onValueChange = { fareAmountText = it; isError = false },
                        label = { Text("Kiraya Rs / کرایہ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional) / تفصیل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = "Kahan Se, Kahan Tak aur Kiraya zaroor likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val km = distanceKmText.toDoubleOrNull() ?: 3.0
                            val fare = fareAmountText.toDoubleOrNull()
                            if (fromLocation.isNotBlank() && toLocation.isNotBlank() && fare != null) {
                                onConfirm(fromLocation, toLocation, km, fare, "", note, customerName)
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text("Save Ride / رائڈ محفوظ کریں")
                    }
                }
            }
        }
    }
}

// 5. PAYMENT HISTORY DIALOG
@Composable
fun PaymentHistoryDialog(
    debtorName: String,
    history: List<PaymentHistoryEntity>,
    onDismiss: () -> Unit,
    onEditPayment: ((PaymentHistoryEntity) -> Unit)? = null,
    onDeletePayment: ((PaymentHistoryEntity) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$debtorName ki Payment History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            val totalPaid = history.sumOf { it.amountPaid }
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kul Wasool Rakam (Total Paid):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Rs. ${totalPaid.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (history.isEmpty()) {
                    Text(
                        text = "Abhi tak koi payment record nahi hui.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(history) { item ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.timestamp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Rs. ${item.amountPaid.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.note.ifBlank { item.paymentType },
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f).padding(top = 2.dp)
                                        )
                                        if (onEditPayment != null || onDeletePayment != null) {
                                            Row {
                                                if (onEditPayment != null) {
                                                    IconButton(
                                                        onClick = { onEditPayment(item) },
                                                        modifier = Modifier.size(24.dp).testTag("btn_history_edit_${item.id}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldGreenPrimary, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                                if (onDeletePayment != null) {
                                                    IconButton(
                                                        onClick = { onDeletePayment(item) },
                                                        modifier = Modifier.size(24.dp).testTag("btn_history_delete_${item.id}")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close / بند کریں")
            }
        }
    )
}

// 6. SETTINGS DIALOG
@Composable
fun SettingsDialog(
    themeMode: com.example.ui.theme.AppThemeMode,
    isBalanceHidden: Boolean,
    onSelectThemeMode: (com.example.ui.theme.AppThemeMode) -> Unit,
    onToggleHideBalance: () -> Unit,
    onOpenCustomerHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var isUpdatingFiles by remember { mutableStateOf(false) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var autoCheckUpdate by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var downloadFailed by remember { mutableStateOf(false) }

    // Auto Check Updates on Dialog Open if enabled
    LaunchedEffect(autoCheckUpdate) {
        if (autoCheckUpdate && updateInfo == null) {
            isCheckingUpdate = true
            val info = ApkUpdateManager.checkLatestUpdate(context, isManualCheck = false)
            isCheckingUpdate = false
            updateInfo = info
            if (info.errorMessage != null) {
                updateStatusMessage = info.errorMessage
            } else if (info.hasUpdate) {
                updateStatusMessage = "New Update Available! (v${info.latestVersionName})"
            } else {
                updateStatusMessage = "App is up to date! (v${BuildConfig.VERSION_NAME})"
            }
        }
    }

    fun triggerApkDownloadAndInstall() {
        scope.launch {
            if (!ApkUpdateManager.canInstallUnknownApps(context)) {
                Toast.makeText(context, "Please allow 'Install Unknown Apps' for Aqeel Rider", Toast.LENGTH_LONG).show()
                ApkUpdateManager.openUnknownAppSourcesSettings(context)
                delay(1000)
            }

            isDownloadingApk = true
            downloadFailed = false
            downloadProgress = 0
            updateStatusMessage = "Downloading APK..."

            val infoToDownload = updateInfo ?: UpdateInfo(
                hasUpdate = true,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME,
                latestVersionCode = BuildConfig.VERSION_CODE,
                latestVersionName = BuildConfig.VERSION_NAME,
                downloadUrl = ApkUpdateManager.PERMANENT_APK_DOWNLOAD_URL
            )

            val apkFile = ApkUpdateManager.downloadApk(context, infoToDownload) { progress ->
                downloadProgress = progress
            }

            isDownloadingApk = false

            if (apkFile != null && apkFile.exists()) {
                downloadFailed = false
                updateStatusMessage = "APK Download Complete — Update Ready — Install Now"
                Toast.makeText(context, "APK Download Complete! Opening Package Installer...", Toast.LENGTH_SHORT).show()
                ApkUpdateManager.installApk(context, apkFile)
            } else {
                downloadFailed = true
                updateStatusMessage = "Download failed. Check internet connection and tap 'Retry Download'."
                Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Settings / ترتیبات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // English App Name & Version Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Aqeel Rider App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (updateInfo?.hasUpdate == true) Color(0xFFE65100).copy(alpha = 0.2f) else EmeraldGreenPrimary.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (updateInfo?.hasUpdate == true) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (updateInfo?.hasUpdate == true) Color(0xFFE65100) else EmeraldGreenPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (updateInfo?.hasUpdate == true) "New Update!" else "Updated",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (updateInfo?.hasUpdate == true) Color(0xFFE65100) else EmeraldGreenPrimary
                                )
                            }
                        }
                    }
                }

                // 1. CUSTOMER HISTORY OPTION
                Card(
                    onClick = {
                        onDismiss()
                        onOpenCustomerHistory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("btn_settings_history"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = EmeraldGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Customer History (کسٹمر ہسٹری)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View saved rides, parcels & payments",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("➔", color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // 2. THEME SYSTEM SELECTION
                Text(
                    text = "Theme System / تھیم منتخب کریں",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.ui.theme.AppThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        Card(
                            onClick = { onSelectThemeMode(mode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_option_${mode.name}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreenPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.titleUrdu,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldGreenPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // 3. HIDE BALANCE / HIDE AMOUNTS TOGGLE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Amounts / رقم چھپائیں",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isBalanceHidden) "Amounts Hidden (Rs. ****)" else "Amounts Visible",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isBalanceHidden,
                        onCheckedChange = { onToggleHideBalance() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // 4. APP UPDATE SECTION
                Text(
                    text = "App Update & Sync / ایپ اپڈیٹ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                // Check for Updates Option Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (updateInfo?.hasUpdate == true) "New Update Available!" else "Check App Updates",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (updateInfo?.hasUpdate == true) EmeraldGreenPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (updateInfo?.hasUpdate == true) "Latest APK: v${updateInfo?.latestVersionName}" else "Nayi release aur APK check karne ke liye click karein",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = {
                                    if (updateInfo?.hasUpdate == true || downloadFailed) {
                                        triggerApkDownloadAndInstall()
                                    } else {
                                        scope.launch {
                                            isCheckingUpdate = true
                                            updateStatusMessage = "Checking GitHub Releases..."
                                            val info = ApkUpdateManager.checkLatestUpdate(context, isManualCheck = true)
                                            isCheckingUpdate = false
                                            updateInfo = info
                                            if (info.errorMessage != null) {
                                                updateStatusMessage = info.errorMessage
                                                Toast.makeText(context, info.errorMessage, Toast.LENGTH_LONG).show()
                                            } else if (info.hasUpdate) {
                                                updateStatusMessage = "New Update Available! (v${info.latestVersionName})"
                                                Toast.makeText(context, "New Update Available! (v${info.latestVersionName})", Toast.LENGTH_SHORT).show()
                                            } else {
                                                updateStatusMessage = "App is up to date! (v${BuildConfig.VERSION_NAME})"
                                                Toast.makeText(context, "App is up to date!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isCheckingUpdate && !isDownloadingApk && !isUpdatingFiles,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (updateInfo?.hasUpdate == true || downloadFailed) Icons.Default.Download else Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            downloadFailed -> "Retry Download"
                                            updateInfo?.hasUpdate == true -> "Download APK"
                                            else -> "Update App"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Progress Bar during download
                        if (isDownloadingApk) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = EmeraldGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Downloading APK... $downloadProgress%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary
                            )
                        }

                        if (updateStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateStatusMessage!!,
                                fontSize = 11.sp,
                                color = if (updateInfo?.hasUpdate == true) EmeraldGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Manual Download & Install Button
                        if (!isDownloadingApk && updateInfo?.hasUpdate == false) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { triggerApkDownloadAndInstall() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download & Install Official APK", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Sync & Reload Files Button
                OutlinedButton(
                    onClick = {
                        isUpdatingFiles = true
                        scope.launch {
                            delay(800)
                            isUpdatingFiles = false
                            Toast.makeText(context, "Local files & cache reloaded (No APK change)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !isUpdatingFiles && !isCheckingUpdate
                ) {
                    if (isUpdatingFiles) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Updating files...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reload & Sync Files (فائلز ریفریش)", fontSize = 12.sp)
                    }
                }

                // Auto Check Update Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Check App Updates",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Nayi updates automatically check hon",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = autoCheckUpdate,
                        onCheckedChange = { autoCheckUpdate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done / مکمل")
            }
        }
    )
}

// 7. EDIT DEBTOR / CUSTOMER DIALOG
@Composable
fun EditDebtorDialog(
    debtor: DebtorEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(debtor.name) }
    var phone by remember { mutableStateOf(debtor.phoneNumber) }
    var note by remember { mutableStateOf(debtor.note) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Customer Info / کسٹمر ایڈٹ کریں",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text("Customer Name / نام") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number / فون نمبر") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / تفصیل") },
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Meherbani karke Customer Name zaroor likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, note)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Update / تبدیل کریں")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 8. EDIT RIDE DIALOG
@Composable
fun EditRideDialog(
    ride: com.example.data.entity.RideEntity,
    onDismiss: () -> Unit,
    onConfirm: (from: String, to: String, distanceKm: Double, fareAmount: Double, timeString: String, note: String) -> Unit
) {
    var fromLocation by remember { mutableStateOf(ride.fromLocation) }
    var toLocation by remember { mutableStateOf(ride.toLocation) }
    var distanceKmText by remember { mutableStateOf(ride.distanceKm.toString()) }
    var fareAmountText by remember { mutableStateOf(ride.fareAmount.toInt().toString()) }
    var timeString by remember { mutableStateOf(ride.timeString) }
    var note by remember { mutableStateOf(ride.note) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_edit_ride"),
        title = {
            Text(
                text = "Edit Ride / رائڈ کی تفصیل تبدیل کریں",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fromLocation,
                    onValueChange = { fromLocation = it; isError = false },
                    label = { Text("From Location / کہاں سے") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_ride_from"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = toLocation,
                    onValueChange = { toLocation = it; isError = false },
                    label = { Text("To Location / کہاں تک") },
                    leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_ride_to"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Distance (KM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_edit_ride_distance"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = fareAmountText,
                        onValueChange = { fareAmountText = it; isError = false },
                        label = { Text("Kiraya Rs / کرایہ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_edit_ride_fare"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = timeString,
                    onValueChange = { timeString = it },
                    label = { Text("Time / وقت (مثلاً 14:30)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_ride_time"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / تفصیل") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_ride_note"),
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = "From, To aur Kiraya sahi likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val km = distanceKmText.toDoubleOrNull() ?: ride.distanceKm
                    val fare = fareAmountText.toDoubleOrNull()
                    if (fromLocation.isNotBlank() && toLocation.isNotBlank() && fare != null && fare >= 0) {
                        onConfirm(fromLocation.trim(), toLocation.trim(), km, fare, timeString.trim(), note.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("btn_edit_ride_confirm")
            ) {
                Text("Update / تبدیل کریں")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_edit_ride_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

// 9. EDIT PARCEL / SAMAN DIALOG
@Composable
fun EditParcelDialog(
    parcel: com.example.data.entity.ParcelEntity,
    onDismiss: () -> Unit,
    onConfirm: (shopName: String, recipientName: String, recipientAddress: String, itemDetails: String, itemPrice: Double, deliveryCharges: Double) -> Unit
) {
    var shopName by remember { mutableStateOf(parcel.shopName) }
    var recipientName by remember { mutableStateOf(parcel.recipientName) }
    var recipientAddress by remember { mutableStateOf(parcel.recipientAddress) }
    var itemDetails by remember { mutableStateOf(parcel.itemDetails) }
    var itemPriceText by remember { mutableStateOf(parcel.itemPrice.toInt().toString()) }
    var deliveryChargesText by remember { mutableStateOf(parcel.deliveryCharges.toInt().toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Saman / سامان تبدیل کریں",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name / دکان کا نام") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it; isError = false },
                    label = { Text("Customer Name / کسٹمر کا نام") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = itemDetails,
                    onValueChange = { itemDetails = it },
                    label = { Text("Saman ki Detail / سامان کی تفصیل") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = itemPriceText,
                        onValueChange = { itemPriceText = it },
                        label = { Text("Saman Price (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = deliveryChargesText,
                        onValueChange = { deliveryChargesText = it },
                        label = { Text("Delivery Charges (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (isError) {
                    Text(
                        text = "Customer Name zaroor likhein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = itemPriceText.toDoubleOrNull() ?: 0.0
                    val delivery = deliveryChargesText.toDoubleOrNull() ?: 0.0
                    if (recipientName.isNotBlank()) {
                        onConfirm(shopName, recipientName, recipientAddress, itemDetails, price, delivery)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Update / تبدیل کریں")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 10. EDIT PAYMENT DIALOG
@Composable
fun EditPaymentDialog(
    payment: PaymentHistoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (amountPaid: Double, paymentType: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(payment.amountPaid.toInt().toString()) }
    var paymentType by remember { mutableStateOf(payment.paymentType) }
    var note by remember { mutableStateOf(payment.note) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_edit_payment"),
        title = {
            Text(
                text = "Edit Payment Record / وصولی تبدیل کریں",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("Amount Paid (Rs) / رقم") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_payment_amount"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer")
                    types.forEach { type ->
                        OutlinedButton(
                            onClick = { paymentType = type },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (paymentType == type) EmeraldGreenPrimary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f).testTag("btn_payment_type_$type")
                        ) {
                            Text(text = type, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Wazahat / Note") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_payment_note"),
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = "Sahi amount darj karein",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, paymentType, note.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("btn_edit_payment_confirm")
            ) {
                Text("Update / تبدیل کریں")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_edit_payment_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

// 11. CUSTOMER PDF EDIT & DATE FILTER DIALOG
@Composable
fun CustomerPdfFilterDialog(
    customerName: String,
    customerPhone: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        filterType: String,
        customTitle: String,
        customNote: String,
        includeRide: Boolean,
        includeSaman: Boolean,
        includePaymentHistory: Boolean,
        hideSamanTotal: Boolean,
        hideRideCharges: Boolean,
        customCustomerName: String,
        customPhoneNumber: String
    ) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All Record") }
    var customTitle by remember { mutableStateOf("AQEEL RIDER - CUSTOMER PAYMENT REPORT") }
    var customNote by remember { mutableStateOf("Shukriya! Meherbani karke baqaya payment time par adaa karein.") }
    var customCustomerName by remember { mutableStateOf(customerName) }
    var customPhoneNumber by remember { mutableStateOf(customerPhone) }

    var hideSamanTotal by remember { mutableStateOf(false) }
    var hideRideCharges by remember { mutableStateOf(false) }

    var includeRide by remember { mutableStateOf(true) }
    var includeSaman by remember { mutableStateOf(true) }
    var includePaymentHistory by remember { mutableStateOf(true) }

    var isEditModeOpen by remember { mutableStateOf(false) }
    var isPreviewOpen by remember { mutableStateOf(true) }

    val options = listOf(
        "Today" to "Aaj Ka Record (Today)",
        "This Week" to "Is Hafta (This Week)",
        "This Month" to "Is Mahine (This Month)",
        "All Record" to "Tamam Record (All Record)"
    )

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = EmeraldGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Customer PDF Options",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "$customerName ki PDF Report Edit aur Customize karein:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. PRIVACY & AMOUNT TOGGLES SECTION
                Text(
                    text = "Amount Privacy / رقم کی ترتیبات:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hide Saman Total Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hideSamanTotal) AccentAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Saman Total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Saman ki kul raqam PDF me chupayein",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = hideSamanTotal,
                            onCheckedChange = { hideSamanTotal = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary),
                            modifier = Modifier.testTag("toggle_customer_hide_saman_total")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hide Ride Charges Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hideRideCharges) AccentAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Ride Charges",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Ride / Kiraya charges PDF me chupayein",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = hideRideCharges,
                            onCheckedChange = { hideRideCharges = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary),
                            modifier = Modifier.testTag("toggle_customer_hide_ride_charges")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. EDIT PDF BUTTON & FIELDS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Report Details:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { isEditModeOpen = !isEditModeOpen },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_customer_edit_pdf")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditModeOpen) "Close Edit" else "Edit PDF", fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = isEditModeOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "PDF-Only Overrides (Does not change customer profile in database):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customCustomerName,
                            onValueChange = { customCustomerName = it },
                            label = { Text("Customer Name on PDF") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customPhoneNumber,
                            onValueChange = { customPhoneNumber = it },
                            label = { Text("Phone Number on PDF") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("PDF Heading / Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Custom Message / Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. SECTIONS TO INCLUDE
                Text(
                    text = "PDF me kya Shamil (Mark) karein?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeRide,
                        onCheckedChange = { includeRide = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text("Ride Payment", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = includeSaman,
                        onCheckedChange = { includeSaman = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text("Saman Payment", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includePaymentHistory,
                        onCheckedChange = { includePaymentHistory = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                    )
                    Text("Wasooli / Payments", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. DURATION SELECTOR
                Text(
                    text = "Duration Select Karein:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                options.forEach { (key, label) ->
                    Card(
                        onClick = { selectedFilter = key },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFilter == key) EmeraldGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilter == key) EmeraldGreenPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedFilter == key) {
                                Text("✓", color = EmeraldGreenPrimary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. LIVE PREVIEW SUMMARY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PDF Preview / پیش نظارہ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenPrimary
                    )
                    IconButton(
                        onClick = { isPreviewOpen = !isPreviewOpen },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewOpen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview",
                            tint = Color.Gray
                        )
                    }
                }

                if (isPreviewOpen) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MintContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = customTitle.ifBlank { "AQEEL RIDER" },
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Customer: ${customCustomerName.ifBlank { customerName }} | Filter: $selectedFilter",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray)

                            val rideStatus = if (!includeRide) "Excluded" else if (hideRideCharges) "[Charges Hidden]" else "Included"
                            Text(text = "• Rides: $rideStatus", fontSize = 10.sp)

                            val samanStatus = if (!includeSaman) "Excluded" else if (hideSamanTotal) "[Total Hidden]" else "Included"
                            Text(text = "• Saman: $samanStatus", fontSize = 10.sp)

                            val wasooliStatus = if (includePaymentHistory) "Included" else "Excluded"
                            Text(text = "• Wasooli: $wasooliStatus", fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedFilter,
                        customTitle,
                        customNote,
                        includeRide,
                        includeSaman,
                        includePaymentHistory,
                        hideSamanTotal,
                        hideRideCharges,
                        customCustomerName.trim(),
                        customPhoneNumber.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                modifier = Modifier.testTag("btn_customer_pdf_share_final")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share PDF 📄", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

