package com.shuckler.app.accessibility

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Accessibility preferences: reduced motion, high contrast.
 * Used to adapt animations and theme for users who need them.
 */
class AccessibilityPreferences(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _highContrastFlow = MutableStateFlow(prefs.getBoolean(KEY_HIGH_CONTRAST, false))
    val highContrastFlow: StateFlow<Boolean> = _highContrastFlow.asStateFlow()

    private val _reduceMotionFlow = MutableStateFlow(prefs.getBoolean(KEY_REDUCE_MOTION, false))
    val reduceMotionFlow: StateFlow<Boolean> = _reduceMotionFlow.asStateFlow()

    /** When true, animations are minimized or disabled. */
    var reduceMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        set(value) {
            prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()
            _reduceMotionFlow.value = value
        }

    /** When true, use high-contrast color scheme for better visibility. */
    var highContrast: Boolean
        get() = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIGH_CONTRAST, value).apply()
            _highContrastFlow.value = value
        }

    companion object {
        private const val PREFS_NAME = "shuckler_accessibility"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
    }
}
