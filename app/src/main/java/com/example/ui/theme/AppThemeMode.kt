package com.example.ui.theme

enum class AppThemeMode(
    val titleUrdu: String,
    val titleEnglish: String,
    val primaryColorLong: Long
) {
    BLUE("Blue (نیلا تھیم)", "Blue", 0xFF0284C7),
    GREEN("Green (سبز تھیم)", "Green", 0xFF0D6338),
    PURPLE("Purple (جامنی تھیم)", "Purple", 0xFF7C3AED),
    ORANGE("Orange (نارنجی تھیم)", "Orange", 0xFFEA580C),
    RED("Red (سرخ تھیم)", "Red", 0xFFDC2626),
    TEAL("Teal (فیروزی تھیم)", "Teal", 0xFF0D9488),
    GOLD("Gold / Amber (سنہری تھیم)", "Gold", 0xFFD97706),
    INDIGO("Indigo (شاہی نیلا تھیم)", "Indigo", 0xFF4F46E5),
    ROSE("Rose / Pink (گلابی تھیم)", "Rose", 0xFFE11D48),
    EMERALD("Emerald (زمرد تھیم)", "Emerald", 0xFF059669),
    COFFEE("Coffee / Brown (کافی تھیم)", "Coffee", 0xFF78350F),
    SLATE("Slate / Graphite (سلیٹ تھیم)", "Slate", 0xFF475569),
    NIGHT_VISION("Night Vision (نائٹ ویژن - ہائی کنٹراسٹ)", "Night Vision", 0xFF00FF66);

    companion object {
        fun fromNameOrDefault(name: String?): AppThemeMode {
            if (name == null) return BLUE
            return try {
                valueOf(name)
            } catch (_: Exception) {
                // Fallback for legacy SYSTEM/LIGHT/DARK
                when (name.uppercase()) {
                    "GREEN" -> GREEN
                    "PURPLE" -> PURPLE
                    "ORANGE" -> ORANGE
                    "RED" -> RED
                    "TEAL" -> TEAL
                    "GOLD" -> GOLD
                    "INDIGO" -> INDIGO
                    "ROSE" -> ROSE
                    "EMERALD" -> EMERALD
                    "COFFEE" -> COFFEE
                    "SLATE" -> SLATE
                    "NIGHT_VISION", "NIGHT", "VISION" -> NIGHT_VISION
                    else -> BLUE
                }
            }
        }
    }
}

