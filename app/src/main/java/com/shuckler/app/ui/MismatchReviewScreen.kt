package com.shuckler.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.spotify.ImportTrackRecord
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import com.shuckler.app.youtube.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MismatchReviewScreen(
    importId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? ShucklerApplication
    val importManager = app?.spotifyImportManager
    val notFoundTracks = remember(importId) {
        importManager?.getNotFoundTracks(importId) ?: emptyList()
    }
    val onWifiOnlyBlocked: () -> Unit = {}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Text1)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "These ${notFoundTracks.size} songs need a manual match",
                    style = MaterialTheme.typography.titleMedium,
                    color = Text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Search YouTube or paste a URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2
                )
            }
        }

        if (notFoundTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All tracks matched!", style = MaterialTheme.typography.titleMedium, color = Text1)
                    Text("Your library is complete.", style = MaterialTheme.typography.bodySmall, color = Text2)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notFoundTracks, key = { it.trackTitle + it.trackArtist }) { record ->
                    MismatchTrackRow(
                        record = record,
                        importId = importId,
                        onResolved = { importManager?.resolveNotFoundTrack(importId, record, it, onWifiOnlyBlocked) }
                    )
                }
            }
        }

        // Sticky footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = LocalAccentColor.current,
                    contentColor = Base
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("My library looks good ✓")
            }
            CancelSpotifyButton(onDone = onDone)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MismatchTrackRow(
    record: ImportTrackRecord,
    importId: String,
    onResolved: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = LocalAccentColor.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.trackTitle, style = MaterialTheme.typography.titleSmall, color = Text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.trackArtist, style = MaterialTheme.typography.bodySmall, color = Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (resolved) {
                Text("Queued ✓", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }

        if (!resolved) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; searchError = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste YouTube URL or search query", color = Text3, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Text1),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (urlInput.isNotBlank()) {
                            handleMismatchSubmit(
                                input = urlInput,
                                record = record,
                                scope = scope,
                                setSearching = { isSearching = it },
                                setError = { searchError = it },
                                setResolved = { resolved = it },
                                onResolved = onResolved
                            )
                        }
                    }
                ),
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent)
                    } else {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            if (urlInput.isNotBlank()) {
                                handleMismatchSubmit(
                                    input = urlInput,
                                    record = record,
                                    scope = scope,
                                    setSearching = { isSearching = it },
                                    setError = { searchError = it },
                                    setResolved = { resolved = it },
                                    onResolved = onResolved
                                )
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = accent)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Text1,
                    unfocusedTextColor = Text1,
                    focusedContainerColor = Base,
                    unfocusedContainerColor = Base,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Border
                ),
                shape = RoundedCornerShape(6.dp)
            )
            searchError?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = com.shuckler.app.ui.theme.Red)
            }
        }
    }
}

private fun handleMismatchSubmit(
    input: String,
    record: ImportTrackRecord,
    scope: kotlinx.coroutines.CoroutineScope,
    setSearching: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setResolved: (Boolean) -> Unit,
    onResolved: (String) -> Unit
) {
    val isUrl = input.startsWith("http://") || input.startsWith("https://")
    setSearching(true)
    setError(null)
    scope.launch {
        if (isUrl) {
            setSearching(false)
            setResolved(true)
            onResolved(input)
        } else {
            val results = try {
                withContext(Dispatchers.IO) { YouTubeRepository.search(input) }
            } catch (_: Exception) { emptyList() }
            setSearching(false)
            val best = results.firstOrNull()
            if (best != null) {
                setResolved(true)
                onResolved(best.url)
            } else {
                setError("No results found — try a different search or paste a URL")
            }
        }
    }
}
