package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Wallpaper presets shown in the "Wallpaper" folder of the three-dot menu.
 * Rendered as smooth gradients (no bundled image assets needed) so the main
 * dashboard display can look nicer without bloating the APK.
 */
enum class AppWallpaper(val displayName: String, val colors: List<Color>) {
    NONE("None (Plain)", emptyList()),
    EMERALD_NIGHT("Emerald Night", listOf(Color(0xFF032D1F), Color(0xFF0F5132), Color(0xFF11998E))),
    OCEAN_BREEZE("Ocean Breeze", listOf(Color(0xFF001F3F), Color(0xFF0B4F6C), Color(0xFF2196F3))),
    ROYAL_PURPLE("Royal Purple Dusk", listOf(Color(0xFF1A0933), Color(0xFF4A148C), Color(0xFF8E24AA))),
    SUNSET_GLOW("Sunset Glow", listOf(Color(0xFF3E0620), Color(0xFFB71C4A), Color(0xFFFF8A00))),
    MIDNIGHT_SKY("Midnight Sky", listOf(Color(0xFF04081A), Color(0xFF0F172A), Color(0xFF334155))),
    AURORA("Aurora Teal", listOf(Color(0xFF00110B), Color(0xFF00695C), Color(0xFF1DE9B6)))
}

/** Diagonal gradient brush for this wallpaper, or null for the plain/default background. */
fun AppWallpaper.brush(): Brush? =
    if (this == AppWallpaper.NONE || colors.isEmpty()) null
    else Brush.linearGradient(colors)
