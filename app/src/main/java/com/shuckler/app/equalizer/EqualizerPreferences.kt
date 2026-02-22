package com.shuckler.app.equalizer

import android.content.Context
import android.content.SharedPreferences

/** Spotify-style equalizer: 6 fixed bands, -12 dB to +12 dB, default 0. */
object EqualizerPreferences {
    private const val PREFS_NAME = "shuckler_equalizer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET_INDEX = "preset_index"
    private const val KEY_BAND_LEVELS = "band_levels" // comma-separated dB per band (6 bands)

    const val BAND_COUNT = 6
    const val MIN_DB = -12
    const val MAX_DB = 12

    /** Band center frequencies in Hz (Spotify-style). */
    val BAND_FREQUENCIES_HZ = intArrayOf(60, 150, 400, 1_000, 2_400, 15_000)

    /** Preset name and 6 band levels in dB. Index -1 = Custom (use saved band levels). */
    val PRESETS: List<Pair<String, IntArray>> = listOf(
        "Flat" to intArrayOf(0, 0, 0, 0, 0, 0),
        "Bass Boost" to intArrayOf(6, 4, 0, -2, -2, 0),
        "Treble Boost" to intArrayOf(0, -2, -2, 0, 4, 6),
        "Pop" to intArrayOf(2, 1, 0, 2, 3, 2),
        "Rock" to intArrayOf(4, 2, -2, 1, 3, 4),
        "Jazz" to intArrayOf(3, 2, 0, 1, 2, 3),
        "Classical" to intArrayOf(2, 1, 1, 0, 2, 4),
        "Hip-Hop" to intArrayOf(6, 4, 2, 0, 0, 0),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** -1 = custom (use band levels); 0..n = preset index. */
    fun getPresetIndex(context: Context): Int =
        prefs(context).getInt(KEY_PRESET_INDEX, -1)

    fun setPresetIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_PRESET_INDEX, index).apply()
    }

    /** Load band levels in dB (0..BAND_COUNT-1). Returns null to use defaults (all 0). */
    fun getBandLevelsDb(context: Context): IntArray? {
        val s = prefs(context).getString(KEY_BAND_LEVELS, null) ?: return null
        val parts = s.split(",").mapNotNull { it.toIntOrNull() }
        if (parts.size != BAND_COUNT) return null
        return parts.map { it.coerceIn(MIN_DB, MAX_DB) }.toIntArray()
    }

    fun setBandLevelsDb(context: Context, levels: IntArray) {
        val s = levels.map { it.coerceIn(MIN_DB, MAX_DB) }.take(BAND_COUNT).joinToString(",")
        prefs(context).edit().putString(KEY_BAND_LEVELS, s).apply()
    }

    /** Get effective band levels: from preset if selected, else from saved custom. Default all 0. */
    fun getEffectiveBandLevelsDb(context: Context): IntArray {
        val presetIndex = getPresetIndex(context)
        if (presetIndex in PRESETS.indices) {
            return PRESETS[presetIndex].second.copyOf()
        }
        return getBandLevelsDb(context) ?: IntArray(BAND_COUNT) { 0 }
    }
}
