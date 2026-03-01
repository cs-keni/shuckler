package com.shuckler.app.personality

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Calendar

/**
 * Tracks when the user listens and computes a "listening personality" label
 * (e.g. Night owl, Morning listener, Weekend warrior).
 */
class ListeningPersonalityManager(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val timestamps: MutableList<Long>
        get() {
            val json = prefs.getString(KEY_TIMESTAMPS, "[]") ?: "[]"
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getLong(it) }.toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
        }

    /** Record a play session (call when a track starts playing). Keeps last MAX_SESSIONS. */
    fun recordPlaySession() {
        val list = timestamps
        list.add(System.currentTimeMillis())
        val trimmed = list.takeLast(MAX_SESSIONS)
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString(KEY_TIMESTAMPS, arr.toString()).apply()
    }

    /**
     * Compute listening personality from stored timestamps.
     * Returns a label and optional description.
     */
    fun computePersonality(): ListeningPersonality {
        val list = timestamps
        if (list.size < MIN_SESSIONS) {
            return ListeningPersonality(
                label = "Getting started",
                emoji = "🎧",
                description = "Keep listening to discover your listening style!"
            )
        }

        val cal = Calendar.getInstance()
        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0
        var weekday = 0
        var weekend = 0

        for (ts in list) {
            cal.timeInMillis = ts
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat
            when {
                hour in 6..11 -> morning++
                hour in 12..17 -> afternoon++
                hour in 18..23 -> evening++
                else -> night++ // 0-5
            }
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) weekend++
            else weekday++
        }

        val timeSlots = listOf(
            "morning" to morning,
            "afternoon" to afternoon,
            "evening" to evening,
            "night" to night
        )
        val dominantTime = timeSlots.maxByOrNull { it.second }!!
        val weekendRatio = weekend.toFloat() / list.size

        return when {
            dominantTime.first == "night" && dominantTime.second > list.size / 3 ->
                ListeningPersonality("Night owl", "🦉", "You listen most after midnight")
            dominantTime.first == "morning" && dominantTime.second > list.size / 3 ->
                ListeningPersonality("Morning listener", "☀️", "You start the day with music")
            dominantTime.first == "evening" && dominantTime.second > list.size / 3 ->
                ListeningPersonality("Evening listener", "🌙", "You wind down with music at night")
            dominantTime.first == "afternoon" && dominantTime.second > list.size / 3 ->
                ListeningPersonality("Afternoon listener", "🌤️", "You listen most in the afternoon")
            weekendRatio > 0.6 ->
                ListeningPersonality("Weekend warrior", "🎉", "You save most listening for the weekend")
            weekendRatio < 0.2 && list.size > 10 ->
                ListeningPersonality("Weekday regular", "📅", "You listen consistently during the week")
            else ->
                ListeningPersonality("Music enthusiast", "🎵", "You listen throughout the day")
        }
    }

    data class ListeningPersonality(
        val label: String,
        val emoji: String,
        val description: String
    )

    companion object {
        private const val PREFS_NAME = "shuckler_personality"
        private const val KEY_TIMESTAMPS = "play_session_timestamps"
        private const val MAX_SESSIONS = 200
        private const val MIN_SESSIONS = 5
    }
}
