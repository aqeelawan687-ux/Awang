package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ActionShortcutButton
import com.example.ui.components.DebtCard
import com.example.ui.components.RideCard
import com.example.ui.components.StatBox
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftRedBg
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CalendarToday
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.AppHeaderDropdownMenu
import com.example.ui.components.SettingsDialog

@Composable
fun DashboardScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onNavigateToDebts: () -> Unit,
    onOpenAddParcel: () -> Unit,
    onOpenAddRide: () -> Unit,
    onOpenAddDebtor: () -> Unit,
    onOpenCustomerHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen")
    ) {
        // App Header Banner
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aqeel_rider_logo),
                            contentDescription = "Aqeel Rider Logo",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aqeel Rider",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Smart Delivery & Debt Ledger",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hide/Show Balance Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleBalanceVisibility() },
                        modifier = Modifier.testTag("balance_hide_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Hide/Show Balance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Dark Mode Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Three-dot Dropdown Menu with Settings & History
                    AppHeaderDropdownMenu(
                        onOpenSettings = onOpenSettings,
                        onOpenCustomerHistory = onOpenCustomerHistory,
                        isBalanceHidden = state.isBalanceHidden,
                        onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 7-Day Overdue Debts Notification Alert Banner
        if (state.overdueDebtors.isNotEmpty()) {
            item {
                val overdue = state.overdueDebtors.first()
                val overdueDebtStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${overdue.totalDebt.toInt()}"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onNavigateToDebts() },
                    colors = CardDefaults.cardColors(containerColor = SoftRedBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hisab Reminder Alert ⚠️",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                            Text(
                                text = "${overdue.name} se $overdueDebtStr lene hain (7 din se zyada ho gaye)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                        IconButton(onClick = { viewModel.sendDebtorReminder(context, overdue) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Send WhatsApp",
                                tint = EmeraldGreenPrimary
                            )
                        }
                    }
                }
            }
        }

        // 1. DASHBOARD FEATURE: 3 BOX CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Box 1: Customer Payment
                StatBox(
                    titleUrdu = "Customer Payment / کسٹمر پیمنٹ",
                    titleEnglish = "Total Customer Payment Pending",
                    value = "Rs. ${state.totalQarza.toInt()}",
                    icon = Icons.Default.AccountBalanceWallet,
                    containerColor = SoftRedBg,
                    contentColor = AccentRed,
                    isBalanceHidden = state.isBalanceHidden,
                    onClick = onNavigateToDebts
                )

                // Box 2: Saman ke Paise
                StatBox(
                    titleUrdu = "Saman ke Paise / سامان کے پیسے",
                    titleEnglish = "Parcel Payment Collected",
                    value = "Rs. ${state.samanKePaise.toInt()}",
                    icon = Icons.Default.ShoppingBag,
                    containerColor = MintContainer,
                    contentColor = EmeraldGreenPrimary,
                    isBalanceHidden = state.isBalanceHidden
                )

                // Box 3: Is Mahine ki Earning
                StatBox(
                    titleUrdu = "Is Mahine ki Earning / اس مہینے کی آمدن",
                    titleEnglish = "This Month's Earnings (Rides + Parcel)",
                    value = "Rs. ${state.isMahineKiEarning.toInt()}",
                    icon = Icons.Default.TrendingUp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = AccentBlue,
                    isBalanceHidden = state.isBalanceHidden
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. DASHBOARD FEATURE: 3 SHORTCUT BUTTONS
        item {
            Text(
                text = "Quick Actions / فوراً کام کریں",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Logon ka Hisab
                ActionShortcutButton(
                    titleUrdu = "Logon ka Hisab",
                    titleEnglish = "Debts Page",
                    icon = Icons.Default.People,
                    buttonColor = AccentRed,
                    onClick = onNavigateToDebts,
                    modifier = Modifier.weight(1f),
                    testTag = "shortcut_debts_button"
                )

                // Button 2: Saman Add Karein
                ActionShortcutButton(
                    titleUrdu = "Saman Add",
                    titleEnglish = "Add Parcel",
                    icon = Icons.Default.ShoppingBag,
                    buttonColor = AccentAmber,
                    onClick = onOpenAddParcel,
                    modifier = Modifier.weight(1f),
                    testTag = "shortcut_add_parcel_button"
                )

                // Button 3: Nayi Ride Add
                ActionShortcutButton(
                    titleUrdu = "Nayi Ride Add",
                    titleEnglish = "Add Ride",
                    icon = Icons.Default.DirectionsBike,
                    buttonColor = EmeraldGreenPrimary,
                    onClick = onOpenAddRide,
                    modifier = Modifier.weight(1f),
                    testTag = "shortcut_add_ride_button"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // DATE-WISE TOTAL EARNING SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Earning by Date / تاریخ وار کل آمدن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Har Date Ki Alag",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val sdfDateKey = SimpleDateFormat("dd MMM yyyy", Locale.US)
                val earningsByDate = remember(state.rides) {
                    state.rides
                        .groupBy { sdfDateKey.format(Date(it.dateMillis)) }
                        .mapValues { entry ->
                            Pair(entry.value.size, entry.value.sumOf { it.fareAmount })
                        }
                }

                if (earningsByDate.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Abhi tak kisi date ki ride earning nahi hui",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        earningsByDate.forEach { (dateStr, rideData) ->
                            val (rideCount, totalFare) = rideData
                            val fareStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${totalFare.toInt()}"
                            val isToday = dateStr == sdfDateKey.format(Date())

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isToday) MintContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = if (isToday) EmeraldGreenPrimary else AccentBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isToday) "Aaj ($dateStr)" else dateStr,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$rideCount rides completed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Text(
                                        text = fareStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isToday) EmeraldGreenPrimary else AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Rides Section Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aakhri Rides / حالیہ رائڈز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rides: ${state.rides.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.rides.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Abhi tak koi ride add nahi hui",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(state.rides.take(3)) { ride ->
                RideCard(
                    ride = ride,
                    onDeleteClick = { viewModel.deleteRide(ride) },
                    modifier = Modifier.padding(vertical = 4.dp),
                    isBalanceHidden = state.isBalanceHidden
                )
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            themeMode = state.themeMode,
            isBalanceHidden = state.isBalanceHidden,
            onSelectThemeMode = { viewModel.setThemeMode(it) },
            onToggleHideBalance = { viewModel.toggleBalanceVisibility() },
            onOpenCustomerHistory = onOpenCustomerHistory,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

