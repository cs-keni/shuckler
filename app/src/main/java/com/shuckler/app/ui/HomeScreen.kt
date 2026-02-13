package com.shuckler.app.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onPlaylistSelected: (Playlist) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val downloadManager = LocalDownloadManager.current
    val playlistManager = LocalPlaylistManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()

    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    val recentlyPlayed = completedTracks
        .filter { it.lastPlayedMs > 0 }
        .sortedByDescending { it.lastPlayedMs }
        .take(8)
    val quickPicks = completedTracks
        .filter { it.isFavorite }
        .shuffled()
        .take(8)
    val displayTracks = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else quickPicks

    val recentPlaylists = playlists
        .sortedByDescending { p -> allEntries.filter { it.playlistId == p.id }.maxOfOrNull { it.position } ?: 0 }
        .take(8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "Home",
            onSettingsClick = onSettingsClick
        )

        val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (recentPlaylists.isNotEmpty()) {
            Text(
                text = "Your playlists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentPlaylists, key = { it.id }) { playlist ->
                    val entries = allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
                    val firstTrack = entries.firstOrNull()?.let { e ->
                        completedTracks.find { it.id == e.trackId }
                    }
                    PlaylistShortcutCard(
                        playlist = playlist,
                        firstTrack = firstTrack,
                        onClick = { onPlaylistSelected(playlist) }
                    )
                }
            }
        }

        Text(
            text = if (recentlyPlayed.isNotEmpty()) "Recently played" else "Quick picks",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (displayTracks.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayTracks, key = { it.id }) { track ->
                    TrackShortcutCard(
                        track = track,
                        onPlay = {
                            val items = displayTracks.map { t ->
                                QueueItem(
                                    uri = Uri.fromFile(File(t.filePath)).toString(),
                                    title = t.title,
                                    artist = t.artist,
                                    trackId = t.id,
                                    thumbnailUrl = t.thumbnailUrl
                                )
                            }
                            val idx = displayTracks.indexOf(track).coerceAtLeast(0)
                            viewModel.playTrackWithQueue(items, idx)
                            downloadManager.incrementPlayCount(track.id)
                        }
                    )
                }
            }
        } else {
            Text(
                text = "Play some music to see your recently played or add favorites for quick picks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun PlaylistShortcutCard(
    playlist: Playlist,
    firstTrack: DownloadedTrack?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (playlist.coverImagePath != null && java.io.File(playlist.coverImagePath).exists()) {
                AsyncImage(
                    model = java.io.File(playlist.coverImagePath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (firstTrack?.thumbnailUrl != null) {
                AsyncImage(
                    model = firstTrack.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TrackShortcutCard(
    track: DownloadedTrack,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
