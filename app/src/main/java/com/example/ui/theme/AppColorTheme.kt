package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Accent color presets the user can pick from the "Theme" folder in the
 * three-dot menu. Each preset drives both the primary and secondary
 * Material color used across the whole app (buttons, highlights, stat cards).
 */
enum class AppColorTheme(val displayName: String, val primary: Color, val secondary: Color) {
    CLASSIC_GREEN("Classic Green", GreenPrimary, BluePrimary),
    OCEAN_BLUE("Ocean Blue", Color(0xFF0288D1), Color(0xFF00ACC1)),
    ROYAL_PURPLE("Royal Purple", Color(0xFF8E24AA), Color(0xFFAB47BC)),
    SUNSET_ORANGE("Sunset Orange", Color(0xFFFB8C00), Color(0xFFE53935)),
    EMERALD("Emerald", Color(0xFF00A86B), Color(0xFF00695C)),
    ROSE_GOLD("Rose Gold", Color(0xFFE91E63), Color(0xFFF06292))
}
