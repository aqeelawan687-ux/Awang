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
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.DebtorEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.ui.components.DebtCard
import com.example.ui.components.EditPaymentDialog
import com.example.ui.components.PaymentHistoryDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel

@Composable
fun DebtsScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onOpenAddDebtor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCustomerAccount by remember { mutableStateOf<DebtorEntity?>(null) }

    var selectedDebtorForPayment by remember { mutableStateOf<DebtorEntity?>(null) }
    var selectedDebtorForHistory by remember { mutableStateOf<DebtorEntity?>(null) }
    var selectedPaymentToEdit by remember { mutableStateOf<PaymentHistoryEntity?>(null) }
    var historyList by remember { mutableStateOf<List<PaymentHistoryEntity>>(emptyList()) }
    val totalHisabStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${state.totalQarza.toInt()}"

    // If a customer account is clicked, display full CustomerAccountScreen!
    val activeCustomer = selectedCustomerAccount
    if (activeCustomer != null) {
        // Find latest updated version of activeCustomer from state
        val updatedCustomer = state.debtors.find { it.id == activeCustomer.id } ?: activeCustomer
        CustomerAccountScreen(
            debtor = updatedCustomer,
            state = state,
            viewModel = viewModel,
            onBackClick = { selectedCustomerAccount = null },
            modifier = modifier
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddDebtor,
                containerColor = AccentRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_debtor")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("debts_screen")
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
                        text = "Customer Payment / کسٹمر پیمنٹ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Total Customer Baqaya: $totalHisabStr",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentRed,
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
                        onClick = onOpenAddDebtor,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("header_add_debtor_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Naya Banda", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SEARCH BAR
            OutlinedTextField(
                value = state.searchQueryDebts,
                onValueChange = { viewModel.setSearchQueryDebts(it) },
                placeholder = { Text("Customer ka Naam ya Phone Number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debtor_search_bar"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Debtor / Customer List
            if (state.filteredDebtors.isEmpty()) {
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
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.searchQueryDebts.isBlank()) "Koi customer add nahi hai." else "Koi customer nahi mila.",
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
                    items(state.filteredDebtors) { debtor ->
                        val (custEarnings, custRideCount) = remember(debtor.name, state.rides) {
                            val name = debtor.name.trim().lowercase()
                            val cRides = state.rides.filter { r ->
                                r.debtorId == debtor.id || r.note.lowercase().contains("customer: $name") || r.note.lowercase().contains(name)
                            }
                            Pair(cRides.sumOf { it.fareAmount }, cRides.size)
                        }

                        DebtCard(
                            debtor = debtor,
                            onCardClick = {
                                selectedCustomerAccount = debtor
                            },
                            onPaymentClick = { selectedDebtorForPayment = debtor },
                            onHistoryClick = {
                                selectedDebtorForHistory = debtor
                                historyList = state.paymentHistory.filter { it.debtorId == debtor.id }
                            },
                            onWhatsAppReminderClick = {
                                viewModel.sendDebtorReminder(context, debtor)
                            },
                            onDeleteClick = {
                                viewModel.deleteDebtor(debtor)
                            },
                            isBalanceHidden = state.isBalanceHidden,
                            customerRideEarning = custEarnings,
                            customerRideCount = custRideCount
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }

    // Payment Deduction Dialog ("Paise Mil Gaye")
    selectedDebtorForPayment?.let { debtor ->
        RecordPaymentDialog(
            debtor = debtor,
            onDismiss = { selectedDebtorForPayment = null },
            onConfirm = { amount, note ->
                viewModel.recordDebtPayment(debtor, amount, note)
                selectedDebtorForPayment = null
            }
        )
    }

    // History Dialog
    selectedDebtorForHistory?.let { debtor ->
        val currentDebtorHistory = state.paymentHistory.filter { it.debtorId == debtor.id || it.debtorName.equals(debtor.name, ignoreCase = true) }
        PaymentHistoryDialog(
            debtorName = debtor.name,
            history = currentDebtorHistory,
            onDismiss = { selectedDebtorForHistory = null },
            onEditPayment = { payment ->
                selectedPaymentToEdit = payment
            },
            onDeletePayment = { payment ->
                viewModel.deletePayment(payment)
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
}
