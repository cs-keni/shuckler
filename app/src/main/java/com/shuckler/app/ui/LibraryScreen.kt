package com.shuckler.app.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val downloadManager = LocalDownloadManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    var libraryFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    val filteredTracks = remember(completedTracks, libraryFilter) {
        when (libraryFilter) {
            LibraryFilter.ALL -> completedTracks
            LibraryFilter.FAVORITES -> completedTracks.filter { it.isFavorite }
        }
    }
    var storageUsed by remember { mutableStateOf(0L) }
    var storageAvailable by remember { mutableStateOf(0L) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(downloads) {
        storageUsed = withContext(Dispatchers.IO) { downloadManager.getTotalStorageUsed() }
        storageAvailable = withContext(Dispatchers.IO) { downloadManager.getAvailableSpace() }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear all downloads?") },
            text = { Text("This will delete all downloaded tracks and free storage. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        downloadManager.clearAllDownloads()
                        showClearAllConfirm = false
                    }
                ) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    label = "All",
                    selected = libraryFilter == LibraryFilter.ALL,
                    onClick = { libraryFilter = LibraryFilter.ALL }
                )
                FilterChip(
                    label = "Favorites",
                    selected = libraryFilter == LibraryFilter.FAVORITES,
                    onClick = { libraryFilter = LibraryFilter.FAVORITES }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Used: ${formatBytes(storageUsed)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Free: ${formatBytes(storageAvailable)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (completedTracks.isNotEmpty()) {
            TextButton(onClick = { showClearAllConfirm = true }) {
                Text("Clear all downloads", color = MaterialTheme.colorScheme.error)
            }
        }
        if (filteredTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (libraryFilter) {
                        LibraryFilter.ALL -> "No downloaded tracks yet. Use Search to download from YouTube or an MP3 URL."
                        LibraryFilter.FAVORITES -> "No favorites yet. Tap the heart on a track to add it."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    LibraryTrackItem(
                        track = track,
                        modifier = Modifier.animateItem(),
                        onPlayClick = {
                            downloadManager.incrementPlayCount(track.id)
                            val queueItems = filteredTracks.map {
                                QueueItem(
                                    uri = Uri.fromFile(File(it.filePath)).toString(),
                                    title = it.title,
                                    artist = it.artist,
                                    trackId = it.id,
                                    thumbnailUrl = it.thumbnailUrl
                                )
                            }
                            val index = filteredTracks.indexOf(track).coerceAtLeast(0)
                            viewModel.playTrackWithQueue(queueItems, index)
                        },
                        onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                        onDeleteClick = { downloadManager.deleteTrack(track.id) }
                    )
                }
            }
        }
    }
}

private enum class LibraryFilter { ALL, FAVORITES }

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected)
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    else
        FilterChipDefaults.filterChipColors()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = colors
    )
}

@Composable
private fun LibraryTrackItem(
    track: DownloadedTrack,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val favoriteScale by animateFloatAsState(
        targetValue = if (track.isFavorite) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "favoriteScale"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (track.durationMs > 0 || track.fileSizeBytes > 0 || track.playCount > 0) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (track.durationMs > 0) {
                            Text(
                                text = formatDurationMs(track.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (track.fileSizeBytes > 0) {
                            Text(
                                text = formatBytes(track.fileSizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (track.playCount > 0) {
                            Text(
                                text = if (track.playCount == 1) "Played once" else "Played ${track.playCount} times",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .padding(4.dp)
                        .scale(favoriteScale)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (track.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable(onClick = onPlayClick)
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return ""
    val totalSec = (ms / 1000).toInt()
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
