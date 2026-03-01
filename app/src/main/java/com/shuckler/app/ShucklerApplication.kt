package com.shuckler.app

import android.app.Application
import com.shuckler.app.accessibility.AccessibilityPreferences
import com.shuckler.app.achievement.AchievementManager
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.lyrics.LyricsRepository
import com.shuckler.app.personality.ListeningPersonalityManager
import com.shuckler.app.playlist.PlaylistManager

class ShucklerApplication : Application() {

    val listeningPersonalityManager: ListeningPersonalityManager by lazy {
        ListeningPersonalityManager(applicationContext)
    }

    val downloadManager: DownloadManager by lazy {
        DownloadManager(applicationContext)
    }

    val playlistManager: PlaylistManager by lazy {
        PlaylistManager(applicationContext)
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(applicationContext)
    }

    val achievementManager: AchievementManager by lazy {
        AchievementManager(applicationContext)
    }

    val accessibilityPreferences: AccessibilityPreferences by lazy {
        AccessibilityPreferences(applicationContext)
    }
}
