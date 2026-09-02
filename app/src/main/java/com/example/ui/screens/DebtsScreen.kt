package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.data.entity.DebtorEntity
import com.example.ui.components.DebtorCard
import com.example.ui.viewmodel.RiderUiState
import com.example.util.ShareUtil

@Composable
fun DebtsScreen(
    state: RiderUiState,
    onAddDebtor: () -> Unit,
    onRecordPayment: (DebtorEntity) -> Unit,
    onViewCustomerLedger: (name: String, phone: String) -> Unit,
    onEditDebtor: (DebtorEntity) -> Unit,
    onDeleteDebtor: (DebtorEntity) -> Unit,
    onSearchChange: (String) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().testTag("debts_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debts_search_input"),
                placeholder = { Text("Search debtor name or phone...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.debtors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No debtors recorded. All accounts settled!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.debtors, key = { it.id }) { debtor ->
                        DebtorCard(
                            debtor = debtor,
                            onRecordPayment = { onRecordPayment(debtor) },
                            onViewCustomerLedger = {
                                onViewCustomerLedger(debtor.name, debtor.phone)
                            },
                            onWhatsApp = {
                                val msg = "Assalam-o-Alaikum ${debtor.name},\nThis is a gentle reminder regarding outstanding payment of Rs. ${debtor.remainingDebt.toInt()}.\nTotal Bill: Rs. ${debtor.totalDebt.toInt()}\nPlease arrange payment at your earliest convenience.\nAqeel Rider Services"
                                ShareUtil.shareViaWhatsApp(context, debtor.phone, msg)
                            },
                            onEdit = { onEditDebtor(debtor) },
                            onDelete = { onDeleteDebtor(debtor) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddDebtor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_debtor_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debtor")
        }
    }
}
