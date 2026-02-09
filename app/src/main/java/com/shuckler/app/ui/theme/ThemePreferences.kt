package com.shuckler.app.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

enum class ThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system")
}

private const val PREFS_NAME = "theme_prefs"
private const val KEY_THEME_MODE = "theme_mode"

fun loadThemeMode(context: Context): ThemeMode {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val key = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key) ?: ThemeMode.SYSTEM.key
    return ThemeMode.entries.find { it.key == key } ?: ThemeMode.SYSTEM
}

fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME_MODE, mode.key)
        .apply()
}

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalThemeModeSetter = compositionLocalOf< (ThemeMode) -> Unit> { {} }
