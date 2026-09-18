package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedError
import com.example.util.DateTimeUtils

@Composable
fun CustomerPdfSelectionDialog(
    customerName: String,
    customerRides: List<RideEntity>,
    customerParcels: List<ParcelEntity>,
    otherHistory: List<CustomerHistoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onGenerate: (
        selectedRideIds: Set<Long>,
        selectedParcelIds: Set<Long>,
        selectedOtherHistoryIds: Set<Long>
    ) -> Unit
) {
    // Default: all existing transactions are marked
    var selectedRideIds by remember { mutableStateOf(customerRides.map { it.id }.toSet()) }
    var selectedParcelIds by remember { mutableStateOf(customerParcels.map { it.id }.toSet()) }
    var selectedOtherIds by remember { mutableStateOf(otherHistory.map { it.id }.toSet()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Rides/Duty, 1: Saman/Parcels, 2: Other/Payments

    val totalRidesCount = customerRides.size
    val totalParcelsCount = customerParcels.size
    val totalOtherCount = otherHistory.size

    val tabs = mutableListOf<String>().apply {
        add("Rides / Duty ($totalRidesCount)")
        add("Saman / Parcels ($totalParcelsCount)")
        if (totalOtherCount > 0) {
            add("Payments ($totalOtherCount)")
        }
    }

    val totalSelected = selectedRideIds.size + selectedParcelIds.size + selectedOtherIds.size
    val totalAvailable = totalRidesCount + totalParcelsCount + totalOtherCount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = RedError
            )
        },
        title = {
            Column {
                Text(
                    text = "Select Transactions for PDF",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Customer: $customerName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_pdf_selection_dialog")
            ) {
                Text(
                    text = "Mark items to show their full row details in the PDF. Unmarked items will be hidden from table rows, but their balances remain accurately included in the totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Select All / Unselect All Quick Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Marked: $totalSelected of $totalAvailable",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row {
                        TextButton(
                            onClick = {
                                selectedRideIds = customerRides.map { it.id }.toSet()
                                selectedParcelIds = customerParcels.map { it.id }.toSet()
                                selectedOtherIds = otherHistory.map { it.id }.toSet()
                            }
                        ) {
                            Text("Mark All", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = {
                                selectedRideIds = emptySet()
                                selectedParcelIds = emptySet()
                                selectedOtherIds = emptySet()
                            }
                        ) {
                            Text("Unmark All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Rides (${customerRides.size})", maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Parcels (${customerParcels.size})", maxLines = 1) }
                    )
                    if (totalOtherCount > 0) {
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Payments (${otherHistory.size})", maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of items under active tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (customerRides.isEmpty()) {
                                item {
                                    Text(
                                        text = "No rides/duty logs recorded for this customer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(customerRides, key = { "ride_${it.id}" }) { ride ->
                                    val isMarked = selectedRideIds.contains(ride.id)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedRideIds = if (isMarked) {
                                                    selectedRideIds - ride.id
                                                } else {
                                                    selectedRideIds + ride.id
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMarked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isMarked,
                                                onCheckedChange = { checked ->
                                                    selectedRideIds = if (checked) selectedRideIds + ride.id else selectedRideIds - ride.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = BluePrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${ride.pickupLocation} → ${ride.dropoffLocation}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = DateTimeUtils.formatDateTime(ride.rideDate),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Rs. ${ride.fare.toInt()}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (ride.remainingBakaya > 0) {
                                                    Text(
                                                        text = "Bakaya: ${ride.remainingBakaya.toInt()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (customerParcels.isEmpty()) {
                                item {
                                    Text(
                                        text = "No saman/parcel deliveries recorded for this customer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(customerParcels, key = { "parcel_${it.id}" }) { parcel ->
                                    val isMarked = selectedParcelIds.contains(parcel.id)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedParcelIds = if (isMarked) {
                                                    selectedParcelIds - parcel.id
                                                } else {
                                                    selectedParcelIds + parcel.id
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMarked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isMarked,
                                                onCheckedChange = { checked ->
                                                    selectedParcelIds = if (checked) selectedParcelIds + parcel.id else selectedParcelIds - parcel.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = BluePrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                val desc = if (parcel.shopName.isNotBlank()) "Shop: ${parcel.shopName}" else "To: ${parcel.receiverName}"
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${DateTimeUtils.formatDateTime(parcel.date)} • ${parcel.deliveryAddress}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Rs. ${parcel.totalCharges.toInt()}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (parcel.remainingBakaya > 0) {
                                                    Text(
                                                        text = "Bakaya: ${parcel.remainingBakaya.toInt()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RedError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (otherHistory.isEmpty()) {
                                item {
                                    Text(
                                        text = "No payments or extra ledger items.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(otherHistory, key = { "other_${it.id}" }) { item ->
                                    val isMarked = selectedOtherIds.contains(item.id)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedOtherIds = if (isMarked) {
                                                    selectedOtherIds - item.id
                                                } else {
                                                    selectedOtherIds + item.id
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMarked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isMarked,
                                                onCheckedChange = { checked ->
                                                    selectedOtherIds = if (checked) selectedOtherIds + item.id else selectedOtherIds - item.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Payment,
                                                contentDescription = null,
                                                tint = GreenPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.details,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = DateTimeUtils.formatDateTime(item.timestamp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Rs. ${item.amount.toInt()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenPrimary
                                            )
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
            Button(
                onClick = {
                    onGenerate(selectedRideIds, selectedParcelIds, selectedOtherIds)
                },
                modifier = Modifier.testTag("confirm_pdf_generate_button")
            ) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
