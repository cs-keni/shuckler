package com.shuckler.app.ui

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.preview.PreviewPlayer
import com.shuckler.app.recommendation.RecommendationEngine
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.BorderSubtle
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val mostPlayedTrack = remember(completedTracks.size) {
        completedTracks.filter { it.playCount > 0 }.maxByOrNull { it.playCount }
    }

    val shuffleable = downloadManager.filterForShuffle(completedTracks)
    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    val throwbacks = shuffleable.filter { it.lastPlayedMs < thirtyDaysAgo || it.lastPlayedMs == 0L }

    val surpriseThumbnail = remember(completedTracks.size) {
        downloadManager.filterForShuffle(completedTracks).randomOrNull()?.thumbnailUrl
    }
    val throwbackThumbnail = remember(completedTracks.size) {
        val tb = downloadManager.filterForShuffle(completedTracks)
            .filter { it.lastPlayedMs < System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 || it.lastPlayedMs == 0L }
        (if (tb.isNotEmpty()) tb else downloadManager.filterForShuffle(completedTracks)).randomOrNull()?.thumbnailUrl
    }

    fun playSingleTrack(track: DownloadedTrack) {
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
    }

    val continueTrack = recentlyPlayed.firstOrNull()
        ?: mostPlayedTrack
        ?: completedTracks.maxByOrNull { it.downloadDateMs }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 132.dp)
    ) {
        val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        HomeTopBar(
            title = "Shuckler",
            subtitle = when {
                completedTracks.isEmpty() -> "$greeting. Start with a search."
                completedTracks.size == 1 -> "$greeting. 1 song in your room."
                else -> "$greeting. ${completedTracks.size} songs in your room."
            },
            onSettingsClick = onSettingsClick
        )

        ContinueListeningBand(
            track = continueTrack,
            hasTracks = completedTracks.isNotEmpty(),
            onPlay = { track -> playSingleTrack(track) },
            onSearch = { onSearchQuerySelected("") }
        )

        LibrarySnapshot(
            trackCount = completedTracks.size,
            playlistCount = playlists.size,
            recentCount = recentlyPlayed.size
        )

        if (completedTracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionCard(
                    label = "Surprise me",
                    thumbnailUrl = surpriseThumbnail,
                    containerColor = SurfaceElevated,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val track = shuffleable.randomOrNull() ?: return@QuickActionCard
                        playSingleTrack(track)
                    }
                )
                QuickActionCard(
                    label = "Throwback",
                    thumbnailUrl = throwbackThumbnail,
                    containerColor = SurfaceElevated,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val track = if (throwbacks.isNotEmpty()) throwbacks.random()
                        else shuffleable.randomOrNull() ?: return@QuickActionCard
                        playSingleTrack(track)
                    }
                )
            }
        }

        if (recentSearches.isNotEmpty()) {
            SectionTitle("Recent searches")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentSearches, key = { it }) { query ->
                    FlowActionChip(
                        label = query,
                        onClick = { onSearchQuerySelected(query) }
                    )
                }
            }
        }

        if (RecommendationEngine.hasRecommendationData(context, completedTracks)) {
            SectionTitle("Recommended for you")
            if (recommendedLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalAccentColor.current
                    )
                    Text(
                        text = "Finding recommendations…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text2,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else if (recommendedResults.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(recommendedResults, key = { index, result -> "${result.url}-$index" }) { index, result ->
                        var visible by remember { mutableStateOf(false) }
                        val alpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "recAlpha"
                        )
                        val offsetY by animateFloatAsState(
                            targetValue = if (visible) 0f else 20f,
                            animationSpec = tween(durationMillis = 300),
                            label = "recOffsetY"
                        )
                        LaunchedEffect(Unit) {
                            delay(index * 60L)
                            visible = true
                        }

                        val isDownloaded = completedTracks.any { it.sourceUrl == result.url }
                        Box(
                            modifier = Modifier
                                .alpha(alpha)
                                .offset(y = offsetY.dp)
                        ) {
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
        }

        if (recentPlaylists.isNotEmpty()) {
            SectionTitle("Your playlists")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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

        SectionTitle(if (recentlyPlayed.isNotEmpty()) "Recently played" else "Quick picks")
        if (displayTracks.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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
                color = Text2,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = Text1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeTopBar(
    title: String,
    subtitle: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Text1,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Text2
            )
        }
    }
}

