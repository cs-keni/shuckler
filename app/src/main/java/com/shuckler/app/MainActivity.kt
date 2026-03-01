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
import com.shuckler.app.achievement.AchievementManager
import com.shuckler.app.achievement.LocalAchievementManager
import com.shuckler.app.personality.ListeningPersonalityManager
import com.shuckler.app.personality.LocalListeningPersonalityManager
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.navigation.ShucklerNavGraph
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.PlaylistManager
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.MusicServiceConnection
import com.shuckler.app.shortcut.AppShortcutHandler
import com.shuckler.app.ui.theme.ShucklerTheme

class MainActivity : ComponentActivity() {

    private val musicServiceConnection = MusicServiceConnection()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShortcutIntent(intent)
        requestNotificationPermission()
        musicServiceConnection.bind(this)
        enableEdgeToEdge()
        val downloadManager = (application as ShucklerApplication).downloadManager
        val showOnboarding = !OnboardingPreferences.hasCompletedOnboarding(this)
        val app = application as ShucklerApplication
        setContent {
            var onboardingComplete by remember { mutableStateOf(!showOnboarding) }
            CompositionLocalProvider(
                LocalAccessibilityPreferences provides app.accessibilityPreferences
            ) {
            ShucklerTheme {
                if (onboardingComplete) {
                    CompositionLocalProvider(
                        LocalMusicServiceConnection provides musicServiceConnection,
                        LocalDownloadManager provides downloadManager,
                        LocalPlaylistManager provides (application as ShucklerApplication).playlistManager,
                        LocalAchievementManager provides app.achievementManager,
                        LocalListeningPersonalityManager provides app.listeningPersonalityManager
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            ShucklerNavGraph()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        musicServiceConnection.unbind(this)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (AppShortcutHandler.handleShortcutIntent(this, intent)) {
            intent?.removeExtra(AppShortcutHandler.EXTRA_SHORTCUT_ACTION)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> { /* Already granted */ }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
