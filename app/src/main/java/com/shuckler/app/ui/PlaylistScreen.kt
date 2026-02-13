package com.shuckler.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.playlist.PlaylistEntry
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
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
    val playlistManager = LocalPlaylistManager.current
    val downloadManager = LocalDownloadManager.current
    val allEntries by playlistManager.allEntries.collectAsState()
    val downloads by downloadManager.downloads.collectAsState()
    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    val entries = remember(playlist.id, allEntries) {
        allEntries.filter { it.playlistId == playlist.id }.sortedBy { it.position }
    }
    val trackIds = entries.map { it.trackId }.toSet()
    val tracks = entries.mapNotNull { e -> completedTracks.find { it.id == e.trackId } }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    item {
                        Spacer(modifier = Modifier.fillMaxWidth().height(maxCoverHeightDp))
                    }
                    if (playlist.description != null) {
                        item {
                            Text(
                                text = playlist.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val items = tracks.map { t ->
                                            QueueItem(
                                                uri = Uri.fromFile(File(t.filePath)).toString(),
                                                title = t.title,
                                                artist = t.artist,
                                                trackId = t.id,
                                                thumbnailUrl = t.thumbnailUrl
                                            )
                                        }
                                        viewModel.playTrackWithQueue(items, 0)
                                        downloadManager.incrementPlayCount(tracks.first().id)
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Play")
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Tracks (${tracks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (tracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tracks yet. Add tracks from Your Library.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(tracks, key = { it.id }) { track ->
                        fun toQueueItem(t: DownloadedTrack) = QueueItem(
                            uri = Uri.fromFile(File(t.filePath)).toString(),
                            title = t.title,
                            artist = t.artist,
                            trackId = t.id,
                            thumbnailUrl = t.thumbnailUrl
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = tracks.indexOf(track)
                                    val items = tracks.map { toQueueItem(it) }
                                    viewModel.playTrackWithQueue(items, idx)
                                    downloadManager.incrementPlayCount(track.id)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (track.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp).align(Alignment.Center),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(text = track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { playlistManager.removeTrackFromPlaylist(playlist.id, track.id) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
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
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            title = { Text("Delete playlist?") },
            text = { Text("This will permanently delete \"${playlist.name}\". Tracks in your library are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    playlistManager.deletePlaylist(playlist.id)
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        CreateEditPlaylistDialog(
            playlist = playlist,
            onDismiss = { showEditDialog = false },
            onSave = {
                playlistManager.updatePlaylist(it)
                onPlaylistUpdated?.invoke(it)
                showEditDialog = false
            }
        )
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
        title = { Text(if (playlist != null) "Edit playlist" else "New playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
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
        title = { Text("Add to playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add \"${track.title}\" to:", style = MaterialTheme.typography.bodyMedium)
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                TextButton(onClick = { showCreateDialog = true }) {
                    Text("+ New playlist")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
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
