package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel
import com.example.util.ApkUpdateManager
import com.example.util.UpdateState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onBackClick: () -> Unit,
    onOpenCustomerHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val updateState by ApkUpdateManager.updateState.collectAsStateWithLifecycle()

    if (updateState !is UpdateState.Idle) {
        AppUpdateDialog(
            updateState = updateState,
            onDismiss = { ApkUpdateManager.resetState() }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings / سیٹنگز",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Theme, PDF Reports & Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. THEME SETTINGS (ڈسپلے اور تھیم)
            // ==========================================
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "Appearance & Themes / ڈسپلے اور تھیم",
                subtitle = "Select app colors or High-Contrast Night Vision"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_settings_theme"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Night Vision Featured Card
                    val isNightVision = state.themeMode == AppThemeMode.NIGHT_VISION
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNightVision) Color.Black else Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isNightVision) 2.dp else 1.dp,
                                color = if (isNightVision) Color(0xFF00FF66) else Color.DarkGray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.setThemeMode(AppThemeMode.NIGHT_VISION)
                                Toast.makeText(context, "🌙 Night Vision Theme Activated!", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("theme_option_night_vision")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF66)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nightlight,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Night Vision (نائٹ ویژن)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF00FF66).copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "OLED 🌙",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00FF66),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "High Contrast Deep Black for Night Riding",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            if (isNightVision) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF00FF66),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Standard Themes (دیگر تھیمز):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // FlowRow of color themes
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.filter { it != AppThemeMode.NIGHT_VISION }.forEach { mode ->
                            val isSelected = state.themeMode == mode
                            val themeColor = Color(mode.primaryColorLong)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, themeColor)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                },
                                modifier = Modifier
                                    .clickable {
                                        viewModel.setThemeMode(mode)
                                        Toast.makeText(context, "${mode.titleEnglish} Theme Selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("theme_option_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(themeColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = mode.titleEnglish,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. PDF REPORT SETTINGS (پی ڈی ایف رپورٹ ترتیبات)
            // ==========================================
            SettingsSectionHeader(
                icon = Icons.Default.PictureAsPdf,
                title = "PDF Report Settings / پی ڈی ایف رپورٹ ترتیبات",
                subtitle = "Configure defaults for daily and customer payment PDF reports"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_settings_pdf"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Show Total Payment Only Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Show Total Payment Only",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (state.isPdfTotalOnly) EmeraldGreenPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (state.isPdfTotalOnly) "صرف کل رقم" else "تفصیلات",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isPdfTotalOnly) EmeraldGreenPrimary else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "PDF رپورٹ میں آئٹم قیمت اور ڈیلیوری فیس چھپائیں اور صرف کل رقم / بقایا دکھائیں۔",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = state.isPdfTotalOnly,
                            onCheckedChange = { isChecked ->
                                viewModel.togglePdfTotalOnly(isChecked)
                                val msg = if (isChecked) {
                                    "PDF: Show Total Payment Only Enabled"
                                } else {
                                    "PDF: Full Itemized Breakdown Enabled"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreenPrimary
                            ),
                            modifier = Modifier.testTag("switch_pdf_total_only")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Notice box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreenPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreenPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = EmeraldGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isPdfTotalOnly) {
                                    "PDF Mode: Total Payment Only is ON. Reports will display Grand Total and Net Bakaya without itemized price breakdown."
                                } else {
                                    "PDF Mode: Full Detailed Breakdown is ON. Reports will display line-by-line item prices and delivery charges."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. PRIVACY & SECURITY (پرائیویسی اور بقایا جات)
            // ==========================================
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = "Privacy & Display / پرائیویسی اور سکرین",
                subtitle = "Protect sensitive customer balances on home screen"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_settings_privacy"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Amounts on Screen / رقم چھپائیں",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "تمام سکرینوں پر رقم ماسک کر کے 'Rs. ****' میں دکھائیں۔",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = state.isBalanceHidden,
                            onCheckedChange = {
                                viewModel.toggleBalanceVisibility()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreenPrimary
                            ),
                            modifier = Modifier.testTag("switch_hide_balance")
                        )
                    }
                }
            }

            // ==========================================
            // 4. DATA & CUSTOMER HISTORY (کسٹمر ریکارڈ اور ڈیٹا)
            // ==========================================
            SettingsSectionHeader(
                icon = Icons.Default.History,
                title = "Customer Records & History / کسٹمر ریکارڈ",
                subtitle = "Access comprehensive action logs and transaction history"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCustomerHistory() }
                    .testTag("card_settings_customer_history"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Customer History Logs (کسٹمر ہسٹری)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Total ${state.customerHistory.size} recorded action logs",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ==========================================
            // 5. APP UPDATES & INFO (ایپ اپڈیٹ اور معلومات)
            // ==========================================
            SettingsSectionHeader(
                icon = Icons.Default.SystemUpdate,
                title = "App Updates & Version / ایپ اپڈیٹ",
                subtitle = "Official releases and automatic APK synchronization"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_settings_updates"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Aqeel Rider (Awang)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreenPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Release Ready",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                ApkUpdateManager.checkLatestUpdate(context, isManualCheck = true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_check_update")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates / اپڈیٹ چیک کریں")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
