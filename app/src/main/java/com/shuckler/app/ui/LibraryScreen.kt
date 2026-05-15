package com.shuckler.app.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.accessibility.LocalAccessibilityPreferences
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.ui.ImportDialog
import com.shuckler.app.util.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.BorderSubtle
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Red
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    initialPlaylistToOpen: Playlist? = null,
    onClearInitialPlaylist: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    savedScrollIndex: Int = 0,
    savedScrollOffset: Int = 0,
    onSaveScrollPosition: (index: Int, offset: Int) -> Unit = { _, _ -> },
    isSheetMode: Boolean = false,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val context = LocalContext.current
    val downloadManager = LocalDownloadManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    var libraryFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var moodFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(false) }
    var playlistSort by remember { mutableStateOf(LibraryPlaylistSort.ALPHABETICAL) }
    var trackSort by remember { mutableStateOf(LibraryTrackSort.DATE_ADDED) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var deleteJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var isGridView by remember { mutableStateOf(false) }
    val filteredTracks = remember(completedTracks, libraryFilter, moodFilter, searchQuery, trackSort, pendingDeleteId) {
        val byFilter = when (libraryFilter) {
            LibraryFilter.ALL -> completedTracks
            LibraryFilter.FAVORITES -> completedTracks.filter { it.isFavorite }
            LibraryFilter.BY_ALBUM -> completedTracks // grouping handled by AlbumGroupedList
        }
        val byMood = if (moodFilter != null) byFilter.filter { it.moodTags.contains(moodFilter) }
        else byFilter
        val query = searchQuery.trim()
        val filtered = if (query.isEmpty()) byMood
        else byMood.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
        val excludingDeleted = filtered.filter { it.id != pendingDeleteId }
        when (trackSort) {
            LibraryTrackSort.DATE_ADDED -> excludingDeleted.sortedByDescending { it.downloadDateMs }
            LibraryTrackSort.TITLE -> excludingDeleted.sortedBy { it.title.lowercase() }
            LibraryTrackSort.ARTIST -> excludingDeleted.sortedBy { it.artist.lowercase() }
            LibraryTrackSort.DURATION -> excludingDeleted.sortedByDescending { it.durationMs }
            LibraryTrackSort.PLAY_COUNT -> excludingDeleted.sortedByDescending { it.playCount }
        }
    }
    var storageUsed by remember { mutableStateOf(0L) }
    var storageAvailable by remember { mutableStateOf(0L) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedAlbumTracks by remember { mutableStateOf<List<DownloadedTrack>?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInitialTab by remember { mutableStateOf(0) }
    var showAddToPlaylistTrack by remember { mutableStateOf<DownloadedTrack?>(null) }
    var showMoodTagTrack by remember { mutableStateOf<DownloadedTrack?>(null) }
    var selectedSmartPlaylist by remember { mutableStateOf<SmartPlaylistType?>(null) }
    var showCleanUpDialog by remember { mutableStateOf(false) }
    var showPlaylistsSheet by remember { mutableStateOf(false) }
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList())
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val currentPlayingTrackId = queueItems.getOrNull((queueInfo.first - 1).coerceIn(0, queueItems.size))?.trackId
    val playlistManager = LocalPlaylistManager.current
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()
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
            LibraryPlaylistSort.MOST_LISTENED -> filtered.sortedByDescending { p ->
                allEntries.filter { it.playlistId == p.id }
                    .sumOf { e -> completedTracks.find { it.id == e.trackId }?.playCount ?: 0 }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val reduceMotion by LocalAccessibilityPreferences.current.reduceMotionFlow.collectAsState(
        initial = LocalAccessibilityPreferences.current.reduceMotion
    )
    val albumGroups = remember(completedTracks) {
        completedTracks
            .groupBy { libraryAlbumGroupKey(it) }
            .map { (key, tracks) ->
                val sortedTracks = tracks.sortedBy { it.title.lowercase() }
                val firstTrack = sortedTracks.first()
                LibraryAlbumGroup(
                    key = key,
                    title = firstTrack.albumTitle?.takeIf { it.isNotBlank() } ?: firstTrack.title,
                    artist = firstTrack.artist,
                    year = firstTrack.albumYear,
                    artworkUrl = firstTrack.thumbnailUrl,
                    tracks = sortedTracks
                )
            }
            .sortedWith(compareByDescending<LibraryAlbumGroup> { it.tracks.maxOfOrNull { track -> track.downloadDateMs } ?: 0L }
                .thenBy { it.title.lowercase() })
    }
    val searchFilteredAlbumGroups = remember(albumGroups, searchQuery) {
        if (searchQuery.isBlank()) albumGroups
        else albumGroups.mapNotNull { group ->
            val matchingTracks = group.tracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
            val groupMatches = group.title.contains(searchQuery, ignoreCase = true) ||
                group.artist.contains(searchQuery, ignoreCase = true)
            when {
                groupMatches -> group
                matchingTracks.isNotEmpty() -> group.copy(tracks = matchingTracks)
                else -> null
            }
        }
    }

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
            containerColor = Surface,
            titleContentColor = Text1,
            textContentColor = Text2,
            title = { Text("Clear all downloads?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("This will delete all downloaded tracks and free storage. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        downloadManager.clearAllDownloads()
                        showClearAllConfirm = false
                    }
                ) {
                    Text("Clear all", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Text2)
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

    selectedAlbumTracks?.let { albumTracks ->
        AlbumDetailScreen(
            tracks = albumTracks,
            currentPlayingTrackId = currentPlayingTrackId,
            onBack = { selectedAlbumTracks = null },
            onPlayTracks = { tracks, index ->
                val items = tracks.map { track ->
                    QueueItem(
                        uri = Uri.fromFile(File(track.filePath)).toString(),
                        title = track.title,
                        artist = track.artist,
                        trackId = track.id,
                        thumbnailUrl = track.thumbnailUrl,
                        startMs = track.startMs,
                        endMs = track.endMs
                    )
                }
                viewModel.playTrackWithQueue(items, index)
            }
        )
        return
    }

    selectedArtist?.let { artist ->
        ArtistDetailScreen(
            artistName = artist,
            tracks = completedTracks.filter { it.artist.equals(artist, ignoreCase = true) },
            currentPlayingTrackId = currentPlayingTrackId,
            onBack = { selectedArtist = null },
            onPlayTracks = { tracks, index ->
                val items = tracks.map { track ->
                    QueueItem(
                        uri = Uri.fromFile(File(track.filePath)).toString(),
                        title = track.title,
                        artist = track.artist,
                        trackId = track.id,
                        thumbnailUrl = track.thumbnailUrl,
                        startMs = track.startMs,
                        endMs = track.endMs
                    )
                }
                viewModel.playTrackWithQueue(items, index)
            }
        )
        return
    }

    if (showImportDialog) {
        ImportDialog(
            initialTab = importInitialTab,
            onDismiss = { showImportDialog = false },
            onImportComplete = { playlist ->
                showImportDialog = false
                playlist?.let { selectedPlaylist = it }
                scope.launch { snackbarHostState.showSnackbar("Import started") }
            }
        )
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
    if (showCleanUpDialog) {
        CleanUpDialog(
            tracks = completedTracks,
            onDismiss = { showCleanUpDialog = false },
            onDelete = { id ->
                downloadManager.deleteTrack(id)
                scope.launch { snackbarHostState.showSnackbar("Removed") }
            }
        )
    }
    showMoodTagTrack?.let { track ->
        MoodTagDialog(
            track = track,
            onDismiss = { showMoodTagTrack = null },
            onSave = { tags ->
                downloadManager.setMoodTags(track.id, tags)
                showMoodTagTrack = null
                scope.launch { snackbarHostState.showSnackbar("Mood tags updated") }
            }
        )
    }
    if (showPlaylistsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Base
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Text1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                filteredPlaylists.forEach { playlist ->
                    val entries = allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
                    PlaylistCard(
                        playlist = playlist,
                        tracks = completedTracks,
                        entries = entries,
                        onClick = {
                            showPlaylistsSheet = false
                            selectedPlaylist = playlist
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                    IconButton(modifier = Modifier.size(36.dp), onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(modifier = Modifier.size(36.dp), onClick = { showSearchField = !showSearchField }) {
                        Icon(Icons.Default.Search, contentDescription = "Search library", modifier = Modifier.size(20.dp))
                    }
                    IconButton(modifier = Modifier.size(36.dp), onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Import playlist", modifier = Modifier.size(20.dp))
                    }
                    IconButton(modifier = Modifier.size(36.dp), onClick = { showCreatePlaylistDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create new playlist", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )

        val importApp = remember(context) { context.applicationContext as? ShucklerApplication }
        val importProgress by remember(importApp) {
            importApp?.spotifyImportManager?.progress ?: kotlinx.coroutines.flow.MutableStateFlow(null)
        }.collectAsState()
        val isImporting by remember(importApp) {
            importApp?.spotifyImportManager?.isImporting ?: kotlinx.coroutines.flow.MutableStateFlow(false)
        }.collectAsState()

        AnimatedVisibility(
            visible = isImporting,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            val progress = importProgress
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalAccentColor.current.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rescuing your music",
                        style = MaterialTheme.typography.labelMedium,
                        color = Text1
                    )
                    if (progress != null && progress.total > 0) {
                        Text(
                            text = "${progress.terminal} of ${progress.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Text2
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                if (progress != null && progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.terminal.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalAccentColor.current,
                        trackColor = LocalAccentColor.current.copy(alpha = 0.15f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalAccentColor.current,
                        trackColor = LocalAccentColor.current.copy(alpha = 0.15f)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }

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
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                label = "All",
                selected = libraryFilter == LibraryFilter.ALL,
                onClick = { libraryFilter = LibraryFilter.ALL; moodFilter = null }
            )
            FilterChip(
                label = "By Album",
                selected = libraryFilter == LibraryFilter.BY_ALBUM,
                onClick = { libraryFilter = LibraryFilter.BY_ALBUM; moodFilter = null }
            )
            FilterChip(
                label = "Favorites",
                selected = libraryFilter == LibraryFilter.FAVORITES,
                onClick = { libraryFilter = LibraryFilter.FAVORITES; moodFilter = null }
            )
            val allMoods = completedTracks.flatMap { it.moodTags }.toSet().sorted()
            allMoods.take(5).forEach { mood ->
                FilterChip(
                    label = mood,
                    selected = moodFilter == mood,
                    onClick = { moodFilter = if (moodFilter == mood) null else mood }
                )
            }
        }
        var showPlaylistSortMenu by remember { mutableStateOf(false) }
        val smartPlaylistTracks = remember(completedTracks, selectedSmartPlaylist) {
            when (selectedSmartPlaylist) {
                SmartPlaylistType.MOST_PLAYED -> completedTracks.sortedByDescending { it.playCount }
                SmartPlaylistType.RECENTLY_ADDED -> completedTracks.sortedByDescending { it.downloadDateMs }
                SmartPlaylistType.NEVER_PLAYED -> completedTracks.filter { it.playCount == 0 }
                SmartPlaylistType.FAVORITES -> completedTracks.filter { it.isFavorite }
                SmartPlaylistType.LONG_SESSIONS -> completedTracks.filter { it.durationMs > 300_000 }.sortedByDescending { it.playCount }
                SmartPlaylistType.HIDDEN_GEMS -> completedTracks.filter { it.playCount in 1..2 }.sortedByDescending { it.downloadDateMs }
                null -> emptyList()
            }
        }
        if (selectedSmartPlaylist != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                SmartPlaylistScreen(
                    type = selectedSmartPlaylist!!,
                    tracks = smartPlaylistTracks,
                    onBack = { selectedSmartPlaylist = null },
                    viewModel = viewModel,
                    downloadManager = downloadManager,
                    currentPlayingTrackId = currentPlayingTrackId,
                    onAddToPlaylist = { showAddToPlaylistTrack = it },
                    onMoodTag = { showMoodTagTrack = it },
                    onArtistClick = { selectedArtist = it },
                    reduceMotion = reduceMotion,
                    trackToQueueItem = { t ->
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
                )
            }
            return@Scaffold
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmartPlaylistType.entries.forEach { type ->
                val count = when (type) {
                    SmartPlaylistType.MOST_PLAYED -> completedTracks.count { it.playCount > 0 }
                    SmartPlaylistType.RECENTLY_ADDED -> completedTracks.size
                    SmartPlaylistType.NEVER_PLAYED -> completedTracks.count { it.playCount == 0 }
                    SmartPlaylistType.FAVORITES -> completedTracks.count { it.isFavorite }
                    SmartPlaylistType.LONG_SESSIONS -> completedTracks.count { it.durationMs > 300_000 }
                    SmartPlaylistType.HIDDEN_GEMS -> completedTracks.count { it.playCount in 1..2 }
                }
                if (count > 0 || type == SmartPlaylistType.RECENTLY_ADDED) {
                    FilterChip(
                        label = "${type.label} ($count)",
                        selected = false,
                        onClick = { selectedSmartPlaylist = type }
                    )
                }
            }
        }
        if (albumGroups.isNotEmpty() && searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.98f)
                    .clickable { libraryFilter = LibraryFilter.BY_ALBUM }
                    .padding(top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Text1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View all albums",
                    tint = Text3,
                    modifier = Modifier.size(20.dp)
                )
            }
            LazyRow(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(albumGroups, key = { it.key }) { album ->
                    val albumIsPlaying = currentPlayingTrackId != null &&
                        album.tracks.any { it.id == currentPlayingTrackId }
                    LibraryAlbumCard(
                        album = album,
                        isPlaying = albumIsPlaying,
                        onClick = { selectedAlbumTracks = album.tracks }
                    )
                }
            }
        }
        if (filteredPlaylists.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.98f)
                    .clickable { showPlaylistsSheet = true }
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Text1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View all playlists",
                    tint = Text3,
                    modifier = Modifier.size(20.dp)
                )
                Box {
                    IconButton(onClick = { showPlaylistSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort playlists")
                    }
                    DropdownMenu(
                        expanded = showPlaylistSortMenu,
                        onDismissRequest = { showPlaylistSortMenu = false }
                    ) {
                        LibraryPlaylistSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label) },
                                onClick = {
                                    playlistSort = sort
                                    showPlaylistSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Text1
                    )
                    if (libraryFilter != LibraryFilter.BY_ALBUM) {
                    var showTrackSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showTrackSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort tracks")
                        }
                        DropdownMenu(
                            expanded = showTrackSortMenu,
                            onDismissRequest = { showTrackSortMenu = false }
                        ) {
                            LibraryTrackSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        trackSort = sort
                                        showTrackSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    }
                }
        if (libraryFilter == LibraryFilter.BY_ALBUM) {
            AlbumGroupedList(
                albumGroups = searchFilteredAlbumGroups,
                currentPlayingTrackId = currentPlayingTrackId,
                viewModel = viewModel,
                downloadManager = downloadManager,
                snackbarHostState = snackbarHostState,
                onAddToPlaylist = { showAddToPlaylistTrack = it },
                onMoodTag = { showMoodTagTrack = it },
                onArtistClick = { selectedArtist = it },
                reduceMotion = reduceMotion,
                scope = scope,
                context = context
            )
        } else if (filteredTracks.isEmpty()) {
                    val isReallyEmpty = searchQuery.isBlank() && libraryFilter == LibraryFilter.ALL
                    EmptyState(
                        icon = when {
                            searchQuery.isNotBlank() -> Icons.Default.Search
                            libraryFilter == LibraryFilter.FAVORITES -> Icons.Default.Favorite
                            else -> Icons.Default.LibraryMusic
                        },
                        title = when {
                            searchQuery.isNotBlank() -> "No tracks match \"$searchQuery\""
                            libraryFilter == LibraryFilter.FAVORITES -> "No favorites yet"
                            else -> "Your library is empty"
                        },
                        subtitle = when {
                            searchQuery.isNotBlank() -> "Try a different search."
                            libraryFilter == LibraryFilter.FAVORITES -> "Tap the heart on any track to add it."
                            else -> "Import your Spotify playlists or search YouTube to get started."
                        },
                        actionLabel = when {
                            searchQuery.isNotBlank() -> null
                            libraryFilter == LibraryFilter.FAVORITES -> null
                            else -> "Import from Spotify"
                        },
                        onAction = when {
                            searchQuery.isNotBlank() -> null
                            libraryFilter == LibraryFilter.FAVORITES -> null
                            else -> { { importInitialTab = 1; showImportDialog = true } }
                        },
                        secondaryActionLabel = if (isReallyEmpty) "Search YouTube" else null,
                        onSecondaryAction = if (isReallyEmpty) onOpenSearch else null
                    )
        } else {
            AnimatedContent(
                        targetState = isGridView,
                        transitionSpec = {
                            fadeIn(tween(if (reduceMotion) 0 else 200)) togetherWith
                                fadeOut(tween(if (reduceMotion) 0 else 150))
                        },
                        modifier = if (isSheetMode) Modifier.fillMaxWidth() else Modifier.heightIn(max = 400.dp),
                        label = "library_view_mode"
                    ) { showGrid ->
                    if (showGrid) {
                        fun trackToQueueItem(t: DownloadedTrack) = QueueItem(
                            uri = Uri.fromFile(File(t.filePath)).toString(),
                            title = t.title,
                            artist = t.artist,
                            trackId = t.id,
                            thumbnailUrl = t.thumbnailUrl,
                            startMs = t.startMs,
                            endMs = t.endMs
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(filteredTracks, key = { it.id }) { track ->
                                LibraryTrackGridItem(
                                    track = track,
                                    modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                                    isCurrentlyPlaying = track.id == currentPlayingTrackId,
                                    onPlayClick = {
                                        val items = filteredTracks.map { trackToQueueItem(it) }
                                        viewModel.playTrackWithQueue(items, filteredTracks.indexOf(track).coerceAtLeast(0))
                                    },
                                    onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                                    reduceMotion = reduceMotion
                                )
                            }
                        }
                    } else {
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = savedScrollIndex,
                        initialFirstVisibleItemScrollOffset = savedScrollOffset
                    )
                    LaunchedEffect(savedScrollIndex, savedScrollOffset) {
                        if (savedScrollIndex > 0 || savedScrollOffset > 0) {
                            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
                        }
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            val idx = listState.firstVisibleItemIndex
                            val off = listState.firstVisibleItemScrollOffset
                            onSaveScrollPosition(idx, off)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredTracks, key = { _, it -> it.id }) { index, track ->
                            var itemVisible by remember(track.id) { mutableStateOf(index >= 5 || reduceMotion) }
                            LaunchedEffect(track.id) {
                                if (!itemVisible) {
                                    kotlinx.coroutines.delay((index * 30L).coerceAtMost(120L))
                                    itemVisible = true
                                }
                            }
                            AnimatedVisibility(
                                visible = itemVisible,
                                enter = fadeIn(tween(if (reduceMotion) 0 else 280)) +
                                        slideInVertically(tween(if (reduceMotion) 0 else 280)) { 24 }
                            ) {
                            fun trackToQueueItem(t: DownloadedTrack) = QueueItem(
                                uri = Uri.fromFile(File(t.filePath)).toString(),
                                title = t.title,
                                artist = t.artist,
                                trackId = t.id,
                                thumbnailUrl = t.thumbnailUrl,
                                startMs = t.startMs,
                                endMs = t.endMs
                            )
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value != SwipeToDismissBoxValue.Settled) {
                                        pendingDeleteId?.let { id ->
                                            downloadManager.deleteTrack(id)
                                        }
                                        pendingDeleteId = track.id
                                        deleteJob?.cancel()
                                        deleteJob = scope.launch {
                                            kotlinx.coroutines.delay(5000)
                                            pendingDeleteId?.let { id ->
                                                downloadManager.deleteTrack(id)
                                            }
                                            pendingDeleteId = null
                                            deleteJob = null
                                        }
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "\"${track.title}\" removed",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Indefinite
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                pendingDeleteId = null
                                                deleteJob?.cancel()
                                                deleteJob = null
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                backgroundContent = {
                                    val progress = dismissState.progress
                                    val iconScale by animateFloatAsState(
                                        targetValue = (0.6f + progress * 0.6f).coerceIn(0.6f, 1.2f),
                                        animationSpec = if (reduceMotion) tween(0) else spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "deleteIconScale"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Red.copy(
                                                    alpha = (0.4f + progress * 0.6f).coerceIn(0f, 1f)
                                                )
                                            )
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Text1,
                                            modifier = Modifier.size(28.dp).scale(iconScale)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                content = {
                                    LibraryTrackItem(
                                track = track,
                                modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                                isCurrentlyPlaying = track.id == currentPlayingTrackId,
                                canSplitByChapters = downloadManager.canSplitByChapters(track),
                                onPlayClick = {
                                    val queueItems = filteredTracks.map { trackToQueueItem(it) }
                                    val index = filteredTracks.indexOf(track).coerceAtLeast(0)
                                    viewModel.playTrackWithQueue(queueItems, index)
                                },
                                onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                                onDeleteClick = {
                                    pendingDeleteId?.let { id ->
                                        downloadManager.deleteTrack(id)
                                    }
                                    pendingDeleteId = track.id
                                    deleteJob?.cancel()
                                    deleteJob = scope.launch {
                                        kotlinx.coroutines.delay(5000)
                                        pendingDeleteId?.let { id ->
                                            downloadManager.deleteTrack(id)
                                        }
                                        pendingDeleteId = null
                                        deleteJob = null
                                    }
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${track.title}\" removed",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Indefinite
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            pendingDeleteId = null
                                            deleteJob?.cancel()
                                            deleteJob = null
                                        }
                                    }
                                },
                                onPlayNextClick = {
                                    viewModel.addToQueueNext(trackToQueueItem(it))
                                    scope.launch { snackbarHostState.showSnackbar("Playing next") }
                                },
                                onAddToQueueClick = {
                                    viewModel.addToQueueEnd(trackToQueueItem(it))
                                    scope.launch { snackbarHostState.showSnackbar("Added to queue") }
                                },
                                onAddToPlaylistClick = { showAddToPlaylistTrack = it },
                                onMoodTagClick = { showMoodTagTrack = it },
                                onArtistClick = { selectedArtist = it },
                                reduceMotion = reduceMotion,
                                onShareClick = { t ->
                                    val text = if (t.sourceUrl.isNotBlank()) t.sourceUrl else "${t.title} — ${t.artist}"
                                    shareText(context, text, "Share track")
                                },
                                onExcludeFromShuffle = { t, untilMs ->
                                    downloadManager.setExcludedFromShuffle(t.id, untilMs)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (untilMs != null) "Excluded from shuffle" else "Exclusion removed"
                                        )
                                    }
                                },
                                onSplitByChaptersClick = {
                                    downloadManager.splitTrackByChapters(it.id) { newIds ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (newIds.isNotEmpty()) "Split into ${newIds.size} chapters" else "No chapters found"
                                            )
                                        }
                                    }
                                }
                            )
                            }
                        )
                        } // end AnimatedVisibility (stagger)
                        }
                    }
                    } // end list branch
                    } // end AnimatedContent
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated.copy(alpha = 0.42f))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { downloadsExpanded = !downloadsExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (downloadsExpanded) "Hide manage tools" else "Manage storage & downloads",
                style = MaterialTheme.typography.bodyMedium,
                color = Text2,
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Icon(
                imageVector = if (downloadsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (downloadsExpanded) "Collapse" else "Expand",
                tint = Text2,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        AnimatedVisibility(
            visible = downloadsExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used: ${formatBytes(storageUsed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text3
                    )
                    Text(
                        text = "Free: ${formatBytes(storageAvailable)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text3
                    )
                }
                if (completedTracks.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { showCleanUpDialog = true }) {
                                Text("Clean up suggestions", color = LocalAccentColor.current)
                            }
                            TextButton(onClick = { showClearAllConfirm = true }) {
                                Text("Clear all downloads", color = Red)
                            }
                        }
                        TextButton(onClick = { downloadManager.clearAllPlaybackPositions() }) {
                            Text("Reset playback positions", color = Text2)
                        }
                    }
                }
            }
        }
    }
    }
}

