package com.shuckler.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shuckler.app.accessibility.AccessibilityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.shuckler.app.achievement.AchievementManager
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.spotify.SpotifyAuthManager
import com.shuckler.app.spotify.SpotifyImportManager
import com.shuckler.app.spotify.SpotifySavingsTracker
import com.shuckler.app.lastfm.LastFmScrobbler
import com.shuckler.app.lyrics.LyricsRepository
import com.shuckler.app.personality.ListeningPersonalityManager
import com.shuckler.app.playlist.PlaylistManager
import com.shuckler.app.player.MusicServiceConnection
import com.shuckler.app.spotify.SpotifyImportManager.Companion.CHANNEL_IMPORT_COMPLETE
import com.shuckler.app.spotify.SpotifyImportManager.Companion.CHANNEL_IMPORT_PROGRESS

class ShucklerApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val musicServiceConnection = MusicServiceConnection()

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

    val spotifyAuthManager: SpotifyAuthManager by lazy {
        SpotifyAuthManager(applicationContext)
    }

    val spotifyImportManager: SpotifyImportManager by lazy {
        SpotifyImportManager(applicationContext, downloadManager, playlistManager)
    }

    val spotifySavingsTracker: SpotifySavingsTracker by lazy {
        SpotifySavingsTracker(applicationContext)
    }

    val lastFmScrobbler: LastFmScrobbler by lazy {
        LastFmScrobbler(applicationContext)
    }

    val accessibilityPreferences: AccessibilityPreferences by lazy {
        AccessibilityPreferences(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        musicServiceConnection.bind(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_IMPORT_PROGRESS,
                "Import progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows progress while rescuing your music library" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_IMPORT_COMPLETE,
                "Import complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when your library import finishes" }
        )
    }
}
