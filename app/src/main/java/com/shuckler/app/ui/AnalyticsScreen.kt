package com.shuckler.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist

private enum class AnalyticsTimeRange(val label: String, val ms: Long?) {
    H24("24 hours", 24 * 60 * 60 * 1000L),
    D7("7 days", 7 * 24 * 60 * 60 * 1000L),
    D30("30 days", 30 * 24 * 60 * 60 * 1000L),
    ALL("All time", null)
}

@Composable
fun AnalyticsScreen(onSettingsClick: () -> Unit = {}) {
    val downloadManager = LocalDownloadManager.current
    val playlistManager = LocalPlaylistManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()
    var timeRange by remember { mutableStateOf(AnalyticsTimeRange.ALL) }

    val completed = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    val now = System.currentTimeMillis()
    val cutoffMs = timeRange.ms?.let { now - it } ?: 0L
    val filteredTracks = if (timeRange == AnalyticsTimeRange.ALL) completed
        else completed.filter { it.lastPlayedMs >= cutoffMs }

    val totalPlayCount = filteredTracks.sumOf { it.playCount }
    val favoriteCount = filteredTracks.count { it.isFavorite }
    val topPlayed = filteredTracks.sortedByDescending { it.playCount }.take(8)
    val maxPlays = topPlayed.maxOfOrNull { it.playCount } ?: 1

    val playlistPlayCounts = playlists.map { p ->
        val entries = allEntries.filter { it.playlistId == p.id }
        val sum = entries.sumOf { e ->
            filteredTracks.find { it.id == e.trackId }?.playCount ?: 0
        }
        p to sum
    }.filter { (_, count) -> count > 0 }.sortedByDescending { it.second }.take(8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(title = "Analytics", onSettingsClick = onSettingsClick)
        Text(
            text = "Your listening stats",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsTimeRange.entries.forEach { tr ->
                FilterChip(
                    selected = timeRange == tr,
                    onClick = { timeRange = tr },
                    label = { Text(tr.label) },
                    colors = if (timeRange == tr)
                        FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    else FilterChipDefaults.filterChipColors()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(label = "Tracks", value = "${filteredTracks.size}")
            StatCard(label = "Plays", value = "$totalPlayCount")
            StatCard(label = "Favorites", value = "$favoriteCount")
        }
        if (topPlayed.isNotEmpty()) {
            Text(
                text = "Most played",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topPlayed.forEachIndexed { i, track ->
                    BarChartRow(
                        rank = i + 1,
                        label = track.title,
                        value = track.playCount,
                        maxValue = maxPlays
                    )
                }
            }
        }
        if (playlistPlayCounts.isNotEmpty()) {
            Text(
                text = "Most played playlists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlistPlayCounts, key = { it.first.id }) { (playlist, count) ->
                    val entries = allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
                    val firstTrack = entries.firstOrNull()?.let { e ->
                        completed.find { it.id == e.trackId }
                    }
                    PlaylistStatCard(
                        playlist = playlist,
                        playCount = count,
                        firstTrack = firstTrack
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BarChartRow(
    rank: Int,
    label: String,
    value: Int,
    maxValue: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((value.toFloat() / maxValue).coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun PlaylistStatCard(
    playlist: Playlist,
    playCount: Int,
    firstTrack: DownloadedTrack?
) {
    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when {
                playlist.coverImagePath != null && java.io.File(playlist.coverImagePath).exists() -> {
                    AsyncImage(
                        model = java.io.File(playlist.coverImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                firstTrack?.thumbnailUrl != null -> {
                    AsyncImage(
                        model = firstTrack.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "$playCount plays",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
