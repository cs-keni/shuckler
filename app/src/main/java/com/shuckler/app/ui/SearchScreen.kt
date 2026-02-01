package com.shuckler.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import kotlinx.coroutines.launch

@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YouTubeSearchResult>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var lastSearchedQuery by remember { mutableStateOf("") }
    var downloadingVideoUrl by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val downloadManager = LocalDownloadManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val progress by downloadManager.progress.collectAsState(initial = emptyMap())
    val scrollState = rememberScrollState()

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.isBlank()) return
        focusManager.clearFocus()
        searchLoading = true
        searchResults = emptyList()
        lastSearchedQuery = q
        scope.launch {
            searchResults = YouTubeRepository.search(q)
            searchLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("YouTube search") },
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
                    onDownloadClick = {
                        downloadingVideoUrl = result.url
                        scope.launch {
                            val audio = YouTubeRepository.getAudioStreamUrl(result.url)
                            downloadingVideoUrl = null
                            if (audio != null) {
                                downloadManager.startDownload(
                                    audio.url,
                                    audio.title.ifBlank { result.title },
                                    audio.uploaderName.ifBlank { result.uploaderName ?: "" }
                                )
                            }
                        }
                    }
                )
            }
        }

        Text(
            text = "Download from URL",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Paste a direct link to an MP3 file, or use YouTube search above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = downloadUrl,
            onValueChange = { downloadUrl = it },
            label = { Text("MP3 URL") },
            placeholder = { Text("https://example.com/audio.mp3") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (optional)") },
            placeholder = { Text("Track title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist (optional)") },
            placeholder = { Text("Artist name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val url = downloadUrl.trim()
                if (url.isNotBlank()) {
                    downloadManager.startDownload(url, title.takeIf { it.isNotBlank() }, artist.takeIf { it.isNotBlank() })
                    downloadUrl = ""
                    title = ""
                    artist = ""
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = downloadUrl.trim().isNotBlank()
        ) {
            Text("Download")
        }

        if (progress.isNotEmpty()) {
            Text("Downloading…", style = MaterialTheme.typography.titleSmall)
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
                    Text("${p.percent}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (downloads.isEmpty() && progress.isEmpty() && searchResults.isEmpty() && !searchLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Search YouTube above or paste a direct MP3 URL below to download.",
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
            Button(
                onClick = onDownloadClick,
                enabled = !isDownloading,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (isDownloading) "…" else "Download")
            }
        }
    }
}
