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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.shuckler.app.ui.LocalOnWifiOnlyBlocked
import com.shuckler.app.ui.LocalSnackbarHostState
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.preview.PreviewPlayer
import com.shuckler.app.recommendation.RecommendationEngine
import com.shuckler.app.ui.SearchPreferences
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onPlaylistSelected: (Playlist) -> Unit,
    onSearchQuerySelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadManager = LocalDownloadManager.current
    val onWifiOnlyBlocked = LocalOnWifiOnlyBlocked.current
    val snackbarHostState = LocalSnackbarHostState.current
    val playlistManager = LocalPlaylistManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val recentSearches = SearchPreferences.getRecentSearches(context)
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()

    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    var recommendedResults by remember { mutableStateOf<List<YouTubeSearchResult>>(emptyList()) }
    var recommendedLoading by remember { mutableStateOf(false) }
    val previewingVideoUrl by PreviewPlayer.previewingVideoUrl.collectAsState(initial = null)
    var downloadingVideoUrl by remember { mutableStateOf<String?>(null) }
    var streamingVideoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(completedTracks) {
        if (RecommendationEngine.hasRecommendationData(context, completedTracks)) {
            recommendedLoading = true
            recommendedResults = RecommendationEngine.fetchRecommendedYouTubeResults(context, completedTracks)
            recommendedLoading = false
        } else {
            recommendedResults = emptyList()
        }
    }
    val recentlyPlayed = completedTracks
        .filter { it.lastPlayedMs > 0 }
        .sortedByDescending { it.lastPlayedMs }
        .take(50)
    val quickPicks = downloadManager.filterForShuffle(completedTracks.filter { it.isFavorite })
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

        if (completedTracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val shuffleable = downloadManager.filterForShuffle(completedTracks)
                        val track = shuffleable.randomOrNull() ?: return@Button
                        val items = listOf(
                            QueueItem(
                                uri = Uri.fromFile(File(track.filePath)).toString(),
                                title = track.title,
                                artist = track.artist,
                                trackId = track.id,
                                thumbnailUrl = track.thumbnailUrl,
                                startMs = track.startMs,
                                endMs = track.endMs
                            )
                        )
                        viewModel.playTrackWithQueue(items, 0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text("Surprise me")
                }
                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val shuffleable = downloadManager.filterForShuffle(completedTracks)
                val throwbacks = shuffleable.filter { it.lastPlayedMs < thirtyDaysAgo || it.lastPlayedMs == 0L }
                Button(
                    onClick = {
                        val track = if (throwbacks.isNotEmpty()) throwbacks.random()
                        else shuffleable.randomOrNull() ?: return@Button
                        val items = listOf(
                            QueueItem(
                                uri = Uri.fromFile(File(track.filePath)).toString(),
                                title = track.title,
                                artist = track.artist,
                                trackId = track.id,
                                thumbnailUrl = track.thumbnailUrl,
                                startMs = track.startMs,
                                endMs = track.endMs
                            )
                        )
                        viewModel.playTrackWithQueue(items, 0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Throwback")
                }
            }
        }

        if (recentSearches.isNotEmpty()) {
            Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentSearches, key = { it }) { query ->
                    Card(
                        modifier = Modifier
                            .clickable { onSearchQuerySelected(query) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (RecommendationEngine.hasRecommendationData(context, completedTracks)) {
            Text(
                text = "Recommended for you",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (recommendedLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Finding recommendations…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else if (recommendedResults.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommendedResults, key = { it.url }) { result ->
                        val isDownloaded = completedTracks.any { it.sourceUrl == result.url }
                        RecommendedYouTubeCard(
                            result = result,
                            isDownloading = downloadingVideoUrl == result.url,
                            isStreaming = streamingVideoUrl == result.url,
                            isPreviewing = previewingVideoUrl == result.url,
                            isDownloaded = isDownloaded,
                            onPlayClick = {
                                if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                                streamingVideoUrl = result.url
                                scope.launch {
                                    val audio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                    when (audio) {
                                        is YouTubeRepository.AudioStreamResult.Success -> {
                                            viewModel.playTrack(
                                                Uri.parse(audio.info.url),
                                                result.title,
                                                result.uploaderName ?: "Unknown",
                                                thumbnailUrl = result.thumbnailUrl
                                            )
                                        }
                                        is YouTubeRepository.AudioStreamResult.Failure -> {
                                            snackbarHostState?.let { host ->
                                                scope.launch {
                                                    val r = host.showSnackbar("Couldn't play — check connection", actionLabel = "Retry", duration = SnackbarDuration.Short)
                                                    if (r == SnackbarResult.ActionPerformed) {
                                                        streamingVideoUrl = result.url
                                                        val retryAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                                        when (retryAudio) {
                                                            is YouTubeRepository.AudioStreamResult.Success -> viewModel.playTrack(
                                                                Uri.parse(retryAudio.info.url), result.title, result.uploaderName ?: "Unknown", thumbnailUrl = result.thumbnailUrl
                                                            )
                                                            is YouTubeRepository.AudioStreamResult.Failure -> { /* already showed snackbar */ }
                                                        }
                                                        streamingVideoUrl = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    streamingVideoUrl = null
                                }
                            },
                            onPreviewClick = {
                                scope.launch {
                                    val audio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                    when (audio) {
                                        is YouTubeRepository.AudioStreamResult.Success ->
                                            PreviewPlayer.play(context, result.url, audio.info.url)
                                        is YouTubeRepository.AudioStreamResult.Failure ->
                                            snackbarHostState?.let { host ->
                                                scope.launch { host.showSnackbar("Couldn't play — check connection", actionLabel = "Retry", duration = SnackbarDuration.Short) }
                                            }
                                    }
                                }
                            },
                            onStopPreviewClick = { PreviewPlayer.stop() },
                            onDownloadClick = {
                                if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                                val id = downloadManager.startDownloadFromYouTube(
                                    result.url,
                                    result.title,
                                    result.uploaderName ?: "",
                                    result.thumbnailUrl,
                                    onWifiOnlyBlocked = onWifiOnlyBlocked
                                )
                                if (id.isNotEmpty()) {
                                    downloadingVideoUrl = result.url
                                    scope.launch {
                                        kotlinx.coroutines.delay(500)
                                        if (downloadingVideoUrl == result.url) downloadingVideoUrl = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

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
                                    thumbnailUrl = t.thumbnailUrl,
                                    startMs = t.startMs,
                                    endMs = t.endMs
                                )
                            }
                            val idx = displayTracks.indexOf(track).coerceAtLeast(0)
                            viewModel.playTrackWithQueue(items, idx)
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

@Composable
private fun RecommendedYouTubeCard(
    result: YouTubeSearchResult,
    isDownloading: Boolean,
    isStreaming: Boolean,
    isPreviewing: Boolean,
    isDownloaded: Boolean,
    onPlayClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onStopPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(width = 160.dp, height = 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model = result.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            Text(
                text = result.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPreviewing) {
                    IconButton(onClick = onStopPreviewClick) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop preview", modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(onClick = onPlayClick, enabled = !isDownloading && !isStreaming) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = if (isStreaming) "Loading…" else "Play (stream)",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onPreviewClick, enabled = !isDownloading) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview 60s", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onDownloadClick, enabled = !isDownloading && !isDownloaded) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
