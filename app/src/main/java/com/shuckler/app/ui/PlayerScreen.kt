package com.shuckler.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.player.DefaultTrackInfo
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.ui.theme.LocalThemeMode
import com.shuckler.app.ui.theme.LocalThemeModeSetter
import com.shuckler.app.ui.theme.ThemeMode
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val trackTitle by viewModel.currentTrackTitle.collectAsState(initial = DefaultTrackInfo.TITLE)
    val trackArtist by viewModel.currentTrackArtist.collectAsState(initial = DefaultTrackInfo.ARTIST)
    val repeatMode by viewModel.repeatMode.collectAsState(initial = Player.REPEAT_MODE_OFF)
    val positionMs by viewModel.playbackPositionMs.collectAsState(initial = 0L)
    val durationMs by viewModel.durationMs.collectAsState(initial = 0L)
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val thumbnailUrl by viewModel.currentTrackThumbnailUrl.collectAsState(initial = null)
    val downloadManager = LocalDownloadManager.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            viewModel.updatePlaybackProgress()
        }
    }

    val themeMode = LocalThemeMode.current
    val setThemeMode = LocalThemeModeSetter.current
    if (showSettingsDialog) {
        SettingsDialog(
            themeMode = themeMode,
            onThemeModeChange = setThemeMode,
            autoDeleteAfterPlayback = downloadManager.autoDeleteAfterPlayback,
            onAutoDeleteChange = { downloadManager.autoDeleteAfterPlayback = it },
            crossfadeDurationMs = downloadManager.crossfadeDurationMs,
            onCrossfadeChange = { downloadManager.crossfadeDurationMs = it },
            downloadQuality = downloadManager.downloadQuality,
            onDownloadQualityChange = { downloadManager.downloadQuality = it },
            onDismiss = { showSettingsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = trackTitle,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = trackArtist,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (queueInfo.second > 1) {
            Text(
                text = "Track ${queueInfo.first} of ${queueInfo.second}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPlaybackTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        var isSeekDragging by remember { mutableStateOf(false) }
        var seekPosition by remember { mutableStateOf(0f) }
        val maxDuration = (durationMs.takeIf { it > 0 } ?: 1L).toFloat()
        Slider(
            value = if (isSeekDragging) seekPosition else positionMs.toFloat().coerceIn(0f, maxDuration),
            onValueChange = {
                seekPosition = it
                isSeekDragging = true
            },
            onValueChangeFinished = {
                viewModel.seekTo(seekPosition.toLong())
                isSeekDragging = false
            },
            valueRange = 0f..maxDuration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Button(
                onClick = { viewModel.skipToPrevious() }
            ) {
                Text("Previous")
            }
            Button(
                onClick = { viewModel.togglePlayPause() }
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(
                onClick = { viewModel.skipToNext() }
            ) {
                Text("Next")
            }
        }

        Button(
            onClick = { viewModel.cycleLoopMode() },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                if (repeatMode == Player.REPEAT_MODE_ONE) "Loop: On" else "Loop: Off"
            )
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = (ms / 1000).toInt()
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private val DOWNLOAD_QUALITY_OPTIONS = listOf(
    "best" to "Best",
    "high" to "High",
    "data_saver" to "Data saver"
)

@Composable
private fun SettingsDialog(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    autoDeleteAfterPlayback: Boolean,
    onAutoDeleteChange: (Boolean) -> Unit,
    crossfadeDurationMs: Int,
    onCrossfadeChange: (Int) -> Unit,
    downloadQuality: String,
    onDownloadQualityChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var checked by remember(autoDeleteAfterPlayback) { mutableStateOf(autoDeleteAfterPlayback) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "App theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                        val selected = themeMode == mode
                        Button(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Crossfade",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (crossfadeDurationMs == 0) "Off" else "${crossfadeDurationMs / 1000} s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = (crossfadeDurationMs / 1000f).coerceIn(0f, 10f),
                    onValueChange = { sec ->
                        onCrossfadeChange((sec * 1000).roundToInt().coerceIn(0, 10000))
                    },
                    valueRange = 0f..10f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Download quality (YouTube)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DOWNLOAD_QUALITY_OPTIONS.forEach { (value, label) ->
                        val selected = downloadQuality == value
                        Button(
                            onClick = { onDownloadQualityChange(value) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Text(
                    text = "Best = highest bitrate; High = second-highest; Data saver = smallest file. M4A preferred when available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Delete after playback (except favorites)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remove track when it finishes, unless it's a favorite",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            onAutoDeleteChange(it)
                        }
                    )
                }
                Text(
                    text = "When on, tracks are removed automatically when they finish playing, unless they are marked as favorites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
