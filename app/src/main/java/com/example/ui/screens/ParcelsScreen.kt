package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ParcelCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel

import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import com.example.ui.components.AppHeaderDropdownMenu

@Composable
fun ParcelsScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onOpenAddParcel: () -> Unit,
    onOpenCustomerHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val samanKePaiseStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${state.samanKePaise.toInt()}"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddParcel,
                containerColor = AccentAmber,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_parcel")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Parcel")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("parcels_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saman / Parcel Deliveries",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Saman ke Paise: $samanKePaiseStr",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleBalanceVisibility() }) {
                        Icon(
                            imageVector = if (state.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Hide/Show Balance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onOpenAddParcel,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("header_add_parcel_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Naya Saman", fontWeight = FontWeight.Bold)
                    }

                    AppHeaderDropdownMenu(
                        onOpenSettings = onOpenSettings,
                        onOpenCustomerHistory = onOpenCustomerHistory,
                        isBalanceHidden = state.isBalanceHidden,
                        onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                        onResetApp = { viewModel.resetAppData() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQueryParcels,
                onValueChange = { viewModel.setSearchQueryParcels(it) },
                placeholder = { Text("Dukan ya Customer ka Naam search karein...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parcel_search_bar"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Parcels List
            if (state.filteredParcels.isEmpty()) {
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
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.searchQueryParcels.isBlank()) "Abhi tak koi saman delivery add nahi hui." else "Koi saman nahi mila.",
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
                    items(state.filteredParcels) { parcel ->
                        ParcelCard(
                            parcel = parcel,
                            onToggleDelivered = { viewModel.toggleParcelDelivered(parcel) },
                            onTogglePaid = { viewModel.toggleParcelPaid(parcel) },
                            onDeleteClick = { viewModel.deleteParcel(parcel) },
                            isBalanceHidden = state.isBalanceHidden
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }
}
