package com.shuckler.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem

@Composable
fun MiniPlayerBar(
    onTap: () -> Unit,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val trackTitle by viewModel.currentTrackTitle.collectAsState(initial = "")
    val trackArtist by viewModel.currentTrackArtist.collectAsState(initial = "")
    val thumbnailUrl by viewModel.currentTrackThumbnailUrl.collectAsState(initial = null)
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList())
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val view = LocalView.current
    val currentIndex = (queueInfo.first - 1).coerceIn(0, queueItems.size)
    val upNextItems = queueItems.drop(currentIndex + 1).take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable(onClick = onTap)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = trackTitle.ifBlank { "No track" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = trackArtist.ifBlank { "" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                viewModel.togglePlayPause()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                viewModel.skipToNext()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
        if (upNextItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upNextItems.forEachIndexed { offset, item ->
                    val targetIndex = currentIndex + 1 + offset
                    Row(
                        modifier = Modifier
                            .clickable {
                                viewModel.playQueueItemAt(targetIndex)
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.thumbnailUrl != null) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
