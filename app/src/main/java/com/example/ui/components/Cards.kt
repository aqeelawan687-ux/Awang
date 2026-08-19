package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftRedBg
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.clickable

// 1. DEBTOR CARD (LOGON KA HISAB & CUSTOMER FOLDER)
@Composable
fun DebtCard(
    debtor: DebtorEntity,
    onPaymentClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onWhatsAppReminderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {},
    isBalanceHidden: Boolean = false,
    customerRideEarning: Double = 0.0,
    customerRideCount: Int = 0
) {
    val isOverdue = NotificationHelper.isOverdue(debtor)
    val now = System.currentTimeMillis()
    val sdfDateOnly = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val startDateStr = sdfDateOnly.format(Date(debtor.createdTimestamp))
    val todayDateStr = sdfDateOnly.format(Date(now))
    
    // Days unpaid counter (increments daily until bill paid)
    val daysUnpaid = ((now - debtor.createdTimestamp) / (1000 * 60 * 60 * 24L)).toInt().coerceAtLeast(0) + 1
    
    val isPaid = debtor.totalDebt <= 0
    val debtText = if (isBalanceHidden) "Rs. ****" else "Rs. ${debtor.totalDebt.toInt()}"
    val rideEarningText = if (isBalanceHidden) "Rs. ****" else "Rs. ${customerRideEarning.toInt()}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("debt_card_${debtor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPaid -> MintContainer.copy(alpha = 0.35f)
                isOverdue -> SoftRedBg.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date Tracking Bar (Top Date & Next/Today Date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shuru: $startDateStr ➔ Aaj: $todayDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                if (!isPaid) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (daysUnpaid > 7) AccentRed else AccentAmber
                    ) {
                        Text(
                            text = "⏳ $daysUnpaid Din Unpaid (Ginti Shuru)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreenPrimary
                    ) {
                        Text(
                            text = "✨ BILL PAID (Clean Window)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Customer Name & Main Balance Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isPaid -> EmeraldGreenPrimary
                                    isOverdue -> AccentRed
                                    else -> AccentAmber
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = debtor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (debtor.phoneNumber.isNotBlank()) {
                            Text(
                                text = debtor.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Bakaya Rakam Display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isPaid) "Rs. 0" else debtText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPaid) EmeraldGreenPrimary else AccentRed
                    )
                    Text(
                        text = if (isPaid) "Bakaya: Saf (Paid)" else "Bakaya Rakam (حساب)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaid) EmeraldGreenPrimary else AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Overdue Alert Banner if > 7 days
            if (isOverdue && !isPaid) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$daysUnpaid din se bil paid nahi hua! Remind karayein",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (debtor.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${debtor.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Customer Ride Earning Badge
            if (customerRideCount > 0 || customerRideEarning > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Is Customer Ki Total Ride Earning: $rideEarningText ($customerRideCount rides)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row (Customer Folder Actions: History, Bill Paid, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    // History / Customer Folder Ledger Button
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Payment History Folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // WhatsApp Reminder Button
                    if (!isPaid) {
                        IconButton(
                            onClick = onWhatsAppReminderClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "WhatsApp Reminder",
                                tint = EmeraldGreenPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Customer",
                            tint = Color.Gray
                        )
                    }
                }

                // "Paise Mil Gaye / Bill Paid" Button
                if (!isPaid) {
                    Button(
                        onClick = onPaymentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Bill Paid / رقم ادا کریں")
                    }
                } else {
                    OutlinedButton(
                        onClick = onPaymentClick
                    ) {
                        Text(text = "Add New Debt / نیا قرضہ", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 2. PARCEL CARD (SAMAN / PARCEL)
@Composable
fun ParcelCard(
    parcel: ParcelEntity,
    onToggleDelivered: () -> Unit,
    onTogglePaid: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false
) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(parcel.createdTimestamp))
    val itemPriceText = if (isBalanceHidden) "Rs. ****" else "Rs. ${parcel.itemPrice.toInt()}"
    val deliveryChargesText = if (isBalanceHidden) "Rs. ****" else "Rs. ${parcel.deliveryCharges.toInt()}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parcel_card_${parcel.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = parcel.recipientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From: ${parcel.shopName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (parcel.recipientAddress.isNotBlank()) {
                            Text(
                                text = "📍 ${parcel.recipientAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }

            if (parcel.itemDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saman: ${parcel.itemDetails}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Money details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saman Ke Paise", fontSize = 10.sp, color = Color.Gray)
                    Text(itemPriceText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Delivery Charges", fontSize = 10.sp, color = Color.Gray)
                    Text(deliveryChargesText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldGreenPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pehunch Gaya Toggle
                OutlinedButton(
                    onClick = onToggleDelivered,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (parcel.isDelivered) EmeraldGreenPrimary.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (parcel.isDelivered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (parcel.isDelivered) EmeraldGreenPrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (parcel.isDelivered) "Pehunch Gaya ✅" else "Pehunch Gaya",
                        fontSize = 11.sp,
                        fontWeight = if (parcel.isDelivered) FontWeight.Bold else FontWeight.Normal,
                        color = if (parcel.isDelivered) EmeraldGreenPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Paise Mil Gaye Toggle
                Button(
                    onClick = onTogglePaid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (parcel.isPaid) EmeraldGreenPrimary else AccentAmber
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (parcel.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (parcel.isPaid) "Paise Mil Gaye ✅" else "Paise Mil Gaye",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 3. RIDE CARD (RIDE / DUTY)
@Composable
fun RideCard(
    ride: RideEntity,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    isBalanceHidden: Boolean = false
) {
    val dateStr = SimpleDateFormat("dd MMM", Locale.US).format(Date(ride.dateMillis))
    val fareText = if (isBalanceHidden) "Rs. ****" else "Rs. ${ride.fareAmount.toInt()}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ride_card_${ride.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ride.fromLocation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = ride.toLocation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 ${ride.distanceKm} KM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🕒 ${ride.timeString.ifEmpty { dateStr }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (ride.note.isNotBlank()) {
                    Text(
                        text = ride.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = fareText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldGreenPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_ride_button_${ride.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Ride",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_ride_button_${ride.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
