package com.shuckler.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.ui.LocalOnWifiOnlyBlocked
import com.shuckler.app.util.shareText
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.playlist.PlaylistEntry
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import androidx.compose.ui.graphics.Color
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Red
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onBack: () -> Unit,
    onPlaylistUpdated: ((Playlist) -> Unit)? = null,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val context = LocalContext.current
    val playlistManager = LocalPlaylistManager.current
    val downloadManager = LocalDownloadManager.current
    val onWifiOnlyBlocked = LocalOnWifiOnlyBlocked.current
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList())
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val currentPlayingTrackId = queueItems.getOrNull((queueInfo.first - 1).coerceIn(0, queueItems.size))?.trackId
    val allEntries by playlistManager.allEntries.collectAsState()
    val downloads by downloadManager.downloads.collectAsState()
    val progress by downloadManager.progress.collectAsState(initial = emptyMap())
    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    val entries = remember(playlist.id, allEntries) {
        allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
    }
    val trackIds = entries.map { it.trackId }.toSet()
    val tracks = entries.mapNotNull { e -> completedTracks.find { it.id == e.trackId } }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var pendingRemoveTrackId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxCoverHeightPx = with(density) { (maxHeight / 2).toPx() }
        val minCoverHeightPx = with(density) { (maxHeight / 4).toPx() }
        val scrollOffset = derivedStateOf {
            val idx = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (idx == 0) offset.toFloat() else maxCoverHeightPx
        }
        val coverHeightPx = derivedStateOf {
            (maxCoverHeightPx - scrollOffset.value).coerceIn(minCoverHeightPx, maxCoverHeightPx)
        }
        val maxCoverHeightDp = with(density) { (maxCoverHeightPx / density.density).toDp() }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentColor = Text1
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Text2)
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                color = Text1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val lines = listOf(playlist.name) + tracks.map { "${it.title} — ${it.artist}" }
                shareText(context, lines.joinToString("\n"), "Share playlist")
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share playlist", tint = Text2)
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Text2)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = {
                    item {
                        Spacer(modifier = Modifier.fillMaxWidth().height(maxCoverHeightDp))
                    }
                    if (playlist.description != null) {
                        item {
                            Text(
                                text = playlist.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Text2,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val items = tracks.map { t ->
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
                                        viewModel.playTrackWithQueue(items, 0)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LocalAccentColor.current,
                                        contentColor = Base
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Play")
                                }
                                val entriesForPlaylist = allEntries.filter { it.playlistId == playlist.id }
                                val toDownload = entriesForPlaylist.mapNotNull { e ->
                                    downloads.find { it.id == e.trackId }
                                }.filter { t ->
                                    (t.status != DownloadStatus.COMPLETED || t.filePath.isBlank()) && t.sourceUrl.isNotBlank()
                                }
                                val inLibrary = entriesForPlaylist.count { e ->
                                    val t = downloads.find { it.id == e.trackId }
                                    t != null && t.status == DownloadStatus.COMPLETED && t.filePath.isNotBlank()
                                }
                                val total = entriesForPlaylist.size
                                val downloadingCount = progress.keys.count { id ->
                                    entriesForPlaylist.any { it.trackId == id }
                                }
                                val downloadAllEnabled = toDownload.isNotEmpty() && downloadingCount == 0
                                TextButton(
                                    onClick = {
                                        toDownload.forEach { t ->
                                            downloadManager.retryDownload(t.id, onWifiOnlyBlocked = onWifiOnlyBlocked)
                                        }
                                    },
                                    enabled = downloadAllEnabled
                                ) {
                                    Text(
                                        when {
                                            downloadingCount > 0 -> "Downloading $downloadingCount…"
                                            toDownload.isNotEmpty() -> "Download ${toDownload.size} missing"
                                            inLibrary == total && total > 0 -> "All $total in library"
                                            else -> "Download all"
                                        },
                                        color = if (downloadAllEnabled) LocalAccentColor.current else Text3
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Tracks (${tracks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Text1,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (tracks.isEmpty()) {
                        item {
                            EmptyState(
                                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.PlaylistAdd,
                                title = "No tracks yet",
                                subtitle = "Add tracks from Your Library to get started."
                            )
                        }
                    } else {
                        items(tracks.filter { it.id != pendingRemoveTrackId }, key = { it.id }) { track ->
                        fun toQueueItem(t: DownloadedTrack) = QueueItem(
                            uri = Uri.fromFile(File(t.filePath)).toString(),
                            title = t.title,
                            artist = t.artist,
                            trackId = t.id,
                            thumbnailUrl = t.thumbnailUrl,
                            startMs = t.startMs,
                            endMs = t.endMs
                        )
                        val displayTracks = tracks.filter { it.id != pendingRemoveTrackId }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    pendingRemoveTrackId = track.id
                                    playlistManager.removeTrackFromPlaylist(playlist.id, track.id)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Removed \"${track.title}\" from playlist",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Indefinite
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            playlistManager.addTrackToPlaylist(playlist.id, track.id)
                                            pendingRemoveTrackId = null
                                        } else {
                                            pendingRemoveTrackId = null
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
                                        .background(Red)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = Text1,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            content = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (track.id == currentPlayingTrackId) {
                                                LocalAccentColor.current.copy(alpha = 0.12f)
                                            } else {
                                                Base
                                            }
                                        )
                                        .clickable {
                                            val idx = displayTracks.indexOf(track)
                                            val items = displayTracks.map { toQueueItem(it) }
                                            viewModel.playTrackWithQueue(items, idx)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                        if (track.id == currentPlayingTrackId) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(32.dp)
                                                    .padding(end = 8.dp)
                                                    .background(LocalAccentColor.current)
                                            )
                                        }
                                        if (track.thumbnailUrl != null) {
                                            AsyncImage(
                                                model = track.thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SurfaceElevated)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp).align(Alignment.Center),
                                                    tint = Text2
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                            Text(text = track.title, style = MaterialTheme.typography.titleSmall, color = Text1, maxLines = 1)
                                            Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = Text2, maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingRemoveTrackId = track.id
                                                playlistManager.removeTrackFromPlaylist(playlist.id, track.id)
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Removed \"${track.title}\" from playlist",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Indefinite
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        playlistManager.addTrackToPlaylist(playlist.id, track.id)
                                                    }
                                                    pendingRemoveTrackId = null
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Red)
                                        }
                                }
                            }
                        )
                    }
                }
            }
            )
            val coverHeightDp = with(density) { (coverHeightPx.value / density.density).toDp() }
            val maxW = this@BoxWithConstraints.maxWidth
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(coverHeightDp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val size = minOf(coverHeightDp, maxW - 48.dp)
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (playlist.coverImagePath != null && File(playlist.coverImagePath).exists()) {
                        AsyncImage(
                            model = File(playlist.coverImagePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (tracks.isNotEmpty() && tracks.first().thumbnailUrl != null) {
                        AsyncImage(
                            model = tracks.first().thumbnailUrl,
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
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Text2
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Surface,
            titleContentColor = Text1,
            textContentColor = Text2,
            title = { Text("Delete playlist?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("This will permanently delete \"${playlist.name}\". Tracks in your library are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    playlistManager.deletePlaylist(playlist.id)
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Text2)
                }
            }
        )
    }

    if (showEditDialog) {
        CreateEditPlaylistDialog(
            playlist = playlist,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                playlistManager.updatePlaylist(updated)
                onPlaylistUpdated?.invoke(updated)
                showEditDialog = false
            }
        )
    }
    }
}

