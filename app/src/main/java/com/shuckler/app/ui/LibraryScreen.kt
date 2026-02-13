package com.shuckler.app.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    initialPlaylistToOpen: Playlist? = null,
    onClearInitialPlaylist: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
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
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(false) }
    var playlistSort by remember { mutableStateOf(LibraryPlaylistSort.ALPHABETICAL) }
    val filteredTracks = remember(completedTracks, libraryFilter, searchQuery) {
        val byFilter = when (libraryFilter) {
            LibraryFilter.ALL -> completedTracks
            LibraryFilter.FAVORITES -> completedTracks.filter { it.isFavorite }
        }
        val query = searchQuery.trim()
        if (query.isEmpty()) byFilter
        else byFilter.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }
    var storageUsed by remember { mutableStateOf(0L) }
    var storageAvailable by remember { mutableStateOf(0L) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistTrack by remember { mutableStateOf<DownloadedTrack?>(null) }
    val playlistManager = LocalPlaylistManager.current
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(downloads) {
        storageUsed = withContext(Dispatchers.IO) { downloadManager.getTotalStorageUsed() }
        storageAvailable = withContext(Dispatchers.IO) { downloadManager.getAvailableSpace() }
    }

    LaunchedEffect(initialPlaylistToOpen) {
        if (initialPlaylistToOpen != null) {
            selectedPlaylist = initialPlaylistToOpen
            onClearInitialPlaylist()
        }
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

    if (selectedPlaylist != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaylistDetailScreen(
                playlist = selectedPlaylist!!,
                onBack = { selectedPlaylist = null },
                onPlaylistUpdated = { selectedPlaylist = it }
            )
        }
        return
    }

    if (showCreatePlaylistDialog) {
        CreateEditPlaylistDialog(
            playlist = null,
            onDismiss = { showCreatePlaylistDialog = false },
            onSave = {
                showCreatePlaylistDialog = false
                scope.launch { snackbarHostState.showSnackbar("Playlist \"${it.name}\" created") }
            }
        )
    }

    showAddToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(
            track = track,
            onDismiss = { showAddToPlaylistTrack = null },
            onAdded = { scope.launch { snackbarHostState.showSnackbar("Added to playlist") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Library",
            onSettingsClick = onSettingsClick,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSearchField = !showSearchField }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create playlist")
                    }
                }
            }
        )
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search playlists & tracks") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        val filteredPlaylists = remember(playlists, searchQuery, playlistSort, allEntries, completedTracks) {
            val q = searchQuery.trim()
            val filtered = if (q.isEmpty()) playlists else playlists.filter { it.name.contains(q, ignoreCase = true) }
            when (playlistSort) {
                LibraryPlaylistSort.ALPHABETICAL -> filtered.sortedBy { it.name.lowercase() }
                LibraryPlaylistSort.RECENTLY_PLAYED -> filtered.sortedByDescending { p ->
                    allEntries.filter { it.playlistId == p.id }
                        .mapNotNull { e -> completedTracks.find { it.id == e.trackId }?.lastPlayedMs ?: 0L }
                        .maxOrNull() ?: 0L
                }
            }
        }
        if (filteredPlaylists.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    label = "A–Z",
                    selected = playlistSort == LibraryPlaylistSort.ALPHABETICAL,
                    onClick = { playlistSort = LibraryPlaylistSort.ALPHABETICAL }
                )
                FilterChip(
                    label = "Recently played",
                    selected = playlistSort == LibraryPlaylistSort.RECENTLY_PLAYED,
                    onClick = { playlistSort = LibraryPlaylistSort.RECENTLY_PLAYED }
                )
            }
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyRow(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPlaylists, key = { it.id }) { playlist ->
                    val entries = allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
                    PlaylistCard(
                        playlist = playlist,
                        tracks = completedTracks,
                        entries = entries,
                        onClick = { selectedPlaylist = playlist }
                    )
                }
            }
        }
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { downloadsExpanded = !downloadsExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (downloadsExpanded) "Storage & downloads" else "Show storage & downloads",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (downloadsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (downloadsExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(
            visible = downloadsExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    text = when {
                        searchQuery.isNotBlank() -> "No tracks match \"$searchQuery\". Try a different search or clear it."
                        libraryFilter == LibraryFilter.FAVORITES -> "No favorites yet. Tap the heart on a track to add it."
                        else -> "No downloaded tracks yet. Use Search to download from YouTube or an MP3 URL."
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
                    fun trackToQueueItem(t: DownloadedTrack) = QueueItem(
                        uri = Uri.fromFile(File(t.filePath)).toString(),
                        title = t.title,
                        artist = t.artist,
                        trackId = t.id,
                        thumbnailUrl = t.thumbnailUrl
                    )
                    LibraryTrackItem(
                        track = track,
                        modifier = Modifier.animateItem(),
                        onPlayClick = {
                            downloadManager.incrementPlayCount(track.id)
                            val queueItems = filteredTracks.map { trackToQueueItem(it) }
                            val index = filteredTracks.indexOf(track).coerceAtLeast(0)
                            viewModel.playTrackWithQueue(queueItems, index)
                        },
                        onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                        onDeleteClick = { downloadManager.deleteTrack(track.id) },
                        onPlayNextClick = {
                            viewModel.addToQueueNext(trackToQueueItem(it))
                            scope.launch { snackbarHostState.showSnackbar("Playing next") }
                        },
                        onAddToQueueClick = {
                            viewModel.addToQueueEnd(trackToQueueItem(it))
                            scope.launch { snackbarHostState.showSnackbar("Added to queue") }
                        },
                        onAddToPlaylistClick = { showAddToPlaylistTrack = it }
                    )
                }
            }
        }
    }
    }
}

private enum class LibraryFilter { ALL, FAVORITES }

private enum class LibraryPlaylistSort { ALPHABETICAL, RECENTLY_PLAYED }

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
private fun PlaylistCard(
    playlist: Playlist,
    tracks: List<DownloadedTrack>,
    entries: List<com.shuckler.app.playlist.PlaylistEntry>,
    onClick: () -> Unit
) {
    val firstTrack = entries.firstOrNull()?.let { e -> tracks.find { it.id == e.trackId } }
    Column(
        modifier = Modifier
            .width(116.dp)
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (playlist.coverImagePath != null && java.io.File(playlist.coverImagePath).exists()) {
                    coil.compose.AsyncImage(
                        model = java.io.File(playlist.coverImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (firstTrack?.thumbnailUrl != null) {
                    coil.compose.AsyncImage(
                        model = firstTrack.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun LibraryTrackItem(
    track: DownloadedTrack,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayNextClick: (DownloadedTrack) -> Unit = {},
    onAddToQueueClick: (DownloadedTrack) -> Unit = {},
    onAddToPlaylistClick: (DownloadedTrack) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Queue options"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play next") },
                            onClick = {
                                onPlayNextClick(track)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to queue") },
                            onClick = {
                                onAddToQueueClick(track)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            onClick = {
                                onAddToPlaylistClick(track)
                                menuExpanded = false
                            }
                        )
                    }
                }
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
