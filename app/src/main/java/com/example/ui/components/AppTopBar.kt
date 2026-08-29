package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreenPrimary

/**
 * Shared consistent Three-Dot Menu (⋮) for every screen across the app.
 * Provides direct access to Central Settings, Customer History, and Privacy controls.
 */
@Composable
fun AppHeaderDropdownMenu(
    onOpenSettings: () -> Unit,
    onOpenCustomerHistory: (() -> Unit)? = null,
    isBalanceHidden: Boolean = false,
    onToggleBalanceVisibility: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("btn_header_three_dots_menu")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Menu / سیٹنگز",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 1. Settings Option (Mandatory on all screens)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Settings / سیٹنگز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Themes, PDF & App options",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
                modifier = Modifier.testTag("menu_item_settings")
            )

            // 2. Customer History Option (if available)
            if (onOpenCustomerHistory != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Customer History / کسٹمر ہسٹری",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Rides, Parcels & Logs",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onOpenCustomerHistory()
                    },
                    modifier = Modifier.testTag("menu_item_history")
                )
            }

            // 3. Hide / Show Amounts Toggle (if available)
            if (onToggleBalanceVisibility != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isBalanceHidden) "Show Amounts / رقم دکھائیں" else "Hide Amounts / رقم چھپائیں",
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onToggleBalanceVisibility()
                    },
                    modifier = Modifier.testTag("menu_item_toggle_balance")
                )
            }
        }
    }
}

/**
 * Shared App Top Bar with Title, Subtitle, Optional Back Arrow, Screen Actions,
 * and the consistent Three-Dot Menu (⋮).
 */
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onOpenCustomerHistory: (() -> Unit)? = null,
    isBalanceHidden: Boolean = false,
    onToggleBalanceVisibility: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("btn_top_bar_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actions != null) {
                actions()
            }

            // Consistent Three-Dot Menu (⋮) on every page
            AppHeaderDropdownMenu(
                onOpenSettings = onOpenSettings,
                onOpenCustomerHistory = onOpenCustomerHistory,
                isBalanceHidden = isBalanceHidden,
                onToggleBalanceVisibility = onToggleBalanceVisibility
            )
        }
    }
}
