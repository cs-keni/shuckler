package com.shuckler.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import com.shuckler.app.achievement.AchievementBadge
import com.shuckler.app.achievement.AchievementDefinitions
import com.shuckler.app.achievement.LocalAchievementManager
import com.shuckler.app.personality.LocalListeningPersonalityManager
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import java.util.Calendar

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
    val achievementManager = LocalAchievementManager.current
    val personalityManager = LocalListeningPersonalityManager.current
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

    LaunchedEffect(completed, playlists, totalPlayCount) {
        achievementManager.checkAndUnlock(completed, playlists, totalPlayCount)
    }

    val unlockedIds = achievementManager.getUnlockedIds()

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
        ScreenHeader(title = "Stats", onSettingsClick = onSettingsClick)
        val personality = personalityManager.computePersonality()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = personality.emoji,
                    style = MaterialTheme.typography.headlineLarge
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Listening personality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = personality.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = personality.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
        WeeklyChart(completedTracks = completed)
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.headlineSmall,
            color = Text1,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )
        val badges = AchievementDefinitions.ALL
        val cols = 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            badges.chunked(cols).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowBadges.forEach { badge ->
                        AchievementBadgeCard(
                            badge = badge,
                            unlocked = unlockedIds.contains(badge.id)
                        )
                    }
                }
            }
        }
        if (topPlayed.isNotEmpty()) {
            Text(
                text = "Most played",
                style = MaterialTheme.typography.headlineSmall,
                color = Text1,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
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
                style = MaterialTheme.typography.headlineSmall,
                color = Text1,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
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
private fun RowScope.AchievementBadgeCard(badge: AchievementBadge, unlocked: Boolean) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (unlocked)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = badge.emoji,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = badge.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeeklyChart(completedTracks: List<DownloadedTrack>) {
    val accentColor = LocalAccentColor.current
    val now = remember { System.currentTimeMillis() }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dayMs = 86_400_000L
    // Index 0 = 6 days ago, index 6 = today
    val buckets = (0..6).map { daysAgo ->
        val start = todayStart - (6 - daysAgo) * dayMs
        val end = start + dayMs
        val count = completedTracks.count { it.lastPlayedMs in start until end }
        daysAgo to count
    }
    val maxCount = buckets.maxOfOrNull { it.second }.takeIf { it != null && it > 0 } ?: 1
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

    Text(
        text = "This Week",
        style = MaterialTheme.typography.headlineSmall,
        color = Text1,
        modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEach { (daysAgo, count) ->
            val isToday = daysAgo == 6
            val targetFraction = count.toFloat() / maxCount
            val fillFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(600),
                label = "wc_$daysAgo"
            )
            val barColor = if (isToday) accentColor else accentColor.copy(alpha = 0.45f)
            val dowIndex = (todayDow - (6 - daysAgo) + 7) % 7
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (count > 0) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((72 * fillFraction).coerceAtLeast(2f).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
                Text(
                    text = dayLabels[dowIndex],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) accentColor else Text3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String) {
    val accentColor = LocalAccentColor.current
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Text2
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
    val accentColor = LocalAccentColor.current
    val fillFraction by animateFloatAsState(
        targetValue = (value.toFloat() / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "barFill"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelSmall,
            color = Text3,
            modifier = Modifier.width(20.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = Text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelMedium,
            color = accentColor
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
