package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RideEntity
import com.example.ui.components.EditRideDialog
import com.example.ui.components.RideCard
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel

import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton

@Composable
fun RidesScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onOpenAddRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayTotalText = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${state.todayRidesTotal.toInt()}"
    val thisMonthTotalText = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${state.thisMonthRidesTotal.toInt()}"

    var selectedRideToEdit by remember { mutableStateOf<RideEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddRide,
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_ride")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ride")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("rides_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ride / Duty Page",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleBalanceVisibility() }) {
                        Icon(
                            imageVector = if (state.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Hide/Show Balance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onOpenAddRide,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("header_add_ride_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Nayi Ride", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Totals Card: Din ka total & Mahine ka total
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Din ka Total / آج کی کمائی",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todayTotalText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Mahine ka Total / اس مہینے",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = thisMonthTotalText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rides List
            if (state.rides.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Abhi tak koi ride record nahi hui.\nNayi Ride button dabayein!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.rides) { ride ->
                        RideCard(
                            ride = ride,
                            onEditClick = { selectedRideToEdit = ride },
                            onDeleteClick = { viewModel.deleteRide(ride) },
                            isBalanceHidden = state.isBalanceHidden
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }

    selectedRideToEdit?.let { ride ->
        EditRideDialog(
            ride = ride,
            onDismiss = { selectedRideToEdit = null },
            onConfirm = { from, to, distanceKm, fareAmount, timeString, note ->
                viewModel.updateRide(
                    ride.copy(
                        fromLocation = from,
                        toLocation = to,
                        distanceKm = distanceKm,
                        fareAmount = fareAmount,
                        timeString = timeString,
                        note = note
                    )
                )
                selectedRideToEdit = null
            }
        )
    }
}
