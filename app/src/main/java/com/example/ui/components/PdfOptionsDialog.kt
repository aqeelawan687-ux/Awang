package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneralPdfOptions(
    val customTitle: String = "AQEEL RIDER / عقیل رائڈر",
    val customSubtitle: String = "",
    val customNote: String = "",
    val hideSamanTotal: Boolean = false,
    val hideRideCharges: Boolean = false,
    val includeRides: Boolean = true,
    val includeParcels: Boolean = true,
    val includeDebtors: Boolean = true
)

@Composable
fun GeneralPdfOptionsDialog(
    selectedDateMillis: Long,
    rides: List<RideEntity>,
    parcels: List<ParcelEntity>,
    debtors: List<DebtorEntity>,
    onDismiss: () -> Unit,
    onConfirm: (options: GeneralPdfOptions) -> Unit
) {
    val dateStr = remember(selectedDateMillis) {
        SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(selectedDateMillis))
    }

    var customTitle by remember { mutableStateOf("AQEEL RIDER / عقیل رائڈر") }
    var customSubtitle by remember { mutableStateOf("Daily Ledger & Rider Summary Report | Date: $dateStr") }
    var customNote by remember { mutableStateOf("") }

    var hideSamanTotal by remember { mutableStateOf(false) }
    var hideRideCharges by remember { mutableStateOf(false) }

    var includeRides by remember { mutableStateOf(true) }
    var includeParcels by remember { mutableStateOf(true) }
    var includeDebtors by remember { mutableStateOf(true) }

    var isEditModeOpen by remember { mutableStateOf(false) }
    var isPreviewOpen by remember { mutableStateOf(true) }

    // Calculated metrics for live preview
    val totalRidesCount = if (includeRides) rides.size else 0
    val totalKm = if (includeRides) rides.sumOf { it.distanceKm } else 0.0
    val ridesEarning = if (includeRides) rides.sumOf { it.fareAmount } else 0.0
    val parcelEarnings = if (includeParcels) parcels.filter { it.isPaid }.sumOf { it.deliveryCharges } else 0.0
    val totalDebtOutstanding = if (includeDebtors) debtors.filter { it.totalDebt > 0 }.sumOf { it.totalDebt } else 0.0

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("general_pdf_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(scrollState)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PDF Options & Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TOGGLES SECTION: Hide Saman Total & Hide Ride Charges
                Text(
                    text = "Privacy & Amount Settings / پردہ اور رقم کی ترتیبات",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 1. Hide Saman Total Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hideSamanTotal) AccentAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Saman Total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Saman/Parcel ki kul raqam PDF me chupayein",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = hideSamanTotal,
                            onCheckedChange = { hideSamanTotal = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary),
                            modifier = Modifier.testTag("toggle_hide_saman_total")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Hide Ride Charges Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hideRideCharges) AccentAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Ride Charges",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ride / Kiraya charges PDF me chupayein",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = hideRideCharges,
                            onCheckedChange = { hideRideCharges = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary),
                            modifier = Modifier.testTag("toggle_hide_ride_charges")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. EDIT PDF TOGGLE BUTTON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Report Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { isEditModeOpen = !isEditModeOpen },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_toggle_edit_pdf")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditModeOpen) "Close Edit" else "Edit PDF", fontSize = 11.sp)
                    }
                }

                // Expandable Edit Fields
                AnimatedVisibility(visible = isEditModeOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "PDF-Only Edits (Does not alter original database):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Report Title / عنوان") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customSubtitle,
                            onValueChange = { customSubtitle = it },
                            label = { Text("Subtitle / تاریخ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Custom Footer Note (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Include Sections:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeRides, onCheckedChange = { includeRides = it })
                            Text("Rides Log", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Checkbox(checked = includeParcels, onCheckedChange = { includeParcels = it })
                            Text("Parcels", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Checkbox(checked = includeDebtors, onCheckedChange = { includeDebtors = it })
                            Text("Debts", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. PREVIEW PDF SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PDF Preview / خلاصہ پیش نظارہ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { isPreviewOpen = !isPreviewOpen },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewOpen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview",
                            tint = Color.Gray
                        )
                    }
                }

                if (isPreviewOpen) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MintContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = customTitle.ifBlank { "AQEEL RIDER" },
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = customSubtitle.ifBlank { "Date: $dateStr" },
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray)

                            // Preview Statistics
                            Text(
                                text = "• Total Rides: $totalRidesCount | Distance: ${String.format("%.1f", totalKm)} KM",
                                fontSize = 11.sp
                            )

                            val fareDisplay = if (hideRideCharges) "[Charges Hidden]" else "Rs. ${ridesEarning.toInt()}"
                            Text(
                                text = "• Rides Earning: $fareDisplay",
                                fontSize = 11.sp,
                                fontWeight = if (hideRideCharges) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (hideRideCharges) Color.Gray else AccentBlue
                            )

                            val samanDisplay = if (hideSamanTotal) "[Total Hidden]" else "Rs. ${parcelEarnings.toInt()}"
                            Text(
                                text = "• Saman Deliveries: ${parcels.size} (Earnings: $samanDisplay)",
                                fontSize = 11.sp,
                                fontWeight = if (hideSamanTotal) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (hideSamanTotal) Color.Gray else AccentAmber
                            )

                            Text(
                                text = "• Pending Debts: Rs. ${totalDebtOutstanding.toInt()}",
                                fontSize = 11.sp,
                                color = AccentRed
                            )

                            if (customNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Note: $customNote",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. SHARE PDF ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val options = GeneralPdfOptions(
                                customTitle = customTitle.trim(),
                                customSubtitle = customSubtitle.trim(),
                                customNote = customNote.trim(),
                                hideSamanTotal = hideSamanTotal,
                                hideRideCharges = hideRideCharges,
                                includeRides = includeRides,
                                includeParcels = includeParcels,
                                includeDebtors = includeDebtors
                            )
                            onConfirm(options)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_share_pdf_final")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF / شیئر کریں", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
