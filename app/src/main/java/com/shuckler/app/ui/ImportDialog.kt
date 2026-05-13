package com.shuckler.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuckler.app.BuildConfig
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.ui.LocalOnWifiOnlyBlocked
import com.shuckler.app.playlist.LocalPlaylistManager
import com.shuckler.app.spotify.LocalSpotifyAuthManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.spotify.SpotifyRepository
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportComplete: (Playlist?) -> Unit = {}
) {
    val context = LocalContext.current
    val downloadManager = LocalDownloadManager.current
    val playlistManager = LocalPlaylistManager.current
    val onWifiOnlyBlocked = LocalOnWifiOnlyBlocked.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Import playlist",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("YouTube") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Spotify") }
                )
            }
            when (selectedTab) {
                0 -> YouTubeImportContent(
                    downloadManager = downloadManager,
                    playlistManager = playlistManager,
                    onWifiOnlyBlocked = onWifiOnlyBlocked ?: {},
                    onImportComplete = { onImportComplete(it); onDismiss() },
                    onDismiss = onDismiss
                )
                1 -> SpotifyImportContent(
                    androidContext = context,
                    downloadManager = downloadManager,
                    playlistManager = playlistManager,
                    spotifyAuthManager = LocalSpotifyAuthManager.current,
                    onWifiOnlyBlocked = onWifiOnlyBlocked ?: {},
                    onImportComplete = { onImportComplete(it); onDismiss() },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun YouTubeImportContent(
    downloadManager: com.shuckler.app.download.DownloadManager,
    playlistManager: com.shuckler.app.playlist.PlaylistManager,
    onWifiOnlyBlocked: () -> Unit = {},
    onImportComplete: (Playlist?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var playlistUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var playlistInfo by remember { mutableStateOf<YouTubeRepository.PlaylistInfo?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = playlistUrl,
            onValueChange = { playlistUrl = it; error = null; playlistInfo = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste YouTube playlist URL") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (playlistUrl.isBlank()) return@Button
                    isLoading = true
                    error = null
                    scope.launch {
                        val result = YouTubeRepository.getPlaylist(playlistUrl.trim())
                        isLoading = false
                        when (result) {
                            is YouTubeRepository.PlaylistResult.Success -> playlistInfo = result.info
                            is YouTubeRepository.PlaylistResult.Failure -> error = result.message
                        }
                    }
                },
                enabled = !isLoading && playlistUrl.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Fetch playlist")
                }
            }
            if (playlistInfo != null) {
                TextButton(onClick = { playlistInfo = null; playlistUrl = "" }) {
                    Text("Clear")
                }
            }
        }
        error?.let { msg ->
            Text(
                msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        playlistInfo?.let { info ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${info.name} (${info.items.size} tracks)",
                style = MaterialTheme.typography.titleMedium
            )
            if (info.description != null) {
                Text(
                    info.description.take(100) + if ((info.description.length) > 100) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val completedUrls = downloadManager.downloads.value
                .filter { it.sourceUrl.isNotBlank() }
                .map { it.sourceUrl }
                .toSet()
            val toDownload = info.items.filter { it.url !in completedUrls }
            val alreadyHave = info.items.size - toDownload.size
            if (alreadyHave > 0) {
                Text(
                    "$alreadyHave already in library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
                Button(
                    onClick = {
                        val playlist = playlistManager.createPlaylist(info.name, info.description)
                        if (toDownload.isEmpty()) {
                            onImportComplete(playlist)
                            return@Button
                        }
                        val appScope = (context.applicationContext as? ShucklerApplication)?.applicationScope ?: return@Button
                        toDownload.forEach { item ->
                            val id = downloadManager.startDownloadFromYouTube(
                                item.url,
                                item.title,
                                item.uploaderName,
                                item.thumbnailUrl,
                                onWifiOnlyBlocked
                            )
                            if (id.isNotBlank()) {
                                appScope.launch {
                                    withContext(Dispatchers.IO) {
                                        var track = downloadManager.downloads.value.find { it.id == id }
                                        var attempts = 0
                                        while (track?.status != com.shuckler.app.download.DownloadStatus.COMPLETED && attempts < 120) {
                                            kotlinx.coroutines.delay(500)
                                            track = downloadManager.downloads.value.find { it.id == id }
                                            if (track?.status == com.shuckler.app.download.DownloadStatus.FAILED) break
                                            attempts++
                                        }
                                        track?.let { t ->
                                            if (t.status == com.shuckler.app.download.DownloadStatus.COMPLETED) {
                                                playlistManager.addTrackToPlaylist(playlist.id, t.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        onImportComplete(playlist)
                    },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (toDownload.isEmpty()) "Create playlist" else "Import ${toDownload.size} tracks")
            }
        }
    }
}

@Composable
private fun SpotifyImportContent(
    androidContext: android.content.Context,
    downloadManager: com.shuckler.app.download.DownloadManager,
    playlistManager: com.shuckler.app.playlist.PlaylistManager,
    spotifyAuthManager: com.shuckler.app.spotify.SpotifyAuthManager,
    onWifiOnlyBlocked: () -> Unit = {},
    onImportComplete: (Playlist?) -> Unit,
    onDismiss: () -> Unit
) {
    val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    val accessToken by spotifyAuthManager.accessToken.collectAsState(initial = null)
    var playlists by remember { mutableStateOf<List<SpotifyRepository.SpotifyPlaylist>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<SpotifyRepository.SpotifyPlaylist?>(null) }
    var tracks by remember { mutableStateOf<List<SpotifyRepository.SpotifyTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (clientId.isBlank()) {
            error = "Spotify Client ID not configured. See IMPORT_SETUP.md"
        }
    }

    if (clientId.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Spotify import requires setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Add SPOTIFY_CLIENT_ID to gradle.properties. See IMPORT_SETUP.md for steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    when {
        accessToken == null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Connect your Spotify account to import playlists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val verifier = SpotifyRepository.createCodeVerifier()
                        spotifyAuthManager.saveCodeVerifier(verifier)
                        val url = SpotifyRepository.buildAuthUrl(clientId, verifier)
                        androidContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Connect Spotify")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        selectedPlaylist == null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (playlists.isEmpty() && !isLoading) {
                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val token = accessToken
                                if (token != null) {
                                    playlists = SpotifyRepository.getPlaylists(token)
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Load my playlists")
                    }
                } else if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists) { pl ->
                            Card(
                                onClick = {
                                    selectedPlaylist = pl
                                    tracks = emptyList()
                                    scope.launch {
                                        val token = accessToken ?: return@launch
                                        tracks = SpotifyRepository.getPlaylistTracks(token, pl.id)
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        pl.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${pl.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {
            val pl = selectedPlaylist!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pl.name, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { selectedPlaylist = null }) { Text("Back") }
                }
                Text("${tracks.size} tracks. Each will be searched on YouTube and downloaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (tracks.isEmpty()) return@Button
                        isImporting = true
                        val playlist = playlistManager.createPlaylist(pl.name, pl.description)
                        val appScope = (androidContext.applicationContext as? ShucklerApplication)?.applicationScope ?: return@Button
                        appScope.launch {
                            for (track in tracks) {
                                val query = "${track.title} ${track.artist}"
                                val results = withContext(Dispatchers.IO) {
                                    YouTubeRepository.search(query)
                                }
                                val best = results.firstOrNull()
                                if (best != null) {
                                    val id = downloadManager.startDownloadFromYouTube(
                                        best.url,
                                        track.title,
                                        track.artist,
                                        best.thumbnailUrl,
                                        onWifiOnlyBlocked,
                                        albumTitle = track.album,
                                        albumYear = track.albumYear
                                    )
                                    if (id.isNotBlank()) {
                                        var d = downloadManager.downloads.value.find { it.id == id }
                                        var attempts = 0
                                        while (d?.status != com.shuckler.app.download.DownloadStatus.COMPLETED && attempts < 120) {
                                            kotlinx.coroutines.delay(500)
                                            d = downloadManager.downloads.value.find { it.id == id }
                                            if (d?.status == com.shuckler.app.download.DownloadStatus.FAILED) break
                                            attempts++
                                        }
                                        d?.let { if (it.status == com.shuckler.app.download.DownloadStatus.COMPLETED) playlistManager.addTrackToPlaylist(playlist.id, it.id) }
                                    }
                                }
                            }
                            isImporting = false
                            onImportComplete(playlist)
                        }
                    },
                    enabled = !isImporting && tracks.isNotEmpty()
                ) {
                    if (isImporting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Import ${tracks.size} tracks from YouTube")
                }
            }
        }
    }
}
