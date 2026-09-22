package com.example.util

import android.content.Context
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppWallpaper

/**
 * Small SharedPreferences wrapper so the user's Theme, color and Wallpaper
 * picks (three-dot menu -> Theme / Wallpaper folders) survive an app restart.
 */
object AppPreferences {
    private const val PREFS_NAME = "aqeel_rider_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR_THEME = "color_theme"
    private const val KEY_WALLPAPER = "wallpaper"

    fun getThemeMode(context: Context): AppThemeMode {
        val stored = prefs(context).getString(KEY_THEME_MODE, null) ?: return AppThemeMode.SYSTEM
        return runCatching { AppThemeMode.valueOf(stored) }.getOrDefault(AppThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getColorTheme(context: Context): AppColorTheme {
        val stored = prefs(context).getString(KEY_COLOR_THEME, null) ?: return AppColorTheme.CLASSIC_GREEN
        return runCatching { AppColorTheme.valueOf(stored) }.getOrDefault(AppColorTheme.CLASSIC_GREEN)
    }

    fun setColorTheme(context: Context, theme: AppColorTheme) {
        prefs(context).edit().putString(KEY_COLOR_THEME, theme.name).apply()
    }

    fun getWallpaper(context: Context): AppWallpaper {
        val stored = prefs(context).getString(KEY_WALLPAPER, null) ?: return AppWallpaper.NONE
        return runCatching { AppWallpaper.valueOf(stored) }.getOrDefault(AppWallpaper.NONE)
    }

    fun setWallpaper(context: Context, wallpaper: AppWallpaper) {
        prefs(context).edit().putString(KEY_WALLPAPER, wallpaper.name).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
