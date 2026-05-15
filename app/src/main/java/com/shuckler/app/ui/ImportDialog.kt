package com.shuckler.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportComplete: (Playlist?) -> Unit = {},
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val downloadManager = LocalDownloadManager.current
    val playlistManager = LocalPlaylistManager.current
    val onWifiOnlyBlocked = LocalOnWifiOnlyBlocked.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(initialTab) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = Text1,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(Surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Import playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = Text1
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Text2)
                }
            }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = LocalAccentColor.current
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = LocalAccentColor.current,
                    unselectedContentColor = Text2,
                    text = { Text("YouTube") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = LocalAccentColor.current,
                    unselectedContentColor = Text2,
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
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Text2) },
            singleLine = true,
            colors = importTextFieldColors()
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
                enabled = !isLoading && playlistUrl.isNotBlank(),
                colors = importPrimaryButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Base
                    )
                } else {
                    Text("Fetch playlist")
                }
            }
            if (playlistInfo != null) {
                TextButton(onClick = { playlistInfo = null; playlistUrl = "" }) {
                    Text("Clear", color = Text2)
                }
            }
        }
        error?.let { msg ->
            Text(
                msg,
                color = Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        playlistInfo?.let { info ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${info.name} (${info.items.size} tracks)",
                style = MaterialTheme.typography.titleMedium,
                color = Text1
            )
            if (info.description != null) {
                Text(
                    info.description.take(100) + if ((info.description.length) > 100) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
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
                    color = Text2
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
                modifier = Modifier.padding(top = 8.dp),
                colors = importPrimaryButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (toDownload.isEmpty()) "Create playlist" else "Import ${toDownload.size} tracks")
            }
        }
    }
}

@Composable
private fun importPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = LocalAccentColor.current,
    contentColor = Base,
    disabledContainerColor = SurfaceElevated,
    disabledContentColor = Text3
)

