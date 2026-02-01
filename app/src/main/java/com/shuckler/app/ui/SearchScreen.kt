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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.LocalDownloadManager

@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val downloadManager = LocalDownloadManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val progress by downloadManager.progress.collectAsState(initial = emptyMap())
    val scrollState = rememberScrollState()

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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            placeholder = { Text("Search for music... (Phase 6)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )

        Text(
            text = "Download from URL",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Paste a direct link to an MP3 file (not a YouTube link — that\u2019s Phase 6).",
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

        if (downloads.isEmpty() && progress.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enter an MP3 URL above to download. Search results (Phase 6) will appear here.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Example: search the web for \"free sample mp3 download\" or use a direct .mp3 link from a site that hosts audio files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
