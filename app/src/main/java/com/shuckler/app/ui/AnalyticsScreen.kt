package com.shuckler.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.shuckler.app.ui.theme.Border
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

    val streakDays = remember(completed) {
        val dayMs = 86_400_000L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        var streak = 0
        for (daysAgo in 0..29) {
            val start = todayStart - daysAgo * dayMs
            if (completed.any { it.lastPlayedMs in start until start + dayMs }) streak++ else break
        }
        streak
    }

    val totalPlayCount = filteredTracks.sumOf { it.playCount }
    val favoriteCount = filteredTracks.count { it.isFavorite }
    val topPlayed = filteredTracks.sortedByDescending { it.playCount }.take(8)
    val maxPlays = topPlayed.maxOfOrNull { it.playCount } ?: 1
    val topArtists = filteredTracks
        .filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .map { (artist, tracks) ->
            val plays = tracks.sumOf { it.playCount }
            artist to if (plays > 0) plays else tracks.size
        }
        .sortedByDescending { it.second }
        .take(8)
    val maxArtistScore = topArtists.maxOfOrNull { it.second } ?: 1

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
        StatsTopBar(onSettingsClick = onSettingsClick)
        val personality = personalityManager.computePersonality()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAccentColor.current.copy(alpha = 0.11f))
                .border(1.dp, LocalAccentColor.current.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
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
                    color = Text3
                )
                Text(
                    text = personality.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Text1
                )
                Text(
                    text = personality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsTimeRange.entries.forEach { tr ->
                AnalyticsChip(
                    label = tr.label,
                    selected = timeRange == tr,
                    onClick = { timeRange = tr }
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
        if (streakDays > 0) {
            val streakAccent = LocalAccentColor.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(streakAccent.copy(alpha = 0.12f))
                    .border(1.dp, streakAccent.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$streakDays-day streak",
                        style = MaterialTheme.typography.titleSmall,
                        color = Text1
                    )
                    Text(
                        text = if (streakDays >= 7) "Week complete — legendary!" else "Keep it going",
                        style = MaterialTheme.typography.labelSmall,
                        color = Text3
                    )
                }
            }
        }
        if (filteredTracks.any { it.playCount > 0 }) {
            Text(
                text = "Play Distribution",
                style = MaterialTheme.typography.headlineSmall,
                color = Text1,
                modifier = Modifier.padding(top = 8.dp)
            )
            DonutChart(tracks = filteredTracks)
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
        var showAllAchievements by remember { mutableStateOf(false) }
        val visibleBadges = if (showAllAchievements) badges else badges.take(cols)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visibleBadges.chunked(cols).forEach { rowBadges ->
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
            if (badges.size > cols) {
                androidx.compose.material3.TextButton(
                    onClick = { showAllAchievements = !showAllAchievements },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showAllAchievements) "Show less" else "Show all ${badges.size}",
                        color = com.shuckler.app.ui.theme.Text3,
                        style = MaterialTheme.typography.labelMedium
                    )
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
        if (topArtists.isNotEmpty()) {
            Text(
                text = "Top artists",
                style = MaterialTheme.typography.headlineSmall,
                color = Text1,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topArtists.forEachIndexed { index, (artist, score) ->
                    BarChartRow(
                        rank = index + 1,
                        label = artist,
                        value = score,
                        maxValue = maxArtistScore
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
private fun StatsTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Stats",
                style = MaterialTheme.typography.headlineLarge,
                color = Text1
            )
            Text(
                text = "Your listening, visible.",
                style = MaterialTheme.typography.bodySmall,
                color = Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        androidx.compose.material3.IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Text2
            )
        }
    }
}

@Composable
private fun AnalyticsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) LocalAccentColor.current.copy(alpha = 0.18f) else SurfaceElevated.copy(alpha = 0.56f))
            .border(
                width = 1.dp,
                color = if (selected) LocalAccentColor.current.copy(alpha = 0.7f) else Border,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) LocalAccentColor.current else Text2
        )
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
                    LocalAccentColor.current.copy(alpha = 0.14f)
                else
                    SurfaceElevated.copy(alpha = 0.56f)
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
                Text1
            else
                Text3,
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
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Text3
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
                .background(SurfaceElevated)
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
                        tint = Text3
                    )
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "$playCount plays",
            style = MaterialTheme.typography.labelSmall,
            color = Text3
        )
    }
}

@Composable
private fun DonutChart(
    tracks: List<DownloadedTrack>,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

    val artistSlices = remember(tracks) {
        tracks
            .filter { it.artist.isNotBlank() && it.playCount > 0 }
            .groupBy { it.artist }
            .map { (artist, list) -> artist to list.sumOf { it.playCount } }
            .sortedByDescending { it.second }
    }

    if (artistSlices.isEmpty()) return

    val totalPlays = artistSlices.sumOf { it.second }
    val top5 = artistSlices.take(5)
    val othersPlays = totalPlays - top5.sumOf { it.second }

    val sliceAlphas = listOf(1.0f, 0.75f, 0.55f, 0.38f, 0.24f)
    val slices = top5.mapIndexed { i, (artist, plays) ->
        Triple(artist, plays, accentColor.copy(alpha = sliceAlphas[i]))
    } + if (othersPlays > 0) listOf(Triple("Others", othersPlays, Text3.copy(alpha = 0.4f))) else emptyList()

    val sweepAngles = slices.map { (_, plays, _) -> 360f * plays / totalPlays }

    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatable.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    val progress = animatable.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(148.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 30.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                var startAngle = -90f
                slices.forEachIndexed { i, (_, _, color) ->
                    val sweep = (sweepAngles[i] * progress).coerceAtLeast(0f)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngles[i]
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalPlays",
                    style = MaterialTheme.typography.titleMedium,
                    color = Text1
                )
                Text(
                    text = "plays",
                    style = MaterialTheme.typography.labelSmall,
                    color = Text3
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { (artist, plays, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = Text2,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${((100f * plays / totalPlays) + 0.5f).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Text3
                    )
                }
            }
        }
    }
}
