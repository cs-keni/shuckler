package com.shuckler.app.achievement

import android.content.Context
import android.content.SharedPreferences
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.playlist.Playlist
import org.json.JSONArray

/**
 * Achievement badge definitions and unlock tracking.
 * Phase 38: Gamification — unlock badges for milestones.
 */
data class AchievementBadge(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String
)

object AchievementDefinitions {
    val ALL = listOf(
        AchievementBadge("first_download", "First Download", "Download your first track", "📥"),
        AchievementBadge("first_playlist", "First Playlist", "Create your first playlist", "📋"),
        AchievementBadge("ten_favorites", "Music Lover", "Add 10 tracks to favorites", "❤️"),
        AchievementBadge("hundred_plays", "Century", "Play 100 tracks", "🎵"),
        AchievementBadge("library_50", "Collector", "Build a library of 50 tracks", "📚"),
        AchievementBadge("library_100", "Archivist", "Build a library of 100 tracks", "🏛️"),
        AchievementBadge("twenty_favorites", "Super Fan", "Add 20 tracks to favorites", "💖"),
        AchievementBadge("five_playlists", "Organizer", "Create 5 playlists", "📂")
    )

    fun get(id: String): AchievementBadge? = ALL.find { it.id == id }
}

class AchievementManager(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val unlockedSet: MutableSet<String>
        get() {
            val json = prefs.getString(KEY_UNLOCKED, "[]") ?: "[]"
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }.toMutableSet()
            } catch (_: Exception) {
                mutableSetOf()
            }
        }

    fun getUnlockedIds(): Set<String> = unlockedSet

    fun isUnlocked(badgeId: String): Boolean = unlockedSet.contains(badgeId)

    /**
     * Check conditions and unlock any new badges. Returns list of newly unlocked badge IDs.
     */
    fun checkAndUnlock(
        downloads: List<DownloadedTrack>,
        playlists: List<Playlist>,
        totalPlayCount: Int
    ): List<String> {
        val completed = downloads.filter { it.filePath.isNotBlank() }
        val favoriteCount = completed.count { it.isFavorite }
        val newlyUnlocked = mutableListOf<String>()

        fun unlock(id: String): Boolean {
            if (!unlockedSet.contains(id)) {
                unlockedSet.add(id)
                saveUnlocked(unlockedSet)
                newlyUnlocked.add(id)
                return true
            }
            return false
        }

        if (completed.isNotEmpty()) unlock("first_download")
        if (playlists.isNotEmpty()) unlock("first_playlist")
        if (favoriteCount >= 10) unlock("ten_favorites")
        if (favoriteCount >= 20) unlock("twenty_favorites")
        if (totalPlayCount >= 100) unlock("hundred_plays")
        if (completed.size >= 50) unlock("library_50")
        if (completed.size >= 100) unlock("library_100")
        if (playlists.size >= 5) unlock("five_playlists")

        return newlyUnlocked
    }

    private fun saveUnlocked(ids: Set<String>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString(KEY_UNLOCKED, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "shuckler_achievements"
        private const val KEY_UNLOCKED = "unlocked_badges"
    }
}