@Composable
private fun importTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
    var likedSongsCount by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedKeys = remember { mutableStateOf(setOf(LIKED_SONGS_KEY)) }
    val scope = rememberCoroutineScope()
    val app = androidContext.applicationContext as? ShucklerApplication

    if (clientId.isBlank()) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Spotify import requires setup.", style = MaterialTheme.typography.bodyMedium, color = Text2)
            Text(
                "Add SPOTIFY_CLIENT_ID to gradle.properties. See IMPORT_SETUP.md for steps.",
                style = MaterialTheme.typography.bodySmall,
                color = Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    // Auto-load playlists + liked songs count when token becomes available
    LaunchedEffect(accessToken) {
        val token = accessToken ?: return@LaunchedEffect
        if (playlists.isNotEmpty()) return@LaunchedEffect
        isLoading = true
        try {
            val (pl, count) = withContext(Dispatchers.IO) {
                Pair(
                    SpotifyRepository.getPlaylists(token),
                    SpotifyRepository.getLikedSongsTotal(token)
                )
            }
            playlists = pl
            likedSongsCount = count
        } catch (e: Exception) {
            error = "Failed to load playlists"
        } finally {
            isLoading = false
        }
    }

    when {
        accessToken == null -> {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Connect your Spotify account to rescue your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val verifier = SpotifyRepository.createCodeVerifier()
                        spotifyAuthManager.saveCodeVerifier(verifier)
                        val url = SpotifyRepository.buildAuthUrl(clientId, verifier)
                        androidContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = importPrimaryButtonColors(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Connect Spotify")
                }
                error?.let { Text(it, color = Red, modifier = Modifier.padding(top = 8.dp)) }
            }
        }

        isLoading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = LocalAccentColor.current)
                    Text("Loading your playlists…", style = MaterialTheme.typography.bodySmall, color = Text2)
                }
            }
        }

        isFetching -> {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = LocalAccentColor.current)
                    Text("Preparing import…", style = MaterialTheme.typography.bodySmall, color = Text2)
                }
            }
        }

        else -> {
            val selKeys = selectedKeys.value
            val allKeys = buildList {
                add(LIKED_SONGS_KEY)
                playlists.forEach { add(it.id) }
            }
            val totalTracks = run {
                var n = 0
                if (LIKED_SONGS_KEY in selKeys) n += likedSongsCount ?: 0
                playlists.filter { it.id in selKeys }.forEach { n += it.trackCount }
                n
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Select all / deselect all header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selKeys.isEmpty()) "Nothing selected" else "${selKeys.size} of ${allKeys.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = Text2
                    )
                    TextButton(
                        onClick = {
                            selectedKeys.value = if (selKeys.size == allKeys.size) emptySet()
                            else allKeys.toSet()
                        }
                    ) {
                        Text(
                            if (selKeys.size == allKeys.size) "Deselect all" else "Select all",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAccentColor.current
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Liked Songs row — always first
                    item(key = LIKED_SONGS_KEY) {
                        val selected = LIKED_SONGS_KEY in selKeys
                        SpotifyChecklistRow(
                            selected = selected,
                            onClick = {
                                selectedKeys.value = if (selected) selKeys - LIKED_SONGS_KEY else selKeys + LIKED_SONGS_KEY
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = LocalAccentColor.current,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            title = "Liked Songs",
                            subtitle = likedSongsCount?.let { "$it tracks" } ?: "Loading…"
                        )
                    }
                    items(playlists, key = { it.id }) { pl ->
                        val selected = pl.id in selKeys
                        SpotifyChecklistRow(
                            selected = selected,
                            onClick = {
                                selectedKeys.value = if (selected) selKeys - pl.id else selKeys + pl.id
                            },
                            leadingContent = {
                                if (pl.imageUrl != null) {
                                    coil.compose.AsyncImage(
                                        model = pl.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Text3, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            title = pl.name,
                            subtitle = "${pl.trackCount} tracks"
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedKeys.value = allKeys.toSet()
                            fireImport(
                                keys = allKeys.toSet(),
                                playlists = playlists,
                                accessToken = accessToken ?: return@Button,
                                app = app ?: return@Button,
                                androidContext = androidContext,
                                onWifiOnlyBlocked = onWifiOnlyBlocked,
                                scope = scope,
                                setFetching = { isFetching = it },
                                onDone = onDismiss
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = importPrimaryButtonColors(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Import All")
                    }
                    Button(
                        onClick = {
                            fireImport(
                                keys = selKeys,
                                playlists = playlists,
                                accessToken = accessToken ?: return@Button,
                                app = app ?: return@Button,
                                androidContext = androidContext,
                                onWifiOnlyBlocked = onWifiOnlyBlocked,
                                scope = scope,
                                setFetching = { isFetching = it },
                                onDone = onDismiss
                            )
                        },
                        enabled = selKeys.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = importPrimaryButtonColors(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (selKeys.isEmpty()) "Import 0 selected"
                            else "Import $totalTracks tracks"
                        )
                    }
                }
                error?.let {
                    Text(it, color = Red, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SpotifyChecklistRow(
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.11f) else SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.28f) else Border,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leadingContent()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Text2)
        }
        androidx.compose.material3.Checkbox(
            checked = selected,
            onCheckedChange = null,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = accent,
                checkmarkColor = Base,
                uncheckedColor = Border
            )
        )
    }
}

private fun fireImport(
    keys: Set<String>,
    playlists: List<SpotifyRepository.SpotifyPlaylist>,
    accessToken: String,
    app: ShucklerApplication,
    androidContext: android.content.Context,
    onWifiOnlyBlocked: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    setFetching: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    if (keys.isEmpty()) return
    setFetching(true)
    scope.launch {
        val items = mutableListOf<com.shuckler.app.spotify.SelectedImportItem>()

        // Fetch Liked Songs if selected
        if (LIKED_SONGS_KEY in keys) {
            val tracks = withContext(Dispatchers.IO) {
                SpotifyRepository.getLikedSongs(accessToken)
            }
            if (tracks.isNotEmpty()) {
                items.add(com.shuckler.app.spotify.SelectedImportItem(
                    key = LIKED_SONGS_KEY,
                    displayName = "Liked Songs",
                    description = null,
                    tracks = tracks
                ))
            }
        }

        // Fetch selected playlists
        for (pl in playlists.filter { it.id in keys }) {
            val tracks = withContext(Dispatchers.IO) {
                SpotifyRepository.getPlaylistTracks(accessToken, pl.id)
            }
            if (tracks.isNotEmpty()) {
                items.add(com.shuckler.app.spotify.SelectedImportItem(
                    key = pl.id,
                    displayName = pl.name,
                    description = pl.description,
                    tracks = tracks
                ))
            }
        }

        if (items.isNotEmpty()) {
            app.spotifyImportManager.startImport(items, onWifiOnlyBlocked)
            val importId = app.spotifyImportManager.progress.value?.importId ?: return@launch
            com.shuckler.app.spotify.SpotifyImportService.start(androidContext, importId)
        }

        setFetching(false)
        kotlinx.coroutines.delay(200)
        onDone()
    }
}

private const val LIKED_SONGS_KEY = "__liked__"
