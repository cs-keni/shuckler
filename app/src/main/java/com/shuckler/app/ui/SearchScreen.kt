package com.shuckler.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.preview.PreviewPlayer
import com.shuckler.app.recommendation.RecommendationEngine
import com.shuckler.app.ui.SearchPreferences
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return ""
    return when {
        bytesPerSecond >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
        bytesPerSecond >= 1024 -> "%.0f KB/s".format(bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B/s"
    }
}

@Composable
fun SearchScreen(
    onSettingsClick: () -> Unit = {},
    initialQuery: String? = null,
    onInitialQueryConsumed: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YouTubeSearchResult>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var lastSearchedQuery by remember { mutableStateOf("") }
    var downloadingVideoUrl by remember { mutableStateOf<String?>(null) }
    var youtubeDownloadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val downloadManager = LocalDownloadManager.current
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
            recommendedResults = RecommendationEngine.fetchRecommendedYouTubeResults(context, completedTracks)
            recommendedLoading = false
        } else {
            recommendedResults = emptyList()
        }
    }
    val lastDownloadError by downloadManager.lastDownloadError.collectAsState(initial = null)
    val scrollState = rememberScrollState()
    val previewingVideoUrl by PreviewPlayer.previewingVideoUrl.collectAsState(initial = null)
    val previewPositionMs by PreviewPlayer.positionMs.collectAsState(initial = 0L)

    DisposableEffect(Unit) {
        onDispose { PreviewPlayer.stop() }
    }

    LaunchedEffect(initialQuery) {
        initialQuery?.let { q ->
            searchQuery = q
            onInitialQueryConsumed()
            // Trigger search - runSearch reads searchQuery which we just set
            focusManager.clearFocus()
            searchLoading = true
            searchResults = emptyList()
            lastSearchedQuery = q
            youtubeDownloadError = null
            SearchPreferences.recordSearch(context, q)
            searchResults = YouTubeRepository.search(q)
            searchLoading = false
        }
    }

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.isBlank()) return
        focusManager.clearFocus()
        searchLoading = true
        searchResults = emptyList()
        lastSearchedQuery = q
        youtubeDownloadError = null
        SearchPreferences.recordSearch(context, q)
        scope.launch {
            searchResults = YouTubeRepository.search(q)
            searchLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(title = "Search", onSettingsClick = onSettingsClick)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                placeholder = { Text("Search for music...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() })
            )
            Button(onClick = { runSearch() }, enabled = !searchLoading) {
                Text(if (searchLoading) "…" else "Search")
            }
        }

        if (progress.isNotEmpty()) {
            Text("Active downloads", style = MaterialTheme.typography.titleSmall)
            progress.values.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (p.bytesPerSecond > 0) "${p.percent}% · ${formatSpeed(p.bytesPerSecond)}" else "${p.percent}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (searchLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Searching…", modifier = Modifier.padding(8.dp))
            }
        }

        youtubeDownloadError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        lastDownloadError?.let { msg ->
            Text(
                text = "Download failed: $msg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (!searchLoading && searchResults.isEmpty() && lastSearchedQuery.isNotBlank()) {
            Text(
                text = "No results for \"$lastSearchedQuery\". Check your connection or try another search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (searchResults.isNotEmpty()) {
            Text(
                text = "Results",
                style = MaterialTheme.typography.titleMedium
            )
            searchResults.forEach { result ->
                YouTubeResultItem(
                    result = result,
                    isDownloading = downloadingVideoUrl == result.url,
                    isPreviewing = previewingVideoUrl == result.url,
                    previewPositionMs = if (previewingVideoUrl == result.url) previewPositionMs else 0L,
                    previewDurationMs = PreviewPlayer.previewDurationMs,
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
                        downloadingVideoUrl = result.url
                        downloadManager.startDownloadFromYouTube(
                            result.url,
                            result.title,
                            result.uploaderName ?: "",
                            result.thumbnailUrl
                        )
                        scope.launch {
                            val urlToClear = result.url
                            kotlinx.coroutines.delay(500)
                            if (downloadingVideoUrl == urlToClear) downloadingVideoUrl = null
                        }
                    }
                )
            }
        }

        if (lastSearchedQuery.isBlank() && RecommendationEngine.hasRecommendationData(context, completedTracks)) {
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
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text("Finding recommendations…", modifier = Modifier.padding(8.dp))
                }
            } else if (recommendedResults.isNotEmpty()) {
                recommendedResults.forEach { result ->
                    YouTubeResultItem(
                        result = result,
                        isDownloading = downloadingVideoUrl == result.url,
                        isPreviewing = previewingVideoUrl == result.url,
                        previewPositionMs = if (previewingVideoUrl == result.url) previewPositionMs else 0L,
                        previewDurationMs = PreviewPlayer.previewDurationMs,
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
                            downloadingVideoUrl = result.url
                            downloadManager.startDownloadFromYouTube(
                                result.url,
                                result.title,
                                result.uploaderName ?: "",
                                result.thumbnailUrl
                            )
                            scope.launch {
                                kotlinx.coroutines.delay(500)
                                if (downloadingVideoUrl == result.url) downloadingVideoUrl = null
                            }
                        }
                    )
                }
            }
        }

        val frequentSearches = remember { SearchPreferences.getFrequentSearches(context, minCount = 3) }
        val showTryThese = !searchLoading && searchResults.isEmpty() && frequentSearches.isNotEmpty() &&
            !(lastSearchedQuery.isBlank() && RecommendationEngine.hasRecommendationData(context, completedTracks) && (recommendedLoading || recommendedResults.isNotEmpty()))
        if (showTryThese) {
            Text(
                "Try these",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun YouTubeResultItem(
    result: YouTubeSearchResult,
    isDownloading: Boolean,
    isPreviewing: Boolean,
    previewPositionMs: Long,
    previewDurationMs: Long,
    onPreviewClick: () -> Unit,
    onStopPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                            .padding(end = 12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2
                    )
                    result.uploaderName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (result.durationSeconds > 0) {
                        Text(
                            text = result.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (isPreviewing) {
                        Button(
                            onClick = onStopPreviewClick,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Stop", modifier = Modifier.padding(start = 4.dp))
                        }
                    } else {
                        Button(
                            onClick = onPreviewClick,
                            enabled = !isDownloading,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Preview", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Button(
                        onClick = onDownloadClick,
                        enabled = !isDownloading,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(if (isDownloading) "…" else "Download")
                    }
                }
            }
            if (isPreviewing && previewDurationMs > 0) {
                val progress = (previewPositionMs.toFloat() / previewDurationMs).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Preview: ${previewPositionMs / 1000}s / ${previewDurationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
