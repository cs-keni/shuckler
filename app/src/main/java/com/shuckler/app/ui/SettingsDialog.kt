package com.shuckler.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val DOWNLOAD_QUALITY_OPTIONS = listOf(
    "best" to "Best",
    "high" to "High",
    "data_saver" to "Data saver"
)

private val SLEEP_TIMER_OPTIONS = listOf(
    null to "Off",
    15 * 60 * 1000L to "15 min",
    30 * 60 * 1000L to "30 min",
    45 * 60 * 1000L to "45 min",
    60 * 60 * 1000L to "60 min",
    -1L to "When track ends"
)

private val DEFAULT_TAB_OPTIONS = listOf(
    "home" to "Home",
    "search" to "Search",
    "library" to "Library",
    "analytics" to "Analytics"
)

@Composable
fun SettingsDialog(
    autoDeleteAfterPlayback: Boolean,
    onAutoDeleteChange: (Boolean) -> Unit,
    crossfadeDurationMs: Int,
    onCrossfadeChange: (Int) -> Unit,
    downloadQuality: String,
    onDownloadQualityChange: (String) -> Unit,
    sleepTimerRemainingMs: Long?,
    onStartSleepTimer: (Long, Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    sleepTimerFadeLastMinute: Boolean,
    onSleepTimerFadeChange: (Boolean) -> Unit,
    defaultTab: String = "home",
    onDefaultTabChange: (String) -> Unit = {},
    wifiOnlyDownloads: Boolean = false,
    onWifiOnlyDownloadsChange: (Boolean) -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onShowTutorial: () -> Unit = {},
    reduceMotion: Boolean = false,
    onReduceMotionChange: (Boolean) -> Unit = {},
    highContrast: Boolean = false,
    onHighContrastChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    var checked by remember(autoDeleteAfterPlayback) { mutableStateOf(autoDeleteAfterPlayback) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    text = "Sleep timer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SLEEP_TIMER_OPTIONS.take(3),
                        SLEEP_TIMER_OPTIONS.drop(3)
                    ).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { (durationMs, label) ->
                                val isSelected = when {
                                    durationMs == null -> sleepTimerRemainingMs == null
                                    durationMs == -1L -> sleepTimerRemainingMs == com.shuckler.app.player.MusicPlayerService.SLEEP_TIMER_END_OF_TRACK
                                    durationMs > 0 -> {
                                        val rem = sleepTimerRemainingMs ?: 0L
                                        rem > 0 && rem != com.shuckler.app.player.MusicPlayerService.SLEEP_TIMER_END_OF_TRACK &&
                                            kotlin.math.abs(rem - durationMs) < 90_000
                                    }
                                    else -> false
                                }
                                Button(
                                    onClick = {
                                        if (durationMs == null) onCancelSleepTimer()
                                        else if (durationMs == -1L) onStartSleepTimer(0L, true)
                                        else onStartSleepTimer(durationMs, false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fade out last 1 min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = sleepTimerFadeLastMinute,
                        onCheckedChange = onSleepTimerFadeChange
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEqualizerClick)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bass, treble, presets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Open on launch",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        DEFAULT_TAB_OPTIONS.take(2),
                        DEFAULT_TAB_OPTIONS.drop(2)
                    ).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { (value, label) ->
                                val selected = defaultTab == value
                                Button(
                                    onClick = { onDefaultTabChange(value) },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Download only on Wi‑Fi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = wifiOnlyDownloads,
                        onCheckedChange = onWifiOnlyDownloadsChange
                    )
                }
                Text(
                    text = "When on, downloads won't start on cellular. Connect to Wi‑Fi to download.",
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
                Text(
                    text = "Accessibility",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reduce motion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = onReduceMotionChange
                    )
                }
                Text(
                    text = "Minimize or disable animations for a simpler experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "High contrast",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = highContrast,
                        onCheckedChange = onHighContrastChange
                    )
                }
                Text(
                    text = "Use higher contrast colors for better visibility.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            onDismiss()
                            onShowTutorial()
                        })
                        .padding(top = 24.dp, bottom = 12.dp, start = 0.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show tutorial",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Walk through how to use the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
