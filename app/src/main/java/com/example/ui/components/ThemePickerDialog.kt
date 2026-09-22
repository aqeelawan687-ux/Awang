package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppThemeMode

/**
 * "Theme" folder shown from the three-dot menu: light/dark/system mode plus
 * a grid of accent color presets, all in one dedicated place.
 */
@Composable
fun ThemePickerDialog(
    currentThemeMode: AppThemeMode,
    currentColorTheme: AppColorTheme,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onColorThemeChange: (AppColorTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = currentColorTheme.primary) },
        title = { Text("Theme", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Appearance Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(
                        label = "System",
                        icon = Icons.Default.SettingsSystemDaydream,
                        selected = currentThemeMode == AppThemeMode.SYSTEM,
                        accent = currentColorTheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeModeChange(AppThemeMode.SYSTEM) }
                    )
                    ModeChip(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        selected = currentThemeMode == AppThemeMode.LIGHT,
                        accent = currentColorTheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeModeChange(AppThemeMode.LIGHT) }
                    )
                    ModeChip(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        selected = currentThemeMode == AppThemeMode.DARK,
                        accent = currentColorTheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeModeChange(AppThemeMode.DARK) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AppColorTheme.entries) { preset ->
                        ColorSwatch(
                            preset = preset,
                            selected = preset == currentColorTheme,
                            onClick = { onColorThemeChange(preset) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ColorSwatch(
    preset: AppColorTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(preset.primary, preset.secondary)))
                .border(
                    width = if (selected) 2.5.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                ),
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = preset.displayName, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