@Composable
private fun LibrarySnapshot(
    trackCount: Int,
    playlistCount: Int,
    recentCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SnapshotStat(label = "Tracks", value = trackCount.toString(), modifier = Modifier.weight(1f))
            SnapshotStat(label = "Playlists", value = playlistCount.toString(), modifier = Modifier.weight(1f))
            SnapshotStat(label = "Recent", value = recentCount.toString(), modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
    }
}

@Composable
private fun SnapshotStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = LocalAccentColor.current,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Text3,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueListeningBand(
    track: DownloadedTrack?,
    hasTracks: Boolean,
    onPlay: (DownloadedTrack) -> Unit,
    onSearch: () -> Unit
) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (track != null) 174.dp else 124.dp)
            .clickable {
                if (track != null) onPlay(track) else onSearch()
            }
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.26f),
                        SurfaceElevated.copy(alpha = 0.86f),
                        Base
                    )
                )
            )
    ) {
        if (track?.thumbnailUrl != null) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .alpha(0.92f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Base.copy(alpha = 0.98f)),
                        startY = 0f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = if (track != null) 132.dp else 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = if (track != null) "Continue listening" else if (hasTracks) "Ready when you are" else "Start with a song",
                style = MaterialTheme.typography.labelSmall,
                color = Text3
            )
            Text(
                text = track?.title ?: if (hasTracks) "Search, import, or shuffle from your library." else "Your library is quiet.",
                style = MaterialTheme.typography.titleMedium,
                color = Text1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = track?.artist?.takeIf { it.isNotBlank() }
                    ?: if (hasTracks) "Build the next queue." else "Search for something and download it to start.",
                style = MaterialTheme.typography.bodySmall,
                color = Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Text1)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (track != null) "PLAY" else "SEARCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = Base
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    thumbnailUrl: String?,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "quickActionScale"
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Border, RoundedCornerShape(999.dp))
            .background(containerColor.copy(alpha = 0.62f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FlowActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Border, RoundedCornerShape(999.dp))
            .background(SurfaceElevated.copy(alpha = 0.54f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistShortcutCard(
    playlist: Playlist,
    firstTrack: DownloadedTrack?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onClick)
    ) {
        if (playlist.coverImagePath != null && java.io.File(playlist.coverImagePath).exists()) {
            AsyncImage(
                model = java.io.File(playlist.coverImagePath),
                contentDescription = null,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else if (firstTrack?.thumbnailUrl != null) {
            AsyncImage(
                model = firstTrack.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            )
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

@Composable
private fun TrackShortcutCard(
    track: DownloadedTrack,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onPlay)
    ) {
        if (track.thumbnailUrl != null) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            )
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
        if (track.artist.isNotBlank()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall,
                color = Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
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
        modifier = Modifier.size(width = 160.dp, height = 200.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model = result.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceElevated)
                )
            }

            // Gradient overlay — transparent at 30%, full black at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.3f to Color.Transparent,
                                1.0f to Base.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Text1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!result.uploaderName.isNullOrBlank()) {
                    Text(
                        text = result.uploaderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPreviewing) {
                        IconButton(onClick = onStopPreviewClick) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop preview",
                                modifier = Modifier.size(18.dp),
                                tint = Text1
                            )
                        }
                    } else {
                        IconButton(onClick = onPlayClick, enabled = !isDownloading && !isStreaming) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = if (isStreaming) "Loading…" else "Play",
                                modifier = Modifier.size(18.dp),
                                tint = Text1
                            )
                        }
                        IconButton(onClick = onPreviewClick, enabled = !isDownloading) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Preview 60s",
                                modifier = Modifier.size(14.dp),
                                tint = Text2
                            )
                        }
                    }
                    IconButton(onClick = onDownloadClick, enabled = !isDownloading && !isDownloaded) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp),
                            tint = Text1
                        )
                    }
                }
            }
        }
    }
}
