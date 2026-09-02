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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.entity.ParcelEntity
import com.example.ui.components.ParcelCard
import com.example.ui.viewmodel.RiderUiState
import com.example.util.ShareUtil

@Composable
fun ParcelsScreen(
    state: RiderUiState,
    onAddParcel: () -> Unit,
    onToggleDelivered: (ParcelEntity) -> Unit,
    onTogglePaid: (ParcelEntity) -> Unit,
    onEditParcel: (ParcelEntity) -> Unit,
    onDeleteParcel: (ParcelEntity) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onViewCustomerLedger: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().testTag("parcels_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parcels_search_input"),
                placeholder = { Text("Search sender, receiver, address...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.parcelFilter == "ALL",
                    onClick = { onFilterChange("ALL") },
                    label = { Text("All (${state.parcels.size})") }
                )
                FilterChip(
                    selected = state.parcelFilter == "PENDING",
                    onClick = { onFilterChange("PENDING") },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = state.parcelFilter == "DELIVERED",
                    onClick = { onFilterChange("DELIVERED") },
                    label = { Text("Delivered") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.parcels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No parcels logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.parcels, key = { it.id }) { parcel ->
                        ParcelCard(
                            parcel = parcel,
                            onToggleDelivered = { onToggleDelivered(parcel) },
                            onTogglePaid = { onTogglePaid(parcel) },
                            onEdit = { onEditParcel(parcel) },
                            onDelete = { onDeleteParcel(parcel) },
                            onWhatsApp = {
                                val msg = "Assalam-o-Alaikum,\nParcel Delivery from ${parcel.senderName} to ${parcel.receiverName}.\nDelivery Address: ${parcel.deliveryAddress}\nCharges: Rs. ${parcel.deliveryCharges.toInt()}\nPaid: Rs. ${parcel.amountPaid.toInt()}\nStatus: ${if (parcel.isDelivered) "Delivered" else "In Transit"}\nAqeel Rider Services"
                                ShareUtil.shareViaWhatsApp(context, parcel.receiverPhone.ifEmpty { parcel.senderPhone }, msg)
                            },
                            onViewCustomerLedger = {
                                onViewCustomerLedger(parcel.senderName, parcel.senderPhone)
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddParcel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_parcel_fab"),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Parcel")
        }
    }
}
