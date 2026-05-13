package com.shuckler.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.ui.LocalOnWifiOnlyBlocked
import com.shuckler.app.ui.LocalSnackbarHostState
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.preview.PreviewPlayer
import com.shuckler.app.recommendation.RecommendationEngine
import com.shuckler.app.ui.SearchPreferences
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Red
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onSettingsClick: () -> Unit = {},
    initialQuery: String? = null,
    onInitialQueryConsumed: () -> Unit = {},
    savedScrollOffset: Int = 0,
    onSaveScrollPosition: (offset: Int) -> Unit = { _ -> },
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            com.shuckler.app.player.LocalMusicServiceConnection.current
        )
    )
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YouTubeSearchResult>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var lastSearchedQuery by remember { mutableStateOf("") }
    var downloadingVideoUrl by remember { mutableStateOf<String?>(null) }
    var streamingVideoUrl by remember { mutableStateOf<String?>(null) }
    var youtubeDownloadError by remember { mutableStateOf<String?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val suggestions = remember(searchQuery, context) {
        SearchPreferences.getSuggestions(context, searchQuery, max = 8)
    }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val downloadManager = LocalDownloadManager.current
    val onWifiOnlyBlocked = LocalOnWifiOnlyBlocked.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val progress by downloadManager.progress.collectAsState(initial = emptyMap())
    val completedTracks = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
    }
    var recommendedResults by remember { mutableStateOf<List<YouTubeSearchResult>>(emptyList()) }
    var recommendedLoading by remember { mutableStateOf(false) }

    LaunchedEffect(completedTracks) {
        if (RecommendationEngine.hasRecommendationData(context, completedTracks)) {
            recommendedLoading = true
            recommendedResults = try {
                RecommendationEngine.fetchRecommendedYouTubeResults(context, completedTracks)
            } catch (_: Throwable) {
                emptyList()
            }
            recommendedLoading = false
        } else {
            recommendedResults = emptyList()
        }
    }
    val lastDownloadError by downloadManager.lastDownloadError.collectAsState(initial = null)
    val lastFailedDownloadId by downloadManager.lastFailedDownloadId.collectAsState(initial = null)
    val snackbarHostState = LocalSnackbarHostState.current
    val scrollState = rememberScrollState()
    LaunchedEffect(savedScrollOffset) {
        if (savedScrollOffset > 0) {
            scrollState.scrollTo(savedScrollOffset)
        }
    }
    val previewingVideoUrl by PreviewPlayer.previewingVideoUrl.collectAsState(initial = null)
    val previewPositionMs by PreviewPlayer.positionMs.collectAsState(initial = 0L)
    val accentColor = LocalAccentColor.current

    DisposableEffect(Unit) {
        onDispose {
            PreviewPlayer.stop()
            onSaveScrollPosition(scrollState.value)
        }
    }

    fun doSearch(q: String) {
        searchLoading = true
        searchResults = emptyList()
        searchError = null
        SearchPreferences.recordSearch(context, q)
        scope.launch {
            try {
                searchResults = YouTubeRepository.search(q)
            } catch (e: Throwable) {
                searchError = e.message ?: "Search failed"
                searchResults = emptyList()
            }
            searchLoading = false
        }
    }

    LaunchedEffect(initialQuery) {
        initialQuery?.let { q ->
            searchQuery = q
            onInitialQueryConsumed()
            focusManager.clearFocus()
            lastSearchedQuery = q
            youtubeDownloadError = null
            doSearch(q)
        }
    }

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.isBlank()) return
        focusManager.clearFocus()
        lastSearchedQuery = q
        youtubeDownloadError = null
        doSearch(q)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(title = "Search", onSettingsClick = onSettingsClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search for music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text3
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Text3
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = accentColor.copy(alpha = 0.75f),
                        unfocusedBorderColor = Border,
                        cursorColor = accentColor,
                        focusedTextColor = Text1,
                        unfocusedTextColor = Text1
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() })
                )
                Button(
                    onClick = { runSearch() },
                    enabled = !searchLoading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Base,
                        disabledContainerColor = SurfaceElevated,
                        disabledContentColor = Text3
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(if (searchLoading) "..." else "Go", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (suggestions.isNotEmpty() && !searchLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SearchChip(
                        text = suggestion,
                        onClick = {
                            searchQuery = suggestion
                            runSearch()
                        }
                    )
                }
            }
        }
        if (SearchPreferences.getRecentSearches(context).isNotEmpty() && searchQuery.length < 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Recent")
                    TextButton(onClick = {
                        SearchPreferences.clearRecentSearches(context)
                    }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = Text3)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchPreferences.getRecentSearches(context).take(8).forEach { recent ->
                        SearchChip(
                            text = recent,
                            onClick = {
                                searchQuery = recent
                                runSearch()
                            }
                        )
                    }
                }
            }
        }

        if (progress.isNotEmpty()) {
            SectionLabel("Downloading", modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                progress.values.forEach { p ->
                    val track = downloads.find { it.id == p.id }
                    WaveformDownloadCard(
                        title = track?.title ?: "Downloading…",
                        artist = track?.artist ?: "",
                        thumbnailUrl = track?.thumbnailUrl,
                        progress = p
                    )
                }
            }
        }

        if (searchLoading) {
            Column(modifier = Modifier.fillMaxWidth()) {
                repeat(5) { ShimmerTrackRow() }
            }
        }

        youtubeDownloadError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = Red,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
            )
        }
        lastDownloadError?.let { msg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Download failed: $msg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Red,
                    modifier = Modifier.weight(1f)
                )
                lastFailedDownloadId?.let { id ->
                    Button(
                        onClick = { downloadManager.retryDownload(id, onWifiOnlyBlocked = onWifiOnlyBlocked) }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        searchError?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Red,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { searchError = null; runSearch() }) {
                    Text("Retry")
                }
            }
        }

        if (!searchLoading && searchResults.isEmpty() && lastSearchedQuery.isNotBlank() && searchError == null) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "No results for \"$lastSearchedQuery\"",
                subtitle = "Try a different search.",
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        if (searchResults.isNotEmpty()) {
            SectionLabel("Results", modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchResults.forEach { result ->
                    val isDownloaded = completedTracks.any { it.sourceUrl == result.url }
                    YouTubeResultItem(
                        result = result,
                        isDownloading = downloadingVideoUrl == result.url,
                        isStreaming = streamingVideoUrl == result.url,
                        isPreviewing = previewingVideoUrl == result.url,
                        isDownloaded = isDownloaded,
                        previewPositionMs = if (previewingVideoUrl == result.url) previewPositionMs else 0L,
                        previewDurationMs = PreviewPlayer.previewDurationMs,
                        onPlayClick = {
                        if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                        youtubeDownloadError = null
                        streamingVideoUrl = result.url
                        scope.launch {
                            val resultAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                            when (resultAudio) {
                                is YouTubeRepository.AudioStreamResult.Success -> {
                                    viewModel.playTrack(
                                        Uri.parse(resultAudio.info.url),
                                        result.title,
                                        result.uploaderName ?: "Unknown",
                                        thumbnailUrl = result.thumbnailUrl
                                    )
                                }
                                is YouTubeRepository.AudioStreamResult.Failure -> {
                                    youtubeDownloadError = resultAudio.message
                                    val host = snackbarHostState
                                    if (host != null) {
                                        val r = host.showSnackbar("Couldn't play — check connection", actionLabel = "Retry", duration = SnackbarDuration.Short)
                                        if (r == SnackbarResult.ActionPerformed) {
                                            streamingVideoUrl = result.url
                                            val retryAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                            when (retryAudio) {
                                                is YouTubeRepository.AudioStreamResult.Success -> viewModel.playTrack(
                                                    Uri.parse(retryAudio.info.url), result.title, result.uploaderName ?: "Unknown", thumbnailUrl = result.thumbnailUrl
                                                )
                                                is YouTubeRepository.AudioStreamResult.Failure -> youtubeDownloadError = retryAudio.message
                                            }
                                            streamingVideoUrl = null
                                        }
                                    }
                                }
                            }
                            streamingVideoUrl = null
                        }
                    },
                    onPreviewClick = {
                        youtubeDownloadError = null
                        scope.launch {
                            val resultAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                            when (resultAudio) {
                                is YouTubeRepository.AudioStreamResult.Success ->
                                    PreviewPlayer.play(context, result.url, resultAudio.info.url)
                                is YouTubeRepository.AudioStreamResult.Failure ->
                                    youtubeDownloadError = resultAudio.message
                            }
                        }
                    },
                    onStopPreviewClick = { PreviewPlayer.stop() },
                    onDownloadClick = {
                        if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                        youtubeDownloadError = null
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
                                val urlToClear = result.url
                                kotlinx.coroutines.delay(500)
                                if (downloadingVideoUrl == urlToClear) downloadingVideoUrl = null
                            }
                        }
                        }
                    )
                }
            }
        }

        if (lastSearchedQuery.isBlank() && RecommendationEngine.hasRecommendationData(context, completedTracks)) {
            SectionLabel("Recommended for you", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            if (recommendedLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        "Finding recommendations…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text2,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else if (recommendedResults.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendedResults.forEach { result ->
                        val isDownloaded = completedTracks.any { it.sourceUrl == result.url }
                        YouTubeResultItem(
                            result = result,
                            isDownloading = downloadingVideoUrl == result.url,
                            isStreaming = streamingVideoUrl == result.url,
                            isPreviewing = previewingVideoUrl == result.url,
                            isDownloaded = isDownloaded,
                            previewPositionMs = if (previewingVideoUrl == result.url) previewPositionMs else 0L,
                            previewDurationMs = PreviewPlayer.previewDurationMs,
                            onPlayClick = {
                                if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                                youtubeDownloadError = null
                                streamingVideoUrl = result.url
                                scope.launch {
                                    val resultAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                    when (resultAudio) {
                                        is YouTubeRepository.AudioStreamResult.Success -> {
                                            viewModel.playTrack(
                                                Uri.parse(resultAudio.info.url),
                                                result.title,
                                                result.uploaderName ?: "Unknown",
                                                thumbnailUrl = result.thumbnailUrl
                                            )
                                        }
                                        is YouTubeRepository.AudioStreamResult.Failure -> {
                                            youtubeDownloadError = resultAudio.message
                                            val host = snackbarHostState
                                            if (host != null) {
                                                val r = host.showSnackbar("Couldn't play — check connection", actionLabel = "Retry", duration = SnackbarDuration.Short)
                                                if (r == SnackbarResult.ActionPerformed) {
                                                    streamingVideoUrl = result.url
                                                    val retryAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                                    when (retryAudio) {
                                                        is YouTubeRepository.AudioStreamResult.Success -> viewModel.playTrack(
                                                            Uri.parse(retryAudio.info.url), result.title, result.uploaderName ?: "Unknown", thumbnailUrl = result.thumbnailUrl
                                                        )
                                                        is YouTubeRepository.AudioStreamResult.Failure -> youtubeDownloadError = retryAudio.message
                                                    }
                                                    streamingVideoUrl = null
                                                }
                                            }
                                        }
                                    }
                                    streamingVideoUrl = null
                                }
                            },
                            onPreviewClick = {
                                youtubeDownloadError = null
                                scope.launch {
                                    val resultAudio = YouTubeRepository.getAudioStreamUrl(result.url, downloadManager.downloadQuality)
                                    when (resultAudio) {
                                        is YouTubeRepository.AudioStreamResult.Success ->
                                            PreviewPlayer.play(context, result.url, resultAudio.info.url)
                                        is YouTubeRepository.AudioStreamResult.Failure ->
                                            youtubeDownloadError = resultAudio.message
                                    }
                                }
                            },
                            onStopPreviewClick = { PreviewPlayer.stop() },
                            onDownloadClick = {
                                if (PreviewPlayer.isPreviewing(result.url)) PreviewPlayer.stop()
                                youtubeDownloadError = null
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

        val frequentSearches = remember { SearchPreferences.getFrequentSearches(context, minCount = 3) }
        val showTryThese = !searchLoading && searchResults.isEmpty() && frequentSearches.isNotEmpty() &&
            !(lastSearchedQuery.isBlank() && RecommendationEngine.hasRecommendationData(context, completedTracks) && (recommendedLoading || recommendedResults.isNotEmpty()))
        if (showTryThese) {
            SectionLabel("Try these", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                frequentSearches.take(5).forEach { suggestion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                searchQuery = suggestion
                                runSearch()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceElevated
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text1,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        val showEmptyState = downloads.isEmpty() && progress.isEmpty() && searchResults.isEmpty() &&
            !searchLoading && recommendedResults.isEmpty() &&
            !(lastSearchedQuery.isBlank() && RecommendationEngine.hasRecommendationData(context, completedTracks) && recommendedLoading)
        if (showEmptyState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Search YouTube to find and download music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text2
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Text1,
        modifier = modifier
    )
}

@Composable
private fun SearchChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceElevated)
            .border(1.dp, Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun YouTubeResultItem(
    result: YouTubeSearchResult,
    isDownloading: Boolean,
    isStreaming: Boolean,
    isPreviewing: Boolean,
    isDownloaded: Boolean,
    previewPositionMs: Long,
    previewDurationMs: Long,
    onPlayClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onStopPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceElevated
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (result.thumbnailUrl != null) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center),
                            tint = Text3
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 80.dp)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Text1,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    result.uploaderName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (result.durationSeconds > 0) {
                        Text(
                            text = result.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = Text3
                        )
                    }
                }
            }

            val compactButtonModifier = Modifier.height(36.dp)
            val compactButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                if (isPreviewing) {
                    Button(
                        onClick = onStopPreviewClick,
                        modifier = compactButtonModifier,
                        contentPadding = compactButtonPadding,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Text1)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Stop", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onPlayClick,
                        enabled = !isDownloading && !isStreaming,
                        modifier = compactButtonModifier,
                        contentPadding = compactButtonPadding,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Base,
                            disabledContainerColor = Surface,
                            disabledContentColor = Text3
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(if (isStreaming) "..." else "Play", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onPreviewClick,
                        enabled = !isDownloading,
                        modifier = compactButtonModifier,
                        contentPadding = compactButtonPadding,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Surface,
                            contentColor = Text1,
                            disabledContainerColor = Surface,
                            disabledContentColor = Text3
                        )
                    ) {
                        Text("Preview", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Button(
                    onClick = onDownloadClick,
                    enabled = !isDownloading && !isDownloaded,
                    modifier = compactButtonModifier,
                    contentPadding = compactButtonPadding,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Surface,
                        contentColor = if (isDownloaded) Text3 else accentColor,
                        disabledContainerColor = Surface,
                        disabledContentColor = Text3
                    )
                ) {
                    Text(if (isDownloading) "..." else if (isDownloaded) "Downloaded" else "Download", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (isPreviewing && previewDurationMs > 0) {
                val progress = (previewPositionMs.toFloat() / previewDurationMs).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = accentColor,
                        trackColor = Surface
                    )
                    Text(
                        text = "Preview: ${previewPositionMs / 1000}s / ${previewDurationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = Text3,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
