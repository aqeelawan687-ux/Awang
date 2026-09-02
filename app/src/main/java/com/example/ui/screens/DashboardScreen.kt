package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.StatBox
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedError
import com.example.ui.viewmodel.RiderUiState

@Composable
fun DashboardScreen(
    state: RiderUiState,
    onNavigateToRides: () -> Unit,
    onNavigateToParcels: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onQuickAddRide: () -> Unit,
    onQuickAddParcel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onQuickAddRide,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_ride_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Ride", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onQuickAddParcel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_parcel_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Parcel", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Net Cash Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReports() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Net In-Hand Cash",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs. ${state.netCashInHand.toInt()}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Includes Rides, Parcels & Recovered Udhaar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Overview & Key Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Total Rides",
                    value = "${state.rides.size}",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = GreenPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToRides() },
                    subtitle = "Rs. ${state.totalRidePaid.toInt()} collected"
                )
                StatBox(
                    title = "Total Parcels",
                    value = "${state.parcels.size}",
                    icon = Icons.Default.Inventory2,
                    accentColor = BluePrimary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToParcels() },
                    subtitle = "${state.totalDeliveredParcels} delivered"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Outstanding Udhaar",
                    value = "Rs. ${state.totalRemainingDebt.toInt()}",
                    icon = Icons.Default.MoneyOff,
                    accentColor = RedError,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDebts() },
                    subtitle = "${state.totalDebtorsCount} active debtors"
                )
                StatBox(
                    title = "Recovered Cash",
                    value = "Rs. ${state.totalRecoveredCash.toInt()}",
                    icon = Icons.Default.Payments,
                    accentColor = PurpleAccent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDebts() },
                    subtitle = "${state.payments.size} recoveries"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Link to Reports
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReports() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Reports & PDF Statements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View analytics and export PDF accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
