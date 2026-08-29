package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeaderDropdownMenu
import com.example.ui.components.GeneralPdfOptionsDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftRedBg
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onOpenCustomerHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showPdfOptionsDialog by remember { mutableStateOf(false) }

    val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(state.selectedReportDateMillis))
    val fileNameExpected = "AqeelRider_Report_$dateStr.pdf"

    // Calculate metrics
    val totalRidesCount = state.rides.size
    val totalKm = state.rides.sumOf { it.distanceKm }
    val ridesEarning = state.rides.sumOf { it.fareAmount }
    val parcelEarnings = state.parcels.filter { it.isPaid }.sumOf { it.deliveryCharges }
    val totalEarning = ridesEarning
    val activeDebtsCount = state.debtors.count { it.totalDebt > 0 }

    // Date Picker Dialog Launcher
    val calendar = Calendar.getInstance().apply { timeInMillis = state.selectedReportDateMillis }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            viewModel.setSelectedReportDate(cal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .testTag("reports_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Report & PDF Generator / رپورٹس",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "PDF file banayein aur WhatsApp par share karein",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            AppHeaderDropdownMenu(
                onOpenSettings = onOpenSettings,
                onOpenCustomerHistory = onOpenCustomerHistory,
                isBalanceHidden = state.isBalanceHidden,
                onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Report Date / تاریخ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Date")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Preview Document Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MintContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = fileNameExpected,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                        Text(
                            text = "PDF document ready to export",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PDF Contents Summary Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📋 PDF Report Overview:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = EmeraldGreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Total Rides: $totalRidesCount | Distance: ${String.format("%.1f", totalKm)} KM",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val parcelEarStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${parcelEarnings.toInt()}"
                        Text(
                            text = "Saman Deliveries: ${state.parcels.size} (Collected: $parcelEarStr)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val qarzaStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${state.totalQarza.toInt()}"
                        Text(
                            text = "Customer Payment: $activeDebtsCount active customers ($qarzaStr)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val totalEarStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${totalEarning.toInt()}"
                        Text(
                            text = "Kul Rider Earning (Rides): $totalEarStr",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Customer-wise Earning Breakdown Section
                if (state.debtors.isNotEmpty()) {
                    Text(
                        text = "Customer Earning Breakdown / ہر کسٹمر کی آمدن:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.debtors.forEach { debtor ->
                            val name = debtor.name.trim().lowercase()
                            val cRides = state.rides.filter { r ->
                                r.note.lowercase().contains("customer: $name") || r.note.lowercase().contains(name)
                            }
                            val cEarn = cRides.sumOf { it.fareAmount }
                            val cEarnStr = if (state.isBalanceHidden) "Rs. ****" else "Rs. ${cEarn.toInt()}"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = debtor.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${cRides.size} rides completed",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = cEarnStr,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreenPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // PDF Action Buttons: Generate PDF & WhatsApp Share
                Button(
                    onClick = { showPdfOptionsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Report Nikalein / PDF ڈاؤن لوڈ کریں",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.shareSummaryText(context) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("whatsapp_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp Share Option / واٹس ایپ شیئر",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showPdfOptionsDialog) {
        GeneralPdfOptionsDialog(
            selectedDateMillis = state.selectedReportDateMillis,
            rides = state.rides,
            parcels = state.parcels,
            debtors = state.debtors,
            onDismiss = { showPdfOptionsDialog = false },
            onConfirm = { options ->
                showPdfOptionsDialog = false
                viewModel.generateAndSharePdfReport(
                    context = context,
                    hideSamanTotal = options.hideSamanTotal,
                    hideRideCharges = options.hideRideCharges,
                    customTitle = options.customTitle,
                    customSubtitle = options.customSubtitle,
                    customNote = options.customNote,
                    includeRides = options.includeRides,
                    includeParcels = options.includeParcels,
                    includeDebtors = options.includeDebtors
                )
            }
        )
    }
}
