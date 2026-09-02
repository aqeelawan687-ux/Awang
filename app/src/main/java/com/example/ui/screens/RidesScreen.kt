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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.RideEntity
import com.example.ui.components.RideCard
import com.example.ui.viewmodel.RiderUiState
import com.example.util.ShareUtil

@Composable
fun RidesScreen(
    state: RiderUiState,
    onAddRide: () -> Unit,
    onEditRide: (RideEntity) -> Unit,
    onDeleteRide: (RideEntity) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onViewCustomerLedger: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().testTag("rides_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rides_search_input"),
                placeholder = { Text("Search passenger, phone, location...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.rideFilter == "ALL",
                    onClick = { onFilterChange("ALL") },
                    label = { Text("All (${state.rides.size})") }
                )
                FilterChip(
                    selected = state.rideFilter == "UNPAID",
                    onClick = { onFilterChange("UNPAID") },
                    label = { Text("Bakaya / Udhaar") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.rides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rides logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.rides, key = { it.id }) { ride ->
                        RideCard(
                            ride = ride,
                            onEdit = { onEditRide(ride) },
                            onDelete = { onDeleteRide(ride) },
                            onWhatsApp = {
                                val msg = "Assalam-o-Alaikum ${ride.customerName},\nYour ride from ${ride.pickupLocation} to ${ride.dropoffLocation}.\nTotal Fare: Rs. ${ride.fare.toInt()}\nPaid: Rs. ${ride.amountPaid.toInt()}\nRemaining Bakaya: Rs. ${ride.remainingBakaya.toInt()}\nThank you for choosing Aqeel Rider!"
                                ShareUtil.shareViaWhatsApp(context, ride.phone, msg)
                            },
                            onViewCustomerLedger = {
                                onViewCustomerLedger(ride.customerName, ride.phone)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddRide,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_ride_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ride")
        }
    }
}