@Composable
fun CreateEditPlaylistDialog(
    playlist: Playlist?,
    onDismiss: () -> Unit,
    onSave: (Playlist) -> Unit
) {
    var name by remember(playlist?.id) { mutableStateOf(playlist?.name ?: "") }
    var description by remember(playlist?.id) { mutableStateOf(playlist?.description ?: "") }
    var coverPath by remember(playlist?.id) { mutableStateOf(playlist?.coverImagePath) }
    var pendingCoverUri by remember { mutableStateOf<String?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<String?>(null) }
    var cropForPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val playlistManager = LocalPlaylistManager.current
    val scope = rememberCoroutineScope()
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (playlist != null) {
                cropUri = uri.toString()
                cropForPlaylist = playlist
                showCropDialog = true
            } else {
                pendingCoverUri = uri.toString()
            }
        }
    }

    if (showCropDialog) {
        val uriToCrop = cropUri
        val targetPlaylist = cropForPlaylist
        if (uriToCrop != null && targetPlaylist != null) {
            CropCoverDialog(
                imageUri = uriToCrop,
                onCropComplete = { bitmap ->
                    scope.launch {
                        val path = playlistManager.saveCoverFromBitmap(targetPlaylist.id, bitmap)
                        val updated = if (path != null) {
                            coverPath = path
                            playlistManager.updatePlaylist(targetPlaylist.copy(coverImagePath = path))
                            targetPlaylist.copy(coverImagePath = path)
                        } else targetPlaylist
                        showCropDialog = false
                        cropUri = null
                        cropForPlaylist = null
                        if (playlist == null) onSave(updated)
                    }
                },
                onDismiss = {
                    showCropDialog = false
                    cropUri = null
                    val p = cropForPlaylist
                    cropForPlaylist = null
                    if (p != null && playlist == null) onSave(p)
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = Text1,
        textContentColor = Text2,
        title = {
            Text(
                if (playlist != null) "Edit playlist" else "New playlist",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = playlistTextFieldColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = playlistTextFieldColors()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        when {
                            coverPath != null && File(coverPath!!).exists() -> {
                                AsyncImage(
                                    model = File(coverPath!!),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            pendingCoverUri != null -> {
                                AsyncImage(
                                    model = android.net.Uri.parse(pendingCoverUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Text2
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change cover",
                                modifier = Modifier.size(36.dp),
                                tint = Text1.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (playlist != null) {
                    val updated = playlist.copy(
                        name = name.ifBlank { playlist.name },
                        description = description.takeIf { it.isNotBlank() },
                        coverImagePath = coverPath
                    )
                    playlistManager.updatePlaylist(updated)
                    onSave(updated)
                } else {
                    val p = playlistManager.createPlaylist(
                        name = name.ifBlank { "New Playlist" },
                        description = description.takeIf { it.isNotBlank() },
                        coverImagePath = null
                    )
                    if (pendingCoverUri != null) {
                        cropUri = pendingCoverUri
                        cropForPlaylist = p
                        showCropDialog = true
                        pendingCoverUri = null
                    } else {
                        onSave(p)
                    }
                }
            }) {
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
private fun playlistTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Text1,
    unfocusedTextColor = Text1,
    focusedContainerColor = SurfaceElevated,
    unfocusedContainerColor = SurfaceElevated,
    focusedBorderColor = LocalAccentColor.current,
    unfocusedBorderColor = Border,
    focusedLabelColor = Text2,
    unfocusedLabelColor = Text3,
    cursorColor = LocalAccentColor.current
)

@Composable
fun AddToPlaylistDialog(
    track: DownloadedTrack,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val playlistManager = LocalPlaylistManager.current
    val playlists by playlistManager.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = Text1,
        textContentColor = Text2,
        title = { Text("Add to playlist", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add \"${track.title}\" to:", style = MaterialTheme.typography.bodyMedium, color = Text2)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(playlists) { p ->
                        Text(
                            text = p.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playlistManager.addTrackToPlaylist(p.id, track.id)
                                    onAdded()
                                    onDismiss()
                                }
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Text1
                        )
                    }
                }
                TextButton(onClick = { showCreateDialog = true }) {
                    Text("+ New playlist", color = LocalAccentColor.current)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = LocalAccentColor.current)
            }
        }
    )

    if (showCreateDialog) {
        CreateEditPlaylistDialog(
            playlist = null,
            onDismiss = { showCreateDialog = false },
            onSave = { newPlaylist ->
                playlistManager.addTrackToPlaylist(newPlaylist.id, track.id)
                onAdded()
                showCreateDialog = false
                onDismiss()
            }
        )
    }
}
