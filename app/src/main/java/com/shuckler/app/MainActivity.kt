package com.shuckler.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shuckler.app.onboarding.OnboardingPreferences
import com.shuckler.app.ui.OnboardingScreen
import androidx.core.content.ContextCompat
import com.shuckler.app.accessibility.AccessibilityPreferences
import com.shuckler.app.accessibility.LocalAccessibilityPreferences
import com.shuckler.app.achievement.LocalAchievementManager
import com.shuckler.app.personality.LocalListeningPersonalityManager
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.navigation.ShucklerNavGraph
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.shortcut.AppShortcutHandler
import com.shuckler.app.spotify.LocalSpotifyAuthManager
import com.shuckler.app.spotify.SpotifyImportManager
import com.shuckler.app.lastfm.LocalLastFmScrobbler
import com.shuckler.app.ui.theme.ShucklerTheme
import com.shuckler.app.BuildConfig
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Surviving import-complete intent across process death: store as state so NavGraph
    // can observe it. Written from onNewIntent so it works whether the app was running or cold-started.
    private val _pendingImportScreen = mutableStateOf<String?>(null)
    private val _pendingImportId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShortcutIntent(intent)
        handleSpotifyCallback(intent)
        handleLastFmCallback(intent)
        handleImportCompleteIntent(intent)
        requestNotificationPermission()
        enableEdgeToEdge()
        val app = application as ShucklerApplication
        val downloadManager = app.downloadManager
        val showOnboarding = !OnboardingPreferences.hasCompletedOnboarding(this)
        setContent {
            var onboardingComplete by remember { mutableStateOf(!showOnboarding) }
            val pendingScreen by _pendingImportScreen
            val pendingImportId by _pendingImportId
            CompositionLocalProvider(
                LocalAccessibilityPreferences provides app.accessibilityPreferences
            ) {
                ShucklerTheme {
                    if (onboardingComplete) {
                        CompositionLocalProvider(
                            LocalMusicServiceConnection provides app.musicServiceConnection,
                            LocalDownloadManager provides downloadManager,
                            LocalPlaylistManager provides app.playlistManager,
                            LocalAchievementManager provides app.achievementManager,
                            LocalListeningPersonalityManager provides app.listeningPersonalityManager,
                            LocalSpotifyAuthManager provides app.spotifyAuthManager,
                            LocalLastFmScrobbler provides app.lastFmScrobbler
                        ) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                ShucklerNavGraph(
                                    pendingImportScreen = pendingScreen,
                                    pendingImportId = pendingImportId,
                                    onImportIntentConsumed = {
                                        _pendingImportScreen.value = null
                                        _pendingImportId.value = null
                                    }
                                )
                            }
                        }
                    } else {
                        OnboardingScreen(onComplete = {
                            OnboardingPreferences.setOnboardingCompleted(this@MainActivity)
                            onboardingComplete = true
                        })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
        handleSpotifyCallback(intent)
        handleLastFmCallback(intent)
        handleImportCompleteIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (AppShortcutHandler.handleShortcutIntent(this, intent)) {
            intent?.removeExtra(AppShortcutHandler.EXTRA_SHORTCUT_ACTION)
        }
    }

    private fun handleSpotifyCallback(intent: Intent?) {
        val clientId = BuildConfig.SPOTIFY_CLIENT_ID
        if (clientId.isBlank()) return
        lifecycleScope.launch {
            val handled = (application as ShucklerApplication).spotifyAuthManager.handleCallback(intent, clientId)
            if (handled) setIntent(Intent())
        }
    }

    private fun handleLastFmCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "shuckler" || data.host != "lastfm") return
        val token = data.getQueryParameter("token")?.takeIf { it.isNotBlank() } ?: return
        lifecycleScope.launch {
            (application as ShucklerApplication).lastFmScrobbler.exchangeToken(token)
            setIntent(Intent())
        }
    }

    private fun handleImportCompleteIntent(intent: Intent?) {
        val screen = intent?.getStringExtra(SpotifyImportManager.EXTRA_SCREEN) ?: return
        val importId = intent.getStringExtra(SpotifyImportManager.EXTRA_IMPORT_ID) ?: return
        if (screen == SpotifyImportManager.SCREEN_IMPORT_COMPLETE ||
            screen == SpotifyImportManager.SCREEN_MISMATCH_REVIEW
        ) {
            _pendingImportScreen.value = screen
            _pendingImportId.value = importId
            setIntent(Intent())
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
