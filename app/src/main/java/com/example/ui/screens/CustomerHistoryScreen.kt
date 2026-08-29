package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.data.entity.CustomerHistoryEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftRedBg
import com.example.ui.viewmodel.RiderUiState
import com.example.ui.viewmodel.RiderViewModel
import com.example.ui.components.FullScreenImageViewerDialog
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ZoomIn
import coil.compose.AsyncImage
import com.example.ui.components.AppHeaderDropdownMenu
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryScreen(
    state: RiderUiState,
    viewModel: RiderViewModel,
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var historyToDelete by remember { mutableStateOf<CustomerHistoryEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("") }

    if (previewImageUri != null) {
        FullScreenImageViewerDialog(
            imageUri = previewImageUri!!,
            title = previewImageTitle.ifBlank { "Saman Photo" },
            onDismiss = { previewImageUri = null }
        )
    }

    val rawHistory = state.customerHistory
    val filteredHistory = if (searchQuery.isBlank()) {
        rawHistory
    } else {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        rawHistory.filter {
            it.customerName.lowercase(Locale.getDefault()).contains(q) ||
            it.phoneNumber.contains(q) ||
            it.title.lowercase(Locale.getDefault()).contains(q) ||
            it.details.lowercase(Locale.getDefault()).contains(q) ||
            it.actionType.lowercase(Locale.getDefault()).contains(q)
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isSelectionMode) {
                            Text(
                                text = "Selected (${selectedIds.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Select items to delete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Customer History / کسٹمر ہسٹری",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${rawHistory.size} Records Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("btn_back_history")
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSelectionMode) "Cancel Selection" else "Back",
                            tint = EmeraldGreenPrimary
                        )
                    }
                },
                actions = {
                    if (rawHistory.isNotEmpty()) {
                        if (isSelectionMode) {
                            IconButton(
                                onClick = {
                                    if (selectedIds.size == filteredHistory.size) {
                                        selectedIds = emptySet()
                                    } else {
                                        selectedIds = filteredHistory.map { it.id }.toSet()
                                    }
                                },
                                modifier = Modifier.testTag("btn_select_all")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (selectedIds.isNotEmpty()) {
                                IconButton(
                                    onClick = { showDeleteSelectedDialog = true },
                                    modifier = Modifier.testTag("btn_delete_selected_history")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = AccentRed
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { isSelectionMode = true },
                                modifier = Modifier.testTag("btn_enter_selection_mode")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select Multiple",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showDeleteAllDialog = true },
                                modifier = Modifier.testTag("btn_delete_all_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Delete All History",
                                    tint = AccentRed
                                )
                            }
                        }
                    }

                    if (!isSelectionMode) {
                        AppHeaderDropdownMenu(
                            onOpenSettings = onOpenSettings,
                            onOpenCustomerHistory = { /* already here */ },
                            isBalanceHidden = state.isBalanceHidden,
                            onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("screen_customer_history")
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("input_search_history"),
                placeholder = { Text("Search by customer name, number or action...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = EmeraldGreenPrimary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Top Header / Delete Actions Banner
            if (rawHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSelectionMode) "Select records to remove" else "Recent Activity (${filteredHistory.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row {
                        if (isSelectionMode) {
                            if (selectedIds.isNotEmpty()) {
                                TextButton(
                                    onClick = { showDeleteSelectedDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AccentRed),
                                    modifier = Modifier.testTag("btn_delete_selected_text")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete (${selectedIds.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { showDeleteAllDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentRed),
                                modifier = Modifier.testTag("btn_delete_all_text")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete All (سب ختم کریں)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Customer History Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customer activities (Payment, Rides, Parcels, Accounts) will automatically appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        CustomerHistoryItemCard(
                            item = item,
                            dateStr = dateFormat.format(Date(item.timestamp)),
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedIds = if (isSelected) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            },
                            onImageClick = { uri, title ->
                                previewImageUri = uri
                                previewImageTitle = title
                            },
                            onDelete = { historyToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Individual History Delete
    historyToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { historyToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AccentRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete History Record / ریکارڈ حذف کریں",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete this history?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${item.customerName} • ${item.actionType}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (item.amount > 0) {
                                Text(
                                    text = "Amount: Rs. ${item.amount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldGreenPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomerHistory(item)
                        historyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("btn_confirm_delete_history")
                ) {
                    Text("Delete / حذف کریں", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { historyToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_history")
                ) {
                    Text("Cancel / منسوخ")
                }
            }
        )
    }

    // Confirmation Dialog for Batch / Multiple Selected History Delete
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AccentRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete ${selectedIds.size} Selected Records?",
                        fontWeight = FontWeight.Bold,
                        color = AccentRed,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${selectedIds.size} selected customer history records?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomerHistoryBatch(selectedIds.toList())
                        showDeleteSelectedDialog = false
                        isSelectionMode = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("btn_confirm_delete_selected_history")
                ) {
                    Text("Delete Selected / منتخب شدہ حذف کریں", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteSelectedDialog = false },
                    modifier = Modifier.testTag("btn_cancel_delete_selected_history")
                ) {
                    Text("Cancel / منسوخ")
                }
            }
        )
    }

    // Confirmation Dialog for Delete All History
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = AccentRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete All Customer History?",
                        fontWeight = FontWeight.Bold,
                        color = AccentRed,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all customer history? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllCustomerHistory()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("btn_confirm_delete_all_history")
                ) {
                    Text("Delete All / تمام حذف کریں", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllDialog = false },
                    modifier = Modifier.testTag("btn_cancel_delete_all_history")
                ) {
                    Text("Cancel / منسوخ")
                }
            }
        )
    }
}

@Composable
fun CustomerHistoryItemCard(
    item: CustomerHistoryEntity,
    dateStr: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onImageClick: (String, String) -> Unit = { _, _ -> },
    onDelete: () -> Unit
) {
    val icon = when (item.actionType.lowercase(Locale.getDefault())) {
        "ride" -> Icons.AutoMirrored.Filled.DirectionsBike
        "parcel", "saman" -> Icons.Default.ShoppingBag
        "payment", "wasooli" -> Icons.Default.AttachMoney
        else -> Icons.Default.Person
    }

    val iconColor = when (item.actionType.lowercase(Locale.getDefault())) {
        "ride" -> EmeraldGreenPrimary
        "parcel", "saman" -> AccentAmber
        "payment", "wasooli" -> AccentBlue
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) {
                    onToggleSelect()
                }
            }
            .testTag("card_history_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Action Icon badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.actionType,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.actionType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.phoneNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.phoneNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (item.details.isNotBlank()) {
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Saman Image Thumbnail in History
                if (!item.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (!isSelectionMode) {
                                    onImageClick(item.imageUri, item.title)
                                }
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            AsyncImage(
                                model = item.imageUri,
                                contentDescription = "Saman Photo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "📷 Saman Photo Attached",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenPrimary
                            )
                            Text(
                                text = "Tap to view full / دیکھنے کیلئے ٹیپ کریں",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    if (item.amount > 0) {
                        Text(
                            text = "Rs. ${item.amount.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_delete_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = AccentRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
