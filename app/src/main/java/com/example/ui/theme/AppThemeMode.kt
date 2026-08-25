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
    TEAL("Teal (فیروزی تھیم)", "Teal", 0xFF0D9488);

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
                    else -> BLUE
                }
            }
        }
    }
}

