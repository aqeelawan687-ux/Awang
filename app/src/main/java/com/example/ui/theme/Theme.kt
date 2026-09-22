package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun darkSchemeFor(colorTheme: AppColorTheme) = darkColorScheme(
    primary = colorTheme.primary,
    onPrimary = Color.Black,
    primaryContainer = colorTheme.primary.copy(alpha = 0.35f).compositeOverSlate900(),
    onPrimaryContainer = Color.White,
    secondary = colorTheme.secondary,
    onSecondary = Color.White,
    secondaryContainer = colorTheme.secondary.copy(alpha = 0.35f).compositeOverSlate900(),
    onSecondaryContainer = Color.White,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    error = RedError,
    onError = Color.White
)

private fun lightSchemeFor(colorTheme: AppColorTheme) = lightColorScheme(
    primary = colorTheme.primary,
    onPrimary = Color.White,
    primaryContainer = colorTheme.primary.copy(alpha = 0.15f).compositeOverWhite(),
    onPrimaryContainer = colorTheme.primary,
    secondary = colorTheme.secondary,
    onSecondary = Color.White,
    secondaryContainer = colorTheme.secondary.copy(alpha = 0.15f).compositeOverWhite(),
    onSecondaryContainer = colorTheme.secondary,
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    error = RedError,
    onError = Color.White
)

private fun Color.compositeOverSlate900(): Color = Color(
    red = red * alpha + Slate900.red * (1 - alpha),
    green = green * alpha + Slate900.green * (1 - alpha),
    blue = blue * alpha + Slate900.blue * (1 - alpha),
    alpha = 1f
)

private fun Color.compositeOverWhite(): Color = Color(
    red = red * alpha + (1 - alpha),
    green = green * alpha + (1 - alpha),
    blue = blue * alpha + (1 - alpha),
    alpha = 1f
)

@Composable
fun AqeelRiderTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorTheme: AppColorTheme = AppColorTheme.CLASSIC_GREEN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) darkSchemeFor(colorTheme) else lightSchemeFor(colorTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
