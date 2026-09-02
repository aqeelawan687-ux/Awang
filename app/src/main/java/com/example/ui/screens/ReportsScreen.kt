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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil

@Composable
fun ReportsScreen(
    state: RiderUiState,
    onOpenPdfOptions: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (PDF Export & Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenPdfOptions,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val file = PdfReportGenerator.generateFullReport(
                            context = context,
                            rides = state.rides,
                            parcels = state.parcels,
                            debtors = state.debtors,
                            payments = state.payments
                        )
                        ShareUtil.sharePdf(context, file)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_summary_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Full", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Net Cash Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Net Cash Balance",
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
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Ride Collections:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rs. ${state.totalRidePaid.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Parcel Collections:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rs. ${state.totalParcelPaid.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Debt Recoveries:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rs. ${state.totalRecoveredCash.toInt()}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Operational Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Ride Gross Total",
                    value = "Rs. ${state.totalRideFare.toInt()}",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = GreenPrimary,
                    modifier = Modifier.weight(1f),
                    subtitle = "Bakaya: Rs. ${state.totalRideBakaya.toInt()}"
                )
                StatBox(
                    title = "Parcel Gross Total",
                    value = "Rs. ${state.totalParcelCharges.toInt()}",
                    icon = Icons.Default.Inventory2,
                    accentColor = BluePrimary,
                    modifier = Modifier.weight(1f),
                    subtitle = "Bakaya: Rs. ${state.totalParcelBakaya.toInt()}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Outstanding Debt",
                    value = "Rs. ${state.totalRemainingDebt.toInt()}",
                    icon = Icons.Default.MoneyOff,
                    accentColor = RedError,
                    modifier = Modifier.weight(1f),
                    subtitle = "${state.totalDebtorsCount} customers"
                )
                StatBox(
                    title = "Total Recoveries",
                    value = "Rs. ${state.totalRecoveredCash.toInt()}",
                    icon = Icons.Default.Payments,
                    accentColor = PurpleAccent,
                    modifier = Modifier.weight(1f),
                    subtitle = "${state.payments.size} payments logged"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
