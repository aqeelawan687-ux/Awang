package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import com.example.ui.components.AddParcelDialog
import com.example.ui.components.AddRideDialog
import com.example.ui.components.AppHeaderDropdownMenu
import com.example.ui.components.CustomerPdfFilterDialog
import com.example.ui.components.EditDebtorDialog
import com.example.ui.components.EditParcelDialog
import com.example.ui.components.EditPaymentDialog
import com.example.ui.components.EditRideDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftRedBg
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomerAccountScreen(
    debtor: DebtorEntity,
    state: RiderUiState,
    viewModel: RiderViewModel,
    onBackClick: () -> Unit,
    onOpenCustomerHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog state controllers
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showAddRideDialog by remember { mutableStateOf(false) }
    var showAddParcelDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showPdfFilterDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerConfirm by remember { mutableStateOf(false) }

    // Item Edit / Delete state controllers
    var selectedRideToEdit by remember { mutableStateOf<RideEntity?>(null) }
    var selectedParcelToEdit by remember { mutableStateOf<ParcelEntity?>(null) }
    var selectedPaymentToEdit by remember { mutableStateOf<PaymentHistoryEntity?>(null) }

    var selectedRideToDelete by remember { mutableStateOf<RideEntity?>(null) }
    var selectedParcelToDelete by remember { mutableStateOf<ParcelEntity?>(null) }
    var selectedPaymentToDelete by remember { mutableStateOf<PaymentHistoryEntity?>(null) }

    // Filter customer transactions with strict customer isolation
    val customerRides = state.rides.filter {
        it.debtorId == debtor.id || (it.debtorId == null && it.note.contains(debtor.name, ignoreCase = true))
    }

    val customerParcels = state.parcels.filter {
        it.debtorId == debtor.id || (it.debtorId == null && it.recipientName.equals(debtor.name, ignoreCase = true))
    }

    val customerPayments = state.paymentHistory.filter {
        it.debtorId == debtor.id || ((it.debtorId == null || it.debtorId == 0L) && it.debtorName.equals(debtor.name, ignoreCase = true))
    }

    // Customer Financial Totals
    val totalRidePayment = customerRides.sumOf { it.fareAmount }
    val totalSamanPayment = customerParcels.sumOf { it.itemPrice + it.deliveryCharges }
    val totalPaid = customerPayments.sumOf { it.amountPaid }

    // Tagged & Category-wise Allocation
    val taggedRidePaid = customerPayments.filter {
        it.note.contains("Ride", ignoreCase = true) || it.paymentType.contains("Ride", ignoreCase = true)
    }.sumOf { it.amountPaid }

    val taggedParcelPaid = customerPayments.filter {
        it.note.contains("Parcel", ignoreCase = true) || it.note.contains("Saman", ignoreCase = true) || it.paymentType.contains("Parcel", ignoreCase = true)
    }.sumOf { it.amountPaid }

    val untaggedPaid = customerPayments.filter {
        !it.note.contains("Ride", ignoreCase = true) &&
        !it.note.contains("Parcel", ignoreCase = true) &&
        !it.note.contains("Saman", ignoreCase = true) &&
        !it.paymentType.contains("Ride", ignoreCase = true) &&
        !it.paymentType.contains("Parcel", ignoreCase = true)
    }.sumOf { it.amountPaid }

    val rideRemainingBeforeUntagged = (totalRidePayment - taggedRidePaid).coerceAtLeast(0.0)
    val untaggedToRide = minOf(untaggedPaid, rideRemainingBeforeUntagged)
    val untaggedToParcel = untaggedPaid - untaggedToRide

    val ridePaid = taggedRidePaid + untaggedToRide
    val parcelPaid = taggedParcelPaid + untaggedToParcel

    val rideBakaya = (totalRidePayment - ridePaid).coerceAtLeast(0.0)
    val parcelBakaya = (totalSamanPayment - parcelPaid).coerceAtLeast(0.0)

    // Grand Total logic: sum of transactions or legacy debt amount
    val grandTotal = if (customerRides.isNotEmpty() || customerParcels.isNotEmpty()) {
        totalRidePayment + totalSamanPayment
    } else {
        debtor.totalDebt + totalPaid
    }

    val baqaya = (grandTotal - totalPaid).coerceAtLeast(0.0)
    val isFullPaid = baqaya <= 0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("customer_account_screen")
    ) {
        // Top Navigation Header
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "${debtor.name} - Customer Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Customer Account & Payment Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showPdfFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "PDF Report",
                            tint = EmeraldGreenPrimary
                        )
                    }
                    IconButton(onClick = { showEditCustomerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Customer",
                            tint = AccentBlue
                        )
                    }
                    IconButton(onClick = { showDeleteCustomerConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Customer",
                            tint = AccentRed
                        )
                    }
                    AppHeaderDropdownMenu(
                        onOpenSettings = onOpenSettings,
                        onOpenCustomerHistory = onOpenCustomerHistory,
                        isBalanceHidden = state.isBalanceHidden,
                        onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Customer Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFullPaid) MintContainer else SoftRedBg
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isFullPaid) EmeraldGreenPrimary else AccentRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = debtor.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (debtor.phoneNumber.isNotBlank()) {
                                    Text(
                                        text = "📞 ${debtor.phoneNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFullPaid) EmeraldGreenPrimary else AccentRed
                        ) {
                            Text(
                                text = if (isFullPaid) "✨ FULL PAID" else "⚠️ BAQAYA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Ledger Totals Grid
                    // 1. Ride Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Ride Total", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${totalRidePayment.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(text = "Ride Paid", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${ridePaid.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = EmeraldGreenPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Ride Bakaya", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${rideBakaya.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (rideBakaya <= 0) EmeraldGreenPrimary else AccentRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Saman / Parcel Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Parcel Total", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${totalSamanPayment.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(text = "Parcel Paid", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${parcelPaid.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = EmeraldGreenPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Parcel Bakaya", fontSize = 10.5.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${parcelBakaya.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (parcelBakaya <= 0) EmeraldGreenPrimary else AccentRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Grand Total, Total Paid, Total Bakaya
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Grand Total", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${grandTotal.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = AccentBlue
                            )
                        }

                        Column {
                            Text(text = "Total Paid (Wasooli)", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${totalPaid.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldGreenPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Total Bakaya", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "Rs. ${baqaya.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (baqaya <= 0) EmeraldGreenPrimary else AccentRed
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons Grid Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddRideDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Ride", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showAddParcelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Saman", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Payment", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showPdfFilterDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Transaction Tabs: Rides | Saman | Payment History
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Rides (${customerRides.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Saman (${customerParcels.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Payments (${customerPayments.size})", fontSize = 12.sp) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tab Content Items
        when (selectedTabIndex) {
            0 -> {
                // Rides Tab
                if (customerRides.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Text(
                                text = "Is customer ki koi ride record nahi hai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(customerRides) { ride ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                            contentDescription = null,
                                            tint = EmeraldGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${ride.fromLocation} ➔ ${ride.toLocation}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Text(
                                        text = "Rs. ${ride.fareAmount.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreenPrimary,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${dateFormat.format(Date(ride.dateMillis))} | ${ride.distanceKm} KM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )

                                    Row {
                                        IconButton(
                                            onClick = { selectedRideToEdit = ride },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { selectedRideToDelete = ride },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Saman Tab
                if (customerParcels.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Text(
                                text = "Is customer ka koi saman/parcel record nahi hai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(customerParcels) { parcel ->
                        val parcelTotal = parcel.itemPrice + parcel.deliveryCharges
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${parcel.shopName}: ${parcel.itemDetails}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Text(
                                        text = "Rs. ${parcelTotal.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Price: Rs. ${parcel.itemPrice.toInt()} | Delivery: Rs. ${parcel.deliveryCharges.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )

                                    Row {
                                        IconButton(
                                            onClick = { selectedParcelToEdit = parcel },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { selectedParcelToDelete = parcel },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Payments Tab
                if (customerPayments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Text(
                                text = "Is customer ki koi payment record nahi hui.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(customerPayments) { pay ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AttachMoney,
                                            contentDescription = null,
                                            tint = EmeraldGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Wasooli (${pay.paymentType})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldGreenPrimary
                                        )
                                    }

                                    Text(
                                        text = "Rs. ${pay.amountPaid.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreenPrimary,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dateFormat.format(Date(pay.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        if (pay.note.isNotBlank()) {
                                            Text(
                                                text = pay.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { selectedPaymentToEdit = pay },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { selectedPaymentToDelete = pay },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    // DIALOG HANDLERS

    // 1. Edit Customer Dialog
    if (showEditCustomerDialog) {
        EditDebtorDialog(
            debtor = debtor,
            onDismiss = { showEditCustomerDialog = false },
            onConfirm = { name, phone, note ->
                val updated = debtor.copy(
                    name = name.trim(),
                    phoneNumber = phone.trim(),
                    note = note.trim(),
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                viewModel.updateDebtor(updated)
                showEditCustomerDialog = false
            }
        )
    }

    // 2. Add Ride Bound to Customer Dialog
    if (showAddRideDialog) {
        AddRideDialog(
            onDismiss = { showAddRideDialog = false },
            onConfirm = { from, to, distance, fare, time, note, _, imageUri ->
                viewModel.addRideForDebtor(debtor, from, to, distance, fare, time, note, imageUri)
                showAddRideDialog = false
            }
        )
    }

    // 3. Add Parcel Bound to Customer Dialog
    if (showAddParcelDialog) {
        AddParcelDialog(
            onDismiss = { showAddParcelDialog = false },
            onConfirm = { shop, recipient, address, details, price, delivery, samanName, imageUri ->
                viewModel.addParcelForDebtor(
                    debtor = debtor,
                    shopName = shop,
                    recipientAddress = address,
                    itemDetails = details,
                    itemPrice = price,
                    deliveryCharges = delivery,
                    samanName = samanName,
                    imageUri = imageUri
                )
                showAddParcelDialog = false
            }
        )
    }

    // 4. Record Payment Bound to Customer Dialog
    if (showAddPaymentDialog) {
        RecordPaymentDialog(
            debtor = debtor,
            totalDue = grandTotal,
            alreadyPaid = totalPaid,
            currentBakaya = baqaya,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, paymentType, note ->
                viewModel.recordDebtPayment(debtor, amount, note, paymentType)
                showAddPaymentDialog = false
            }
        )
    }

    // 5. PDF Filter & Edit Dialog
    if (showPdfFilterDialog) {
        CustomerPdfFilterDialog(
            customerName = debtor.name,
            customerPhone = debtor.phoneNumber,
            onDismiss = { showPdfFilterDialog = false },
            onConfirm = { filterType, customTitle, customNote, includeRide, includeSaman, includePaymentHistory, hideSamanTotal, hideRideCharges, customCustomerName, customPhoneNumber ->
                showPdfFilterDialog = false
                val filteredRides = filterRidesByDate(customerRides, filterType)
                val filteredParcels = filterParcelsByDate(customerParcels, filterType)
                val filteredPayments = filterPaymentsByDate(customerPayments, filterType)

                val pdfFile = PdfReportGenerator.generateCustomerPdfReport(
                    context = context,
                    debtor = debtor,
                    rides = filteredRides,
                    parcels = filteredParcels,
                    payments = filteredPayments,
                    dateFilterLabel = filterType,
                    customTitle = customTitle,
                    customNote = customNote,
                    includeRide = includeRide,
                    includeSaman = includeSaman,
                    includePaymentHistory = includePaymentHistory,
                    hideSamanTotal = hideSamanTotal,
                    hideRideCharges = hideRideCharges,
                    customCustomerName = customCustomerName,
                    customPhoneNumber = customPhoneNumber,
                    isPdfTotalOnly = state.isPdfTotalOnly
                )
                ShareUtil.shareCustomerPdfReport(context, pdfFile, customCustomerName.ifBlank { debtor.name })
            }
        )
    }

    // 6. Delete Customer Dialog
    if (showDeleteCustomerConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerConfirm = false },
            title = { Text("Customer Delete Karein?") },
            text = { Text("${debtor.name} ka account aur tamam hisab record delete ho jaye ga.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebtor(debtor)
                        showDeleteCustomerConfirm = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ITEM EDIT DIALOGS
    selectedRideToEdit?.let { ride ->
        EditRideDialog(
            ride = ride,
            onDismiss = { selectedRideToEdit = null },
            onConfirm = { from, to, distance, fare, time, note, imageUri ->
                viewModel.updateRide(ride.copy(fromLocation = from, toLocation = to, distanceKm = distance, fareAmount = fare, timeString = time, note = note, imageUri = imageUri))
                selectedRideToEdit = null
            }
        )
    }

    selectedParcelToEdit?.let { parcel ->
        EditParcelDialog(
            parcel = parcel,
            onDismiss = { selectedParcelToEdit = null },
            onConfirm = { shop, recipient, address, details, price, delivery, samanName, imageUri ->
                viewModel.updateParcel(
                    parcel.copy(
                        shopName = shop,
                        recipientName = recipient,
                        recipientAddress = address,
                        itemDetails = details,
                        itemPrice = price,
                        deliveryCharges = delivery,
                        samanName = samanName,
                        imageUri = imageUri
                    )
                )
                selectedParcelToEdit = null
            }
        )
    }

    selectedPaymentToEdit?.let { payment ->
        EditPaymentDialog(
            payment = payment,
            onDismiss = { selectedPaymentToEdit = null },
            onConfirm = { amount, type, note ->
                val oldAmount = payment.amountPaid
                val updated = payment.copy(amountPaid = amount, paymentType = type, note = note)
                viewModel.updatePayment(updated, oldAmountPaid = oldAmount)
                selectedPaymentToEdit = null
            }
        )
    }

    // ITEM DELETE CONFIRMATION DIALOGS
    selectedRideToDelete?.let { ride ->
        AlertDialog(
            onDismissRequest = { selectedRideToDelete = null },
            title = { Text("Ride Delete Karein?") },
            text = { Text("Yeh ride record delete ho jaye ga aur hisab auto recalculate ho ga.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRide(ride)
                        selectedRideToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { selectedRideToDelete = null }) { Text("Cancel") }
            }
        )
    }

    selectedParcelToDelete?.let { parcel ->
        AlertDialog(
            onDismissRequest = { selectedParcelToDelete = null },
            title = { Text("Saman Record Delete Karein?") },
            text = { Text("Yeh saman/parcel record delete ho jaye ga aur total hisab auto update ho ga.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteParcel(parcel)
                        selectedParcelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { selectedParcelToDelete = null }) { Text("Cancel") }
            }
        )
    }

    selectedPaymentToDelete?.let { payment ->
        AlertDialog(
            onDismissRequest = { selectedPaymentToDelete = null },
            title = { Text("Payment Record Delete Karein?") },
            text = { Text("Yeh payment transaction delete ho jaye gi.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(payment)
                        selectedPaymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { selectedPaymentToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// Date Filter Helpers
private fun filterRidesByDate(rides: List<RideEntity>, filterType: String): List<RideEntity> {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    return when (filterType) {
        "Today" -> {
            val todayDay = cal.get(Calendar.DAY_OF_YEAR)
            val todayYear = cal.get(Calendar.YEAR)
            rides.filter {
                val rCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                rCal.get(Calendar.DAY_OF_YEAR) == todayDay && rCal.get(Calendar.YEAR) == todayYear
            }
        }
        "This Week" -> {
            val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
            rides.filter { it.dateMillis >= weekAgo }
        }
        "This Month" -> {
            val curMonth = cal.get(Calendar.MONTH)
            val curYear = cal.get(Calendar.YEAR)
            rides.filter {
                val rCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                rCal.get(Calendar.MONTH) == curMonth && rCal.get(Calendar.YEAR) == curYear
            }
        }
        else -> rides
    }
}

private fun filterParcelsByDate(parcels: List<ParcelEntity>, filterType: String): List<ParcelEntity> {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    return when (filterType) {
        "Today" -> {
            val todayDay = cal.get(Calendar.DAY_OF_YEAR)
            val todayYear = cal.get(Calendar.YEAR)
            parcels.filter {
                val pCal = Calendar.getInstance().apply { timeInMillis = it.createdTimestamp }
                pCal.get(Calendar.DAY_OF_YEAR) == todayDay && pCal.get(Calendar.YEAR) == todayYear
            }
        }
        "This Week" -> {
            val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
            parcels.filter { it.createdTimestamp >= weekAgo }
        }
        "This Month" -> {
            val curMonth = cal.get(Calendar.MONTH)
            val curYear = cal.get(Calendar.YEAR)
            parcels.filter {
                val pCal = Calendar.getInstance().apply { timeInMillis = it.createdTimestamp }
                pCal.get(Calendar.MONTH) == curMonth && pCal.get(Calendar.YEAR) == curYear
            }
        }
        else -> parcels
    }
}

private fun filterPaymentsByDate(payments: List<PaymentHistoryEntity>, filterType: String): List<PaymentHistoryEntity> {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    return when (filterType) {
        "Today" -> {
            val todayDay = cal.get(Calendar.DAY_OF_YEAR)
            val todayYear = cal.get(Calendar.YEAR)
            payments.filter {
                val pCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                pCal.get(Calendar.DAY_OF_YEAR) == todayDay && pCal.get(Calendar.YEAR) == todayYear
            }
        }
        "This Week" -> {
            val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
            payments.filter { it.timestamp >= weekAgo }
        }
        "This Month" -> {
            val curMonth = cal.get(Calendar.MONTH)
            val curYear = cal.get(Calendar.YEAR)
            payments.filter {
                val pCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                pCal.get(Calendar.MONTH) == curMonth && pCal.get(Calendar.YEAR) == curYear
            }
        }
        else -> payments
    }
}
