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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.accessibility.LocalAccessibilityPreferences
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import com.shuckler.app.util.shareText
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
    val filteredTracks = remember(completedTracks, libraryFilter, moodFilter, searchQuery, trackSort, pendingDeleteId) {
        val byFilter = when (libraryFilter) {
            LibraryFilter.ALL -> completedTracks
            LibraryFilter.FAVORITES -> completedTracks.filter { it.isFavorite }
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
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistTrack by remember { mutableStateOf<DownloadedTrack?>(null) }
    var showMoodTagTrack by remember { mutableStateOf<DownloadedTrack?>(null) }
    var selectedSmartPlaylist by remember { mutableStateOf<SmartPlaylistType?>(null) }
    var showCleanUpDialog by remember { mutableStateOf(false) }
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList())
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val currentPlayingTrackId = queueItems.getOrNull((queueInfo.first - 1).coerceIn(0, queueItems.size))?.trackId
    val playlistManager = LocalPlaylistManager.current
    val playlists by playlistManager.playlists.collectAsState()
    val allEntries by playlistManager.allEntries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val reduceMotion by LocalAccessibilityPreferences.current.reduceMotionFlow.collectAsState(
        initial = LocalAccessibilityPreferences.current.reduceMotion
    )

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
                    IconButton(
                        onClick = { showSearchField = !showSearchField },
                        modifier = Modifier
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search library")
                    }
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create new playlist")
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
        val smartPlaylistTracks = remember(completedTracks, selectedSmartPlaylist) {
            when (selectedSmartPlaylist) {
                SmartPlaylistType.MOST_PLAYED -> completedTracks.sortedByDescending { it.playCount }
                SmartPlaylistType.RECENTLY_ADDED -> completedTracks.sortedByDescending { it.downloadDateMs }
                SmartPlaylistType.NEVER_PLAYED -> completedTracks.filter { it.playCount == 0 }
                SmartPlaylistType.FAVORITES -> completedTracks.filter { it.isFavorite }
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmartPlaylistType.entries.forEach { type ->
                val count = when (type) {
                    SmartPlaylistType.MOST_PLAYED -> completedTracks.count { it.playCount > 0 }
                    SmartPlaylistType.RECENTLY_ADDED -> completedTracks.size
                    SmartPlaylistType.NEVER_PLAYED -> completedTracks.count { it.playCount == 0 }
                    SmartPlaylistType.FAVORITES -> completedTracks.count { it.isFavorite }
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
        if (filteredPlaylists.isEmpty() && searchQuery.isBlank()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "No playlists yet",
                subtitle = "Create one to organize your music",
                actionLabel = "Create playlist",
                onAction = { showCreatePlaylistDialog = true }
            )
        }
        if (filteredPlaylists.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showCleanUpDialog = true }) {
                            Text("Clean up suggestions")
                        }
                        TextButton(onClick = { showClearAllConfirm = true }) {
                            Text("Clear all downloads", color = MaterialTheme.colorScheme.error)
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
                        text = "Your Library",
                        style = MaterialTheme.typography.titleMedium
                    )
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
                if (filteredTracks.isEmpty()) {
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
                            else -> "Search and download to get started"
                        },
                        actionLabel = when {
                            searchQuery.isNotBlank() -> null
                            libraryFilter == LibraryFilter.FAVORITES -> null
                            else -> "Open Search"
                        },
                        onAction = when {
                            searchQuery.isNotBlank() -> null
                            libraryFilter == LibraryFilter.FAVORITES -> null
                            else -> onOpenSearch
                        }
                    )
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
                        modifier = if (isSheetMode) Modifier.heightIn(min = 500.dp) else Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
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
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(28.dp)
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
                        }
                    }
                }
            }
        }
    }
    }
}

private enum class LibraryFilter { ALL, FAVORITES }

private enum class SmartPlaylistType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MOST_PLAYED("Most played", Icons.Default.PlayArrow),
    RECENTLY_ADDED("Recently added", Icons.Default.LibraryMusic),
    NEVER_PLAYED("Never played", Icons.Default.Clear),
    FAVORITES("Favorites", Icons.Default.Favorite)
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
    isCurrentlyPlaying: Boolean = false,
    canSplitByChapters: Boolean = false,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayNextClick: (DownloadedTrack) -> Unit = {},
    onAddToQueueClick: (DownloadedTrack) -> Unit = {},
    onAddToPlaylistClick: (DownloadedTrack) -> Unit = {},
    onMoodTagClick: (DownloadedTrack) -> Unit = {},
    reduceMotion: Boolean = false,
    onSplitByChaptersClick: (DownloadedTrack) -> Unit = {},
    onExcludeFromShuffle: (DownloadedTrack, untilMs: Long?) -> Unit = { _, _ -> },
    onShareClick: (DownloadedTrack) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val favoriteScale by animateFloatAsState(
        targetValue = if (track.isFavorite) 1.15f else 1f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 150),
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
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .padding(end = 8.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
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
                        if (track.isExcludedFromShuffle) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Excluded from shuffle",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("No tracks in this list", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = { Text("Mood tags for \"${track.title}\"") },
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
                        singleLine = true
                    )
                    TextButton(onClick = {
                        val t = customTag.trim().lowercase()
                        if (t.isNotBlank()) {
                            selectedTags = selectedTags + t
                            customTag = ""
                        }
                    }) {
                        Text("Add")
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
                                    selected = selectedTags.contains(mood),
                                    onClick = {
                                        selectedTags = if (selectedTags.contains(mood))
                                            selectedTags - mood else selectedTags + mood
                                    },
                                    label = { Text(mood) }
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Selected: ${selectedTags.joinToString(", ").ifEmpty { "None" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedTags) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Clean up suggestions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tracks you might want to remove: never played, or not played in 90+ days with fewer than 2 plays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (suggested.isEmpty()) {
                    Text("Nothing to suggest! Your library looks tidy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { onDelete(track.id) }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
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
