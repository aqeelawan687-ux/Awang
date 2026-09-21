package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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

                        // Phone/gesture back button: navigate within the app instead of
                        // exiting straight away. Sub-screens (Settings, Customer Account)
                        // go back to MAIN first; a non-Dashboard tab on MAIN goes back to
                        // Dashboard; only pressing back from the Dashboard tab exits the app
                        // (default system behaviour, so BackHandler is simply not enabled there).
                        BackHandler(enabled = currentScreen != "MAIN" || currentTab != "DASHBOARD") {
                            if (currentScreen != "MAIN") {
                                currentScreen = "MAIN"
                            } else {
                                currentTab = "DASHBOARD"
                            }
                        }

                        when (currentScreen) {
                            "SETTINGS" -> {
                                SettingsScreen(
                                    currentTheme = uiState.themeMode,
                                    onThemeChange = { viewModel.setThemeMode(it) },
                                    licenseState = licenseState,
                                    onCheckUpdate = {
                                        scope.launch {
                                            updateManager.checkLatestUpdate(isManual = true)
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
                                    onRecordPayment = { amount, note ->
                                        val customerId = uiState.selectedCustomerId
                                        if (customerId != null) {
                                            viewModel.recordPayment(customerId, amount, note)
                                        } else {
                                            viewModel.recordCustomerPayment(
                                                uiState.selectedCustomerName ?: "Customer",
                                                uiState.selectedCustomerPhone ?: "",
                                                amount,
                                                note
                                            )
                                        }
                                    },
                                    onUpdateBakaya = { newBakaya ->
                                        val customerId = uiState.selectedCustomerId
                                        if (customerId != null) {
                                            viewModel.updateCustomerBakayaById(customerId, newBakaya)
                                        } else {
                                            viewModel.updateCustomerBakaya(
                                                uiState.selectedCustomerName ?: "",
                                                uiState.selectedCustomerPhone ?: "",
                                                newBakaya
                                            )
                                        }
                                    },
                                    onDeleteBakaya = {
                                        val customerId = uiState.selectedCustomerId
                                        if (customerId != null) {
                                            viewModel.deleteCustomerBakayaById(customerId)
                                        } else {
                                            viewModel.deleteCustomerBakaya(
                                                uiState.selectedCustomerName ?: "",
                                                uiState.selectedCustomerPhone ?: ""
                                            )
                                        }
                                    },
                                    onDeleteHistoryItem = { item ->
                                        viewModel.deleteCustomerHistory(item)
                                    },
                                    onAddRideForCustomer = { name, phone, pickup, dropoff, fare, paid, notes, cId, rideDate ->
                                        viewModel.addRide(name, phone, pickup, dropoff, fare, paid, notes, cId ?: uiState.selectedCustomerId, rideDate)
                                    },
                                    onAddParcelForCustomer = { sName, sPhone, rName, rPhone, pickup, delivery, shopName, sCharges, dCharges, paid, isDelivered, notes, cId, pDate ->
                                        viewModel.addParcel(
                                            senderName = sName,
                                            senderPhone = sPhone,
                                            receiverName = rName,
                                            receiverPhone = rPhone,
                                            pickupAddress = pickup,
                                            deliveryAddress = delivery,
                                            deliveryCharges = dCharges,
                                            amountPaid = paid,
                                            isDelivered = isDelivered,
                                            notes = notes,
                                            customerId = cId ?: uiState.selectedCustomerId,
                                            shopName = shopName,
                                            samanCharges = sCharges,
                                            date = pDate
                                        )
                                    },
                                    onUpdateRide = { updatedRide ->
                                        viewModel.updateRide(updatedRide)
                                    },
                                    onUpdateParcel = { updatedParcel ->
                                        viewModel.updateParcel(updatedParcel)
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
                                                },
                                                onAddAccount = {
                                                    debtorToEdit = null
                                                    showAddDebtorDialog = true
                                                },
                                                onViewCustomerLedger = { debtor ->
                                                    viewModel.selectCustomer(debtor)
                                                    currentScreen = "CUSTOMER_ACCOUNT"
                                                },
                                                onRecordPayment = { debtor ->
                                                    debtorForPayment = debtor
                                                },
                                                onEditCustomer = { debtor ->
                                                    debtorToEdit = debtor
                                                    showAddDebtorDialog = true
                                                },
                                                onDeleteCustomer = { debtor ->
                                                    viewModel.deleteDebtor(debtor)
                                                },
                                                onWhatsAppCustomer = { debtor ->
                                                    val msg = "Assalam-o-Alaikum ${debtor.name},\nYour current balance with Aqeel Rider is Rs. ${debtor.remainingDebt.toInt()}.\nThank you!"
                                                    ShareUtil.shareViaWhatsApp(context, debtor.phone, msg)
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
                                                    val d = uiState.allDebtors.find { it.name == name && it.phone == phone }
                                                    if (d != null) {
                                                        viewModel.selectCustomer(d)
                                                    } else {
                                                        viewModel.selectCustomer(name, phone)
                                                    }
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
                                                    val d = uiState.allDebtors.find { it.name == name && it.phone == phone }
                                                    if (d != null) {
                                                        viewModel.selectCustomer(d)
                                                    } else {
                                                        viewModel.selectCustomer(name, phone)
                                                    }
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
                                                    val d = uiState.allDebtors.find { it.name == name && it.phone == phone }
                                                    if (d != null) {
                                                        viewModel.selectCustomer(d)
                                                    } else {
                                                        viewModel.selectCustomer(name, phone)
                                                    }
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
                                                    val d = uiState.allDebtors.find { it.name == name && it.phone == phone }
                                                    if (d != null) {
                                                        viewModel.selectCustomer(d)
                                                    } else {
                                                        viewModel.selectCustomer(name, phone)
                                                    }
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
                                existingCustomers = uiState.allDebtors,
                                onDismiss = {
                                    showAddRideDialog = false
                                    rideToEdit = null
                                },
                                onConfirm = { name, phone, pickup, dropoff, fare, paid, notes, customerId, rideDate ->
                                    if (rideToEdit != null) {
                                        val remaining = (fare - paid).coerceAtLeast(0.0)
                                        val paymentStatus = when {
                                            remaining <= 0 -> "PAID"
                                            paid > 0 -> "PARTIAL"
                                            else -> "UNPAID"
                                        }
                                        viewModel.updateRide(
                                            rideToEdit!!.copy(
                                                customerId = customerId ?: rideToEdit!!.customerId,
                                                customerName = name,
                                                phone = phone,
                                                pickupLocation = pickup,
                                                dropoffLocation = dropoff,
                                                fare = fare,
                                                paymentStatus = paymentStatus,
                                                amountPaid = paid,
                                                remainingBakaya = remaining,
                                                rideDate = rideDate,
                                                notes = notes
                                            )
                                        )
                                    } else {
                                        viewModel.addRide(name, phone, pickup, dropoff, fare, paid, notes, customerId, rideDate)
                                    }
                                    showAddRideDialog = false
                                    rideToEdit = null
                                }
                            )
                        }

                        if (showAddParcelDialog) {
                            AddEditParcelDialog(
                                parcelToEdit = parcelToEdit,
                                existingCustomers = uiState.allDebtors,
                                onDismiss = {
                                    showAddParcelDialog = false
                                    parcelToEdit = null
                                },
                                onConfirm = { sName, sPhone, rName, rPhone, pickup, delivery, shopName, sCharges, dCharges, paid, isDelivered, notes, customerId, date ->
                                    if (parcelToEdit != null) {
                                        val total = sCharges + dCharges
                                        val remaining = (total - paid).coerceAtLeast(0.0)
                                        viewModel.updateParcel(
                                            parcelToEdit!!.copy(
                                                customerId = customerId ?: parcelToEdit!!.customerId,
                                                senderName = sName,
                                                senderPhone = sPhone,
                                                receiverName = rName,
                                                receiverPhone = rPhone,
                                                pickupAddress = pickup,
                                                deliveryAddress = delivery,
                                                shopName = shopName,
                                                samanCharges = sCharges,
                                                deliveryCharges = dCharges,
                                                amountPaid = paid,
                                                remainingBakaya = remaining,
                                                isDelivered = isDelivered,
                                                isPaid = remaining <= 0,
                                                date = date,
                                                notes = notes
                                            )
                                        )
                                    } else {
                                        viewModel.addParcel(
                                            senderName = sName,
                                            senderPhone = sPhone,
                                            receiverName = rName,
                                            receiverPhone = rPhone,
                                            pickupAddress = pickup,
                                            deliveryAddress = delivery,
                                            deliveryCharges = dCharges,
                                            amountPaid = paid,
                                            isDelivered = isDelivered,
                                            notes = notes,
                                            customerId = customerId,
                                            shopName = shopName,
                                            samanCharges = sCharges,
                                            date = date
                                        )
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
                                onConfirm = { name, phone, debt, notes, address, photoUri ->
                                    if (debtorToEdit != null) {
                                        viewModel.updateDebtor(
                                            debtorToEdit!!.copy(
                                                name = name,
                                                phone = phone,
                                                address = address,
                                                location = address,
                                                photoUri = photoUri,
                                                remainingDebt = debt,
                                                notes = notes,
                                                lastUpdated = System.currentTimeMillis()
                                            )
                                        )
                                    } else {
                                        viewModel.addCustomer(
                                            name = name,
                                            phone = phone,
                                            address = address,
                                            location = address,
                                            photoUri = photoUri,
                                            initialBalance = debt,
                                            notes = notes
                                        )
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
                                    val rides = if (reportType == "PARCELS" || reportType == "DEBTORS") emptyList() else uiState.allRides
                                    val parcels = if (reportType == "RIDES" || reportType == "DEBTORS") emptyList() else uiState.allParcels
                                    val debtors = if (reportType == "RIDES" || reportType == "PARCELS") emptyList() else uiState.allDebtors
                                    val payments = if (reportType == "RIDES" || reportType == "PARCELS") emptyList() else uiState.payments
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