private enum class LibraryFilter { ALL, BY_ALBUM, FAVORITES }

private enum class SmartPlaylistType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MOST_PLAYED("Most played", Icons.Default.PlayArrow),
    RECENTLY_ADDED("Recently added", Icons.Default.LibraryMusic),
    NEVER_PLAYED("Never played", Icons.Default.Clear),
    FAVORITES("Favorites", Icons.Default.Favorite),
    LONG_SESSIONS("Long sessions", Icons.Default.HourglassEmpty),
    HIDDEN_GEMS("Hidden gems", Icons.Default.AutoAwesome)
}

private val PRESET_MOODS = listOf("chill", "workout", "focus", "party", "sleep", "road trip", "sad", "happy")

private enum class LibraryPlaylistSort(val label: String) {
    ALPHABETICAL("A–Z"),
    RECENTLY_PLAYED("Recently played"),
    MOST_LISTENED("Most listened to")
}

private enum class LibraryTrackSort(val label: String) {
    DATE_ADDED("Date added"),
    TITLE("Title A–Z"),
    ARTIST("Artist"),
    DURATION("Duration"),
    PLAY_COUNT("Play count")
}

private data class LibraryAlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val artworkUrl: String?,
    val tracks: List<DownloadedTrack>
)

private fun libraryAlbumGroupKey(track: DownloadedTrack): String {
    val albumTitle = track.albumTitle?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    return if (albumTitle != null) {
        "album:${track.artist.trim().lowercase()}:$albumTitle"
    } else {
        "art:${track.thumbnailUrl ?: track.id}"
    }
}

