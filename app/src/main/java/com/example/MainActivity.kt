package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddDebtorDialog
import com.example.ui.components.AddParcelDialog
import com.example.ui.components.AddRideDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.CustomerHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.ParcelsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RidesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AqeelRiderTheme
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.RiderViewModel
import com.example.util.ApkUpdateManager
import com.example.util.UpdateState

enum class NavigationTab(
    val titleUrdu: String,
    val titleEnglish: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Home", "Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    DEBTS("Customer Payment", "Customer Payment", Icons.Default.AccountBalanceWallet, "tab_debts"),
    PARCELS("Saman", "Parcels", Icons.Default.ShoppingBag, "tab_parcels"),
    RIDES("Ride", "Rides", Icons.AutoMirrored.Filled.DirectionsBike, "tab_rides"),
    REPORTS("Reports", "PDF Report", Icons.Default.Description, "tab_reports")
}

class MainActivity : ComponentActivity() {

    private val viewModel: RiderViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AqeelRiderTheme(themeMode = state.themeMode) {
                RiderApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RiderApp(viewModel: RiderViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by ApkUpdateManager.updateState.collectAsStateWithLifecycle()
    var selectedTabItem by remember { mutableIntStateOf(0) }
    var showCustomerHistoryScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    var showAddDebtorDialog by remember { mutableStateOf(false) }
    var showAddParcelDialog by remember { mutableStateOf(false) }
    var showAddRideDialog by remember { mutableStateOf(false) }

    // Check for updates on startup (throttled to every 6 hours)
    LaunchedEffect(Unit) {
        ApkUpdateManager.checkLatestUpdate(context, isManualCheck = false)
    }

    if (showSettingsScreen) {
        SettingsScreen(
            state = state,
            viewModel = viewModel,
            onBackClick = { showSettingsScreen = false },
            onOpenCustomerHistory = {
                showSettingsScreen = false
                showCustomerHistoryScreen = true
            }
        )
        return
    }

    if (showCustomerHistoryScreen) {
        CustomerHistoryScreen(
            state = state,
            viewModel = viewModel,
            onBackClick = { showCustomerHistoryScreen = false },
            onOpenSettings = {
                showCustomerHistoryScreen = false
                showSettingsScreen = true
            }
        )
        return
    }

    val tabs = NavigationTab.entries.toTypedArray()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabItem == index,
                        onClick = { selectedTabItem = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.titleEnglish) },
                        label = { Text(text = tab.titleUrdu, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldGreenPrimary,
                            selectedTextColor = EmeraldGreenPrimary,
                            indicatorColor = EmeraldGreenPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (selectedTabItem) {
            0 -> DashboardScreen(
                state = state,
                viewModel = viewModel,
                onNavigateToDebts = { selectedTabItem = 1 },
                onOpenAddParcel = { showAddParcelDialog = true },
                onOpenAddRide = { showAddRideDialog = true },
                onOpenAddDebtor = { showAddDebtorDialog = true },
                onOpenCustomerHistory = { showCustomerHistoryScreen = true },
                onOpenSettings = { showSettingsScreen = true },
                modifier = modifier
            )
            1 -> DebtsScreen(
                state = state,
                viewModel = viewModel,
                onOpenAddDebtor = { showAddDebtorDialog = true },
                onOpenCustomerHistory = { showCustomerHistoryScreen = true },
                onOpenSettings = { showSettingsScreen = true },
                modifier = modifier
            )
            2 -> ParcelsScreen(
                state = state,
                viewModel = viewModel,
                onOpenAddParcel = { showAddParcelDialog = true },
                onOpenCustomerHistory = { showCustomerHistoryScreen = true },
                onOpenSettings = { showSettingsScreen = true },
                modifier = modifier
            )
            3 -> RidesScreen(
                state = state,
                viewModel = viewModel,
                onOpenAddRide = { showAddRideDialog = true },
                onOpenCustomerHistory = { showCustomerHistoryScreen = true },
                onOpenSettings = { showSettingsScreen = true },
                modifier = modifier
            )
            4 -> ReportsScreen(
                state = state,
                viewModel = viewModel,
                onOpenCustomerHistory = { showCustomerHistoryScreen = true },
                onOpenSettings = { showSettingsScreen = true },
                modifier = modifier
            )
        }
    }

    // Global Add Dialogs
    if (showAddDebtorDialog) {
        AddDebtorDialog(
            onDismiss = { showAddDebtorDialog = false },
            onConfirm = { name, phone, debt, note ->
                viewModel.addDebtor(name, phone, debt, note)
                showAddDebtorDialog = false
            }
        )
    }

    if (showAddParcelDialog) {
        AddParcelDialog(
            onDismiss = { showAddParcelDialog = false },
            onConfirm = { shop, recipient, address, details, price, delivery, samanName, imageUri ->
                viewModel.addParcel(shop, recipient, address, details, price, delivery, samanName, imageUri)
                showAddParcelDialog = false
            }
        )
    }

    if (showAddRideDialog) {
        AddRideDialog(
            onDismiss = { showAddRideDialog = false },
            onConfirm = { from, to, km, fare, time, note, customer, imageUri ->
                viewModel.addRide(from, to, km, fare, time, note, customer, imageUri)
                showAddRideDialog = false
            }
        )
    }

    // Auto-Update Dialog (only shown if not dismissed for this session)
    if (!ApkUpdateManager.isDismissedForSession &&
        (updateState is UpdateState.UpdateAvailable ||
         updateState is UpdateState.Downloading ||
         updateState is UpdateState.Downloaded ||
         updateState is UpdateState.Error)
    ) {
        AppUpdateDialog(
            updateState = updateState,
            onDismiss = {
                ApkUpdateManager.resetState()
            }
        )
    }
}
