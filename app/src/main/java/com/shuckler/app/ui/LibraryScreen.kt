package com.shuckler.app.ui

import android.net.Uri
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val downloadManager = LocalDownloadManager.current
    val downloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val completedTracks = downloads.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium
        )
        if (completedTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No downloaded tracks yet. Use Search to download from an MP3 URL.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(completedTracks, key = { it.id }) { track ->
                    LibraryTrackItem(
                        track = track,
                        onPlayClick = {
                            val uri = Uri.fromFile(File(track.filePath))
                            viewModel.playTrack(uri, track.title, track.artist)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTrackItem(
    track: DownloadedTrack,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