@Composable
private fun AlbumGroupHeader(
    album: LibraryAlbumGroup,
    isExpanded: Boolean,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    val accent = LocalAccentColor.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevron_${album.key}"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .background(if (isPlaying) accent.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (album.artworkUrl != null) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Text3
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isPlaying) accent else Text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    album.artist.takeIf { it.isNotBlank() },
                    album.year?.toString(),
                    "${album.tracks.size} songs"
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = chevronRotation },
            tint = Text3
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGroupedList(
    albumGroups: List<LibraryAlbumGroup>,
    currentPlayingTrackId: String?,
    viewModel: PlayerViewModel,
    downloadManager: DownloadManager,
    snackbarHostState: SnackbarHostState,
    onAddToPlaylist: (DownloadedTrack) -> Unit,
    onMoodTag: (DownloadedTrack) -> Unit,
    onArtistClick: (String) -> Unit,
    reduceMotion: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context, // needed for shareText
    modifier: Modifier = Modifier
) {
    var collapsedAlbums by remember { mutableStateOf(emptySet<String>()) }

    if (albumGroups.isEmpty()) {
        EmptyState(
            icon = Icons.Default.LibraryMusic,
            title = "Your library is empty",
            subtitle = "Import your Spotify playlists or search YouTube to get started.",
            actionLabel = null,
            onAction = null
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        albumGroups.forEach { album ->
            val isExpanded = album.key !in collapsedAlbums
            val isPlayingAlbum = currentPlayingTrackId != null &&
                album.tracks.any { it.id == currentPlayingTrackId }

            item(key = "header_${album.key}") {
                AlbumGroupHeader(
                    album = album,
                    isExpanded = isExpanded,
                    isPlaying = isPlayingAlbum,
                    onToggle = {
                        collapsedAlbums = if (isExpanded) collapsedAlbums + album.key
                                          else collapsedAlbums - album.key
                    }
                )
            }

            if (isExpanded) {
                itemsIndexed(
                    items = album.tracks,
                    key = { _, track -> "track_${track.id}" }
                ) { index, track ->
                    var itemVisible by remember(track.id) { mutableStateOf(index >= 5 || reduceMotion) }
                    LaunchedEffect(track.id) {
                        if (!itemVisible) {
                            kotlinx.coroutines.delay((index * 30L).coerceAtMost(120L))
                            itemVisible = true
                        }
                    }
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn(tween(if (reduceMotion) 0 else 280)) +
                                slideInVertically(tween(if (reduceMotion) 0 else 280)) { 24 }
                    ) {
                        LibraryTrackItem(
                            track = track,
                            modifier = Modifier.padding(start = 40.dp),
                            showArt = false,
                            isCurrentlyPlaying = track.id == currentPlayingTrackId,
                            onPlayClick = {
                                val queueItems = album.tracks.map { t ->
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
                                viewModel.playTrackWithQueue(queueItems, album.tracks.indexOf(track).coerceAtLeast(0))
                            },
                            onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                            onDeleteClick = {
                                downloadManager.deleteTrack(track.id)
                                scope.launch { snackbarHostState.showSnackbar("\"${track.title}\" removed") }
                            },
                            onAddToPlaylistClick = onAddToPlaylist,
                            onMoodTagClick = onMoodTag,
                            onArtistClick = onArtistClick,
                            reduceMotion = reduceMotion,
                            onShareClick = { t ->
                                val text = if (t.sourceUrl.isNotBlank()) t.sourceUrl else "${t.title} — ${t.artist}"
                                shareText(context, text, "Share track")
                            }
                        )
                    }
                }
            }

            item(key = "divider_${album.key}") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle)
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .pressScale(0.92f)
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
            color = if (selected) LocalAccentColor.current else Text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryAlbumCard(
    album: LibraryAlbumGroup,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val infiniteTransition = rememberInfiniteTransition(label = "bloom_${album.key}")
    val bloomPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloomPulse"
    )
    Column(
        modifier = Modifier
            .width(116.dp)
            .pressScale(0.96f)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(104.dp)
        ) {
            if (isPlaying) {
                val bloomScale = 1f + bloomPulse * 0.18f
                val bloomAlpha = 0.35f + bloomPulse * 0.45f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = bloomScale
                            scaleY = bloomScale
                            alpha = bloomAlpha
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor)
                )
            }
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            ) {
            if (album.artworkUrl != null) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Text3
                    )
                }
            }
            }
        }
        Text(
            text = album.title,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = listOfNotNull(
                album.artist.takeIf { it.isNotBlank() },
                album.year?.toString(),
                "${album.tracks.size} songs"
            ).joinToString(" / "),
            style = MaterialTheme.typography.labelSmall,
            color = Text3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
            .pressScale(0.96f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
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
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Text3
                        )
                    }
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelSmall,
            color = Text1,
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
    isCurrentlyPlaying: Boolean = false,
    showArt: Boolean = true,
    canSplitByChapters: Boolean = false,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayNextClick: (DownloadedTrack) -> Unit = {},
    onAddToQueueClick: (DownloadedTrack) -> Unit = {},
    onAddToPlaylistClick: (DownloadedTrack) -> Unit = {},
    onMoodTagClick: (DownloadedTrack) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    reduceMotion: Boolean = false,
    onSplitByChaptersClick: (DownloadedTrack) -> Unit = {},
    onExcludeFromShuffle: (DownloadedTrack, untilMs: Long?) -> Unit = { _, _ -> },
    onShareClick: (DownloadedTrack) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val favoriteScale by animateFloatAsState(
        targetValue = if (track.isFavorite) 1.4f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "favoriteScale"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrentlyPlaying) LocalAccentColor.current.copy(alpha = 0.12f) else Base)
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .padding(end = 8.dp)
                        .background(LocalAccentColor.current)
                )
            }
            if (showArt) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(7.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = Text3
                    )
                }
            }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentlyPlaying) LocalAccentColor.current else Text1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onArtistClick(track.artist) }
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
                                color = Text3
                            )
                        }
                        if (track.fileSizeBytes > 0) {
                            Text(
                                text = formatBytes(track.fileSizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = Text3
                            )
                        }
                        if (track.playCount > 0) {
                            Text(
                                text = if (track.playCount == 1) "Played once" else "Played ${track.playCount} times",
                                style = MaterialTheme.typography.labelSmall,
                                color = Text3
                            )
                        }
                        if (track.isExcludedFromShuffle) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Excluded from shuffle",
                                modifier = Modifier.size(14.dp),
                                tint = Text3
                            )
                        }
                    }
                }
            }
            val view = LocalView.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        onFavoriteClick()
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .scale(favoriteScale)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (track.isFavorite) Red else Text2
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
                            text = { Text("Share") },
                            onClick = {
                                onShareClick(track)
                                menuExpanded = false
                            }
                        )
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
                        DropdownMenuItem(
                            text = { Text("Mood tags") },
                            onClick = {
                                onMoodTagClick(track)
                                menuExpanded = false
                            }
                        )
                        if (track.isExcludedFromShuffle) {
                            DropdownMenuItem(
                                text = { Text("Remove exclusion") },
                                onClick = {
                                    onExcludeFromShuffle(track, null)
                                    menuExpanded = false
                                }
                            )
                        } else {
                            listOf(7 to "7 days", 30 to "30 days", 90 to "90 days").forEach { (days, label) ->
                                DropdownMenuItem(
                                    text = { Text("Don't play for $label") },
                                    onClick = {
                                        val untilMs = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
                                        onExcludeFromShuffle(track, untilMs)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                        if (canSplitByChapters) {
                            DropdownMenuItem(
                                text = { Text("Split by chapters") },
                                onClick = {
                                    onSplitByChaptersClick(track)
                                    menuExpanded = false
                                }
                            )
                        }
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

@Composable
private fun LibraryTrackGridItem(
    track: DownloadedTrack,
    modifier: Modifier = Modifier,
    isCurrentlyPlaying: Boolean = false,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    reduceMotion: Boolean = false
) {
    val favoriteScale by animateFloatAsState(
        targetValue = if (track.isFavorite) 1.4f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "gridFavScale"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
    ) {
        Box {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Text3
                    )
                }
            }
            // Now-playing accent bar at the bottom of the art
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(LocalAccentColor.current)
                        .align(Alignment.BottomCenter)
                )
            }
            // Favorite overlay at top-right
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Unfavorite" else "Favorite",
                    modifier = Modifier.size(16.dp).scale(favoriteScale),
                    tint = if (track.isFavorite) Red else Text1.copy(alpha = 0.85f)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
            text = track.artist,
            style = MaterialTheme.typography.labelSmall,
            color = Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
private fun SmartPlaylistScreen(
    type: SmartPlaylistType,
    tracks: List<DownloadedTrack>,
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
    downloadManager: com.shuckler.app.download.DownloadManager,
    currentPlayingTrackId: String?,
    onAddToPlaylist: (DownloadedTrack) -> Unit,
    onMoodTag: (DownloadedTrack) -> Unit,
    onArtistClick: (String) -> Unit,
    reduceMotion: Boolean = false,
    trackToQueueItem: (DownloadedTrack) -> QueueItem
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ExpandMore, contentDescription = "Back")
            }
            Text(
                text = type.label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tracks in this list", color = Text2)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    LibraryTrackItem(
                        track = track,
                        isCurrentlyPlaying = track.id == currentPlayingTrackId,
                        onPlayClick = {
                            val items = tracks.map { trackToQueueItem(it) }
                            val idx = tracks.indexOf(track).coerceAtLeast(0)
                            viewModel.playTrackWithQueue(items, idx)
                        },
                        onFavoriteClick = { downloadManager.setFavorite(track.id, !track.isFavorite) },
                        onDeleteClick = { downloadManager.deleteTrack(track.id) },
                        onPlayNextClick = { viewModel.addToQueueNext(trackToQueueItem(it)) },
                        onAddToQueueClick = { viewModel.addToQueueEnd(trackToQueueItem(it)) },
                        onAddToPlaylistClick = onAddToPlaylist,
                        onMoodTagClick = onMoodTag,
                        onArtistClick = onArtistClick,
                        reduceMotion = reduceMotion
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodTagDialog(
    track: DownloadedTrack,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selectedTags by remember { mutableStateOf(track.moodTags.toSet()) }
    var customTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = Text1,
        textContentColor = Text2,
        title = { Text("Mood tags for \"${track.title}\"", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add custom tag") },
                        singleLine = true,
                        colors = libraryTextFieldColors()
                    )
                    TextButton(onClick = {
                        val t = customTag.trim().lowercase()
                        if (t.isNotBlank()) {
                            selectedTags = selectedTags + t
                            customTag = ""
                        }
                    }) {
                        Text("Add", color = LocalAccentColor.current)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_MOODS.chunked(4).forEach { chunk ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
	                            chunk.forEach { mood ->
	                                FilterChip(
	                                    label = mood,
	                                    selected = selectedTags.contains(mood),
	                                    onClick = {
	                                        selectedTags = if (selectedTags.contains(mood))
	                                            selectedTags - mood else selectedTags + mood
	                                    }
	                                )
	                            }
                        }
                    }
                }
                Text(
                    text = "Selected: ${selectedTags.joinToString(", ").ifEmpty { "None" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedTags) }) {
                Text("Save", color = LocalAccentColor.current)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Text2)
            }
        }
    )
}

