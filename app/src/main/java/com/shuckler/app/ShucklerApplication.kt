package com.shuckler.app

import android.app.Application
import com.shuckler.app.accessibility.AccessibilityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.shuckler.app.achievement.AchievementManager
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.spotify.SpotifyAuthManager
import com.shuckler.app.lastfm.LastFmScrobbler
import com.shuckler.app.lyrics.LyricsRepository
import com.shuckler.app.personality.ListeningPersonalityManager
import com.shuckler.app.playlist.PlaylistManager
import com.shuckler.app.player.MusicServiceConnection

class ShucklerApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Singleton scoped to the process so it survives activity recreation (config changes).
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

    val lastFmScrobbler: LastFmScrobbler by lazy {
        LastFmScrobbler(applicationContext)
    }

    val accessibilityPreferences: AccessibilityPreferences by lazy {
        AccessibilityPreferences(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        musicServiceConnection.bind(this)
    }
}
