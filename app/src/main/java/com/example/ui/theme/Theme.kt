package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun getThemeColorScheme(themeMode: AppThemeMode, isDark: Boolean): ColorScheme {
    return when (themeMode) {
        AppThemeMode.BLUE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFF60A5FA),
                tertiary = Color(0xFF93C5FD),
                background = BlueDarkSurface,
                surface = Color(0xFF0F172A),
                surfaceContainer = BlueDarkCard,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = BluePrimary,
                secondary = BlueSecondary,
                tertiary = AccentAmber,
                background = BlueLightBg,
                surface = Color.White,
                surfaceContainer = BlueContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }

        AppThemeMode.GREEN -> if (isDark) {
            darkColorScheme(
                primary = Emerald80,
                secondary = EmeraldGreen70,
                tertiary = MintContainer,
                background = DarkBackground,
                surface = DarkSurface,
                surfaceContainer = DarkCardBg,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = GreenPrimary,
                secondary = GreenSecondary,
                tertiary = AccentAmber,
                background = GreenLightBg,
                surface = Color.White,
                surfaceContainer = GreenContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }

        AppThemeMode.PURPLE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFC084FC),
                secondary = Color(0xFFA855F7),
                tertiary = Color(0xFFE9D5FF),
                background = Color(0xFF1E1035),
                surface = Color(0xFF281845),
                surfaceContainer = Color(0xFF38225C),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = PurplePrimary,
                secondary = PurpleSecondary,
                tertiary = AccentAmber,
                background = PurpleLightBg,
                surface = Color.White,
                surfaceContainer = PurpleContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1E1035),
                onSurface = Color(0xFF1E1035)
            )
        }

        AppThemeMode.ORANGE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFB923C),
                secondary = Color(0xFFF97316),
                tertiary = Color(0xFFFED7AA),
                background = Color(0xFF291508),
                surface = Color(0xFF3B1E0C),
                surfaceContainer = Color(0xFF4D2912),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = OrangePrimary,
                secondary = OrangeSecondary,
                tertiary = AccentAmber,
                background = OrangeLightBg,
                surface = Color.White,
                surfaceContainer = OrangeContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF291508),
                onSurface = Color(0xFF291508)
            )
        }

        AppThemeMode.RED -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFF87171),
                secondary = Color(0xFFEF4444),
                tertiary = Color(0xFFFECACA),
                background = Color(0xFF2A0D0D),
                surface = Color(0xFF3D1414),
                surfaceContainer = Color(0xFF501C1C),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = RedPrimary,
                secondary = RedSecondary,
                tertiary = AccentAmber,
                background = RedLightBg,
                surface = Color.White,
                surfaceContainer = RedContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF2A0D0D),
                onSurface = Color(0xFF2A0D0D)
            )
        }

        AppThemeMode.TEAL -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF2DD4BF),
                secondary = Color(0xFF14B8A6),
                tertiary = Color(0xFF99F6E4),
                background = Color(0xFF042F2E),
                surface = Color(0xFF0A3E3D),
                surfaceContainer = Color(0xFF114F4E),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = TealPrimary,
                secondary = TealSecondary,
                tertiary = AccentAmber,
                background = TealLightBg,
                surface = Color.White,
                surfaceContainer = TealContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF042F2E),
                onSurface = Color(0xFF042F2E)
            )
        }

        AppThemeMode.GOLD -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFBBF24),
                secondary = Color(0xFFF59E0B),
                tertiary = Color(0xFFFDE68A),
                background = Color(0xFF2E1C05),
                surface = Color(0xFF3F2707),
                surfaceContainer = Color(0xFF55350A),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = GoldPrimary,
                secondary = GoldSecondary,
                tertiary = AccentAmber,
                background = GoldLightBg,
                surface = Color.White,
                surfaceContainer = GoldContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF2E1C05),
                onSurface = Color(0xFF2E1C05)
            )
        }

        AppThemeMode.INDIGO -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF818CF8),
                secondary = Color(0xFF6366F1),
                tertiary = Color(0xFFC7D2FE),
                background = Color(0xFF131131),
                surface = Color(0xFF1C1A46),
                surfaceContainer = Color(0xFF292663),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = IndigoPrimary,
                secondary = IndigoSecondary,
                tertiary = AccentAmber,
                background = IndigoLightBg,
                surface = Color.White,
                surfaceContainer = IndigoContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF131131),
                onSurface = Color(0xFF131131)
            )
        }

        AppThemeMode.ROSE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFB7185),
                secondary = Color(0xFFF43F5E),
                tertiary = Color(0xFFFECDD3),
                background = Color(0xFF2B0B13),
                surface = Color(0xFF3E101D),
                surfaceContainer = Color(0xFF561628),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = RosePrimary,
                secondary = RoseSecondary,
                tertiary = AccentAmber,
                background = RoseLightBg,
                surface = Color.White,
                surfaceContainer = RoseContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF2B0B13),
                onSurface = Color(0xFF2B0B13)
            )
        }

        AppThemeMode.EMERALD -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF34D399),
                secondary = Color(0xFF10B981),
                tertiary = Color(0xFFA7F3D0),
                background = Color(0xFF04281E),
                surface = Color(0xFF063A2C),
                surfaceContainer = Color(0xFF0B4E3B),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = EmeraldPrimary,
                secondary = EmeraldSecondary,
                tertiary = AccentAmber,
                background = EmeraldLightBg,
                surface = Color.White,
                surfaceContainer = EmeraldContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF04281E),
                onSurface = Color(0xFF04281E)
            )
        }

        AppThemeMode.COFFEE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFD97706),
                secondary = Color(0xFFB45309),
                tertiary = Color(0xFFFDE68A),
                background = Color(0xFF241408),
                surface = Color(0xFF351F0D),
                surfaceContainer = Color(0xFF4A2B12),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = CoffeePrimary,
                secondary = CoffeeSecondary,
                tertiary = AccentAmber,
                background = CoffeeLightBg,
                surface = Color.White,
                surfaceContainer = CoffeeContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF241408),
                onSurface = Color(0xFF241408)
            )
        }

        AppThemeMode.SLATE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF94A3B8),
                secondary = Color(0xFF64748B),
                tertiary = Color(0xFFCBD5E1),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                surfaceContainer = Color(0xFF334155),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = SlatePrimary,
                secondary = SlateSecondary,
                tertiary = AccentAmber,
                background = SlateLightBg,
                surface = Color.White,
                surfaceContainer = SlateContainer,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun AqeelRiderTheme(
    themeMode: AppThemeMode = AppThemeMode.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(themeMode, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AqeelRiderTheme(darkTheme = darkTheme, content = content)
}

