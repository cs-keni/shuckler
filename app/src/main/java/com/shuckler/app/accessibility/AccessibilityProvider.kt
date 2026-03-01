package com.shuckler.app.accessibility

import androidx.compose.runtime.compositionLocalOf

val LocalAccessibilityPreferences = compositionLocalOf<AccessibilityPreferences> {
    error("No AccessibilityPreferences provided")
}
