package com.shuckler.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shuckler.app.ui.theme.LocalThemeMode
import com.shuckler.app.ui.theme.LocalThemeModeSetter
import com.shuckler.app.ui.theme.ThemeMode
import com.shuckler.app.ui.theme.loadThemeMode
import com.shuckler.app.ui.theme.saveThemeMode
import androidx.core.content.ContextCompat
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.navigation.ShucklerNavGraph
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.MusicServiceConnection
import com.shuckler.app.ui.theme.ShucklerTheme

class MainActivity : ComponentActivity() {

    private val musicServiceConnection = MusicServiceConnection()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        musicServiceConnection.bind(this)
        enableEdgeToEdge()
        val downloadManager = (application as ShucklerApplication).downloadManager
        setContent {
            val (themeMode, setThemeModeState) = remember { mutableStateOf(loadThemeMode(this)) }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(
                LocalThemeMode provides themeMode,
                LocalThemeModeSetter provides { mode ->
                    saveThemeMode(this, mode)
                    setThemeModeState(mode)
                }
            ) {
                ShucklerTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalMusicServiceConnection provides musicServiceConnection,
                        LocalDownloadManager provides downloadManager
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            ShucklerNavGraph()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicServiceConnection.unbind(this)
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
