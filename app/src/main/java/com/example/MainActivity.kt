package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.lifecycleScope
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.components.AddEditDebtorDialog
import com.example.ui.components.AddEditParcelDialog
import com.example.ui.components.AddEditRideDialog
import com.example.ui.components.AppTopBar
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.PdfOptionsDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.screens.CustomerAccountScreen
import com.example.ui.screens.CustomerHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.LicenseActivationScreen
import com.example.ui.screens.LicenseBlockedScreen
import com.example.ui.screens.ParcelsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RidesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AqeelRiderTheme
import com.example.ui.viewmodel.RiderViewModel
import com.example.util.ApkUpdateManager
import com.example.util.LicenseManager
import com.example.util.LicenseState
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: RiderViewModel by viewModels()
    private lateinit var licenseManager: LicenseManager
    private lateinit var updateManager: ApkUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        licenseManager = LicenseManager(this)
        updateManager = ApkUpdateManager(this)

        // Initial background license verification & update check
        lifecycleScope.launch {
            licenseManager.verifyLicense()
            updateManager.checkLatestUpdate()
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val licenseState by licenseManager.licenseState.collectAsState()
            val updateState by updateManager.updateState.collectAsState()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            AqeelRiderTheme(themeMode = uiState.themeMode) {
                // Check License Gate
                when (licenseState) {
                    is LicenseState.Blocked -> {
                        LicenseBlockedScreen(
                            licenseManager = licenseManager,
                            onUnblocked = {
                                licenseManager.init()
                            }
                        )
                    }
                    is LicenseState.Unactivated -> {
                        LicenseActivationScreen(
                            licenseManager = licenseManager,
                            onActivated = {
                                licenseManager.init()
                            }
                        )
                    }
                    else -> {
                        // Main Application
                        var currentTab by remember { mutableStateOf("DASHBOARD") }
                        var currentScreen by remember { mutableStateOf("MAIN") } // MAIN, CUSTOMER_ACCOUNT, SETTINGS

                        // Dialog States
                        var showAddRideDialog by remember { mutableStateOf(false) }
                        var rideToEdit by remember { mutableStateOf<RideEntity?>(null) }

                        var showAddParcelDialog by remember { mutableStateOf(false) }
                        var parcelToEdit by remember { mutableStateOf<ParcelEntity?>(null) }

                        var showAddDebtorDialog by remember { mutableStateOf(false) }
                        var debtorToEdit by remember { mutableStateOf<DebtorEntity?>(null) }

                        var debtorForPayment by remember { mutableStateOf<DebtorEntity?>(null) }
                        var showPdfOptionsDialog by remember { mutableStateOf(false) }

                        when (currentScreen) {
                            "SETTINGS" -> {
                                SettingsScreen(
                                    currentTheme = uiState.themeMode,
                                    onThemeChange = { viewModel.setThemeMode(it) },
                                    licenseState = licenseState,
                                    onCheckUpdate = {
                                        scope.launch {
                                            updateManager.checkLatestUpdate()
                                            Toast.makeText(context, "Checking for latest updates...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onResetLicense = {
                                        licenseManager.resetLicense()
                                    },
                                    onSimulateBlock = {
                                        licenseManager.blockLocally()
                                    },
                                    onSimulateUnblock = {
                                        licenseManager.unblockLocally()
                                    },
                                    onClearAllData = {
                                        viewModel.resetAppData()
                                        Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    onBack = { currentScreen = "MAIN" }
                                )
                            }
                            "CUSTOMER_ACCOUNT" -> {
                                CustomerAccountScreen(
                                    customerName = uiState.selectedCustomerName ?: "Customer",
                                    phone = uiState.selectedCustomerPhone ?: "",
                                    state = uiState,
                                    onBack = { currentScreen = "MAIN" },
                                    onUpdateBakaya = { newBakaya ->
                                        viewModel.updateCustomerBakaya(
                                            uiState.selectedCustomerName ?: "",
                                            uiState.selectedCustomerPhone ?: "",
                                            newBakaya
                                        )
                                    },
                                    onDeleteBakaya = {
                                        viewModel.deleteCustomerBakaya(
                                            uiState.selectedCustomerName ?: "",
                                            uiState.selectedCustomerPhone ?: ""
                                        )
                                    },
                                    onDeleteHistoryItem = { item ->
                                        viewModel.deleteCustomerHistory(item)
                                    }
                                )
                            }
                            else -> {
                                Scaffold(
                                    topBar = {
                                        val title = when (currentTab) {
                                            "DASHBOARD" -> "Aqeel Rider"
                                            "RIDES" -> "Rides Log"
                                            "PARCELS" -> "Parcel Deliveries"
                                            "DEBTS" -> "Debtors (Khata)"
                                            "REPORTS" -> "Reports & PDF"
                                            "HISTORY" -> "Activity Ledger"
                                            else -> "Aqeel Rider"
                                        }
                                        AppTopBar(
                                            title = title,
                                            themeMode = uiState.themeMode,
                                            onToggleTheme = {
                                                val nextMode = when (uiState.themeMode) {
                                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                                    AppThemeMode.DARK -> AppThemeMode.SYSTEM
                                                    AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                                }
                                                viewModel.setThemeMode(nextMode)
                                            },
                                            onOpenSettings = { currentScreen = "SETTINGS" }
                                        )
                                    },
                                    bottomBar = {
                                        NavigationBar(modifier = Modifier.testTag("bottom_nav")) {
                                            NavigationBarItem(
                                                selected = currentTab == "DASHBOARD",
                                                onClick = { currentTab = "DASHBOARD" },
                                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                                label = { Text("Home") },
                                                modifier = Modifier.testTag("nav_dashboard")
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == "RIDES",
                                                onClick = { currentTab = "RIDES" },
                                                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Rides") },
                                                label = { Text("Rides") },
                                                modifier = Modifier.testTag("nav_rides")
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == "PARCELS",
                                                onClick = { currentTab = "PARCELS" },
                                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Parcels") },
                                                label = { Text("Parcels") },
                                                modifier = Modifier.testTag("nav_parcels")
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == "DEBTS",
                                                onClick = { currentTab = "DEBTS" },
                                                icon = { Icon(Icons.Default.MoneyOff, contentDescription = "Udhaar") },
                                                label = { Text("Khata") },
                                                modifier = Modifier.testTag("nav_debts")
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == "REPORTS",
                                                onClick = { currentTab = "REPORTS" },
                                                icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                                                label = { Text("Reports") },
                                                modifier = Modifier.testTag("nav_reports")
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == "HISTORY",
                                                onClick = { currentTab = "HISTORY" },
                                                icon = { Icon(Icons.Default.History, contentDescription = "Ledger") },
                                                label = { Text("Ledger") },
                                                modifier = Modifier.testTag("nav_history")
                                            )
                                        }
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        when (currentTab) {
                                            "DASHBOARD" -> DashboardScreen(
                                                state = uiState,
                                                onNavigateToRides = { currentTab = "RIDES" },
                                                onNavigateToParcels = { currentTab = "PARCELS" },
                                                onNavigateToDebts = { currentTab = "DEBTS" },
                                                onNavigateToReports = { currentTab = "REPORTS" },
                                                onQuickAddRide = {
                                                    rideToEdit = null
                                                    showAddRideDialog = true
                                                },
                                                onQuickAddParcel = {
                                                    parcelToEdit = null
                                                    showAddParcelDialog = true
                                                }
                                            )
                                            "RIDES" -> RidesScreen(
                                                state = uiState,
                                                onAddRide = {
                                                    rideToEdit = null
                                                    showAddRideDialog = true
                                                },
                                                onEditRide = {
                                                    rideToEdit = it
                                                    showAddRideDialog = true
                                                },
                                                onDeleteRide = { viewModel.deleteRide(it) },
                                                onSearchChange = { viewModel.setSearchQuery(it) },
                                                onFilterChange = { viewModel.setRideFilter(it) },
                                                onViewCustomerLedger = { name, phone ->
                                                    viewModel.selectCustomer(name, phone)
                                                    currentScreen = "CUSTOMER_ACCOUNT"
                                                }
                                            )
                                            "PARCELS" -> ParcelsScreen(
                                                state = uiState,
                                                onAddParcel = {
                                                    parcelToEdit = null
                                                    showAddParcelDialog = true
                                                },
                                                onToggleDelivered = { viewModel.toggleParcelDelivered(it) },
                                                onTogglePaid = { viewModel.toggleParcelPaid(it) },
                                                onEditParcel = {
                                                    parcelToEdit = it
                                                    showAddParcelDialog = true
                                                },
                                                onDeleteParcel = { viewModel.deleteParcel(it) },
                                                onSearchChange = { viewModel.setSearchQuery(it) },
                                                onFilterChange = { viewModel.setParcelFilter(it) },
                                                onViewCustomerLedger = { name, phone ->
                                                    viewModel.selectCustomer(name, phone)
                                                    currentScreen = "CUSTOMER_ACCOUNT"
                                                }
                                            )
                                            "DEBTS" -> DebtsScreen(
                                                state = uiState,
                                                onAddDebtor = {
                                                    debtorToEdit = null
                                                    showAddDebtorDialog = true
                                                },
                                                onRecordPayment = { debtorForPayment = it },
                                                onViewCustomerLedger = { name, phone ->
                                                    viewModel.selectCustomer(name, phone)
                                                    currentScreen = "CUSTOMER_ACCOUNT"
                                                },
                                                onEditDebtor = {
                                                    debtorToEdit = it
                                                    showAddDebtorDialog = true
                                                },
                                                onDeleteDebtor = { viewModel.deleteDebtor(it) },
                                                onSearchChange = { viewModel.setSearchQuery(it) }
                                            )
                                            "REPORTS" -> ReportsScreen(
                                                state = uiState,
                                                onOpenPdfOptions = { showPdfOptionsDialog = true }
                                            )
                                            "HISTORY" -> CustomerHistoryScreen(
                                                state = uiState,
                                                onSelectCustomer = { name, phone ->
                                                    viewModel.selectCustomer(name, phone)
                                                    currentScreen = "CUSTOMER_ACCOUNT"
                                                },
                                                onDeleteHistoryItem = { viewModel.deleteCustomerHistory(it) },
                                                onSearchChange = { viewModel.setSearchQuery(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Dialogs
                        if (showAddRideDialog) {
                            AddEditRideDialog(
                                rideToEdit = rideToEdit,
                                onDismiss = {
                                    showAddRideDialog = false
                                    rideToEdit = null
                                },
                                onConfirm = { name, phone, pickup, dropoff, fare, paid, notes ->
                                    if (rideToEdit != null) {
                                        val remaining = (fare - paid).coerceAtLeast(0.0)
                                        viewModel.updateRide(
                                            rideToEdit!!.copy(
                                                customerName = name,
                                                phone = phone,
                                                pickupLocation = pickup,
                                                dropoffLocation = dropoff,
                                                fare = fare,
                                                amountPaid = paid,
                                                remainingBakaya = remaining,
                                                notes = notes
                                            )
                                        )
                                    } else {
                                        viewModel.addRide(name, phone, pickup, dropoff, fare, paid, notes)
                                    }
                                    showAddRideDialog = false
                                    rideToEdit = null
                                }
                            )
                        }

                        if (showAddParcelDialog) {
                            AddEditParcelDialog(
                                parcelToEdit = parcelToEdit,
                                onDismiss = {
                                    showAddParcelDialog = false
                                    parcelToEdit = null
                                },
                                onConfirm = { sName, sPhone, rName, rPhone, pickup, delivery, charges, paid, isDelivered, notes ->
                                    if (parcelToEdit != null) {
                                        val remaining = (charges - paid).coerceAtLeast(0.0)
                                        viewModel.updateParcel(
                                            parcelToEdit!!.copy(
                                                senderName = sName,
                                                senderPhone = sPhone,
                                                receiverName = rName,
                                                receiverPhone = rPhone,
                                                pickupAddress = pickup,
                                                deliveryAddress = delivery,
                                                deliveryCharges = charges,
                                                amountPaid = paid,
                                                remainingBakaya = remaining,
                                                isDelivered = isDelivered,
                                                notes = notes
                                            )
                                        )
                                    } else {
                                        viewModel.addParcel(sName, sPhone, rName, rPhone, pickup, delivery, charges, paid, isDelivered, notes)
                                    }
                                    showAddParcelDialog = false
                                    parcelToEdit = null
                                }
                            )
                        }

                        if (showAddDebtorDialog) {
                            AddEditDebtorDialog(
                                debtorToEdit = debtorToEdit,
                                onDismiss = {
                                    showAddDebtorDialog = false
                                    debtorToEdit = null
                                },
                                onConfirm = { name, phone, debt, notes ->
                                    if (debtorToEdit != null) {
                                        viewModel.updateDebtor(
                                            debtorToEdit!!.copy(
                                                name = name,
                                                phone = phone,
                                                remainingDebt = debt,
                                                notes = notes
                                            )
                                        )
                                    } else {
                                        viewModel.addDebtor(name, phone, debt, notes)
                                    }
                                    showAddDebtorDialog = false
                                    debtorToEdit = null
                                }
                            )
                        }

                        debtorForPayment?.let { debtor ->
                            RecordPaymentDialog(
                                debtorName = debtor.name,
                                remainingDebt = debtor.remainingDebt,
                                onDismiss = { debtorForPayment = null },
                                onConfirm = { amount, note ->
                                    viewModel.recordPayment(debtor.id, amount, note)
                                    debtorForPayment = null
                                }
                            )
                        }

                        if (showPdfOptionsDialog) {
                            PdfOptionsDialog(
                                onDismiss = { showPdfOptionsDialog = false },
                                onGenerate = { reportType ->
                                    val (rides, parcels, debtors, payments) = when (reportType) {
                                        "RIDES" -> Quadruple(uiState.rides, emptyList(), emptyList(), emptyList())
                                        "PARCELS" -> Quadruple(emptyList(), uiState.parcels, emptyList(), emptyList())
                                        "DEBTORS" -> Quadruple(emptyList(), emptyList(), uiState.debtors, uiState.payments)
                                        else -> Quadruple(uiState.rides, uiState.parcels, uiState.debtors, uiState.payments)
                                    }
                                    val file = PdfReportGenerator.generateFullReport(
                                        context = context,
                                        rides = rides,
                                        parcels = parcels,
                                        debtors = debtors,
                                        payments = payments,
                                        title = "Aqeel Rider - $reportType Report"
                                    )
                                    showPdfOptionsDialog = false
                                    ShareUtil.sharePdf(context, file)
                                }
                            )
                        }

                        AppUpdateDialog(
                            updateState = updateState,
                            onStartDownload = { url ->
                                scope.launch {
                                    updateManager.downloadApk(url)
                                }
                            },
                            onInstall = { file ->
                                updateManager.installApk(file)
                            },
                            onDismiss = {
                                updateManager.reset()
                            }
                        )
                    }
                }
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