@Composable
private fun CleanUpDialog(
    tracks: List<DownloadedTrack>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val now = System.currentTimeMillis()
    val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000
    val suggested = remember(tracks) {
        tracks.filter { t ->
            t.playCount == 0 ||
            (t.lastPlayedMs > 0 && now - t.lastPlayedMs > ninetyDaysMs && t.playCount < 2)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = Text1,
        textContentColor = Text2,
        title = { Text("Clean up suggestions", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tracks you might want to remove: never played, or not played in 90+ days with fewer than 2 plays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2
                )
                if (suggested.isEmpty()) {
                    Text("Nothing to suggest! Your library looks tidy.", color = Text2)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(suggested.take(20), key = { it.id }) { track ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${track.artist} • ${if (track.playCount == 0) "Never played" else "Played ${track.playCount}x"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Text2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { onDelete(track.id) }) {
                                    Text("Remove", color = Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = LocalAccentColor.current)
            }
        }
    )
}

@Composable
private fun libraryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Text1,
    unfocusedTextColor = Text1,
    focusedContainerColor = SurfaceElevated,
    unfocusedContainerColor = SurfaceElevated,
    focusedBorderColor = LocalAccentColor.current,
    unfocusedBorderColor = Border,
    focusedPlaceholderColor = Text3,
    unfocusedPlaceholderColor = Text3,
    cursorColor = LocalAccentColor.current
)
