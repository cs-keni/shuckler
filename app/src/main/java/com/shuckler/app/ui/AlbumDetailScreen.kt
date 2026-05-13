package com.shuckler.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.LocalAccentColor
import kotlin.random.Random

@Composable
fun AlbumDetailScreen(
    tracks: List<DownloadedTrack>,
    currentPlayingTrackId: String?,
    onBack: () -> Unit,
    onPlayTracks: (tracks: List<DownloadedTrack>, index: Int) -> Unit
) {
    val albumTracks = remember(tracks) {
        tracks.sortedWith(compareBy<DownloadedTrack> { it.title.lowercase() })
    }
    val firstTrack = albumTracks.firstOrNull()
    val albumTitle = firstTrack?.albumTitle?.takeIf { it.isNotBlank() } ?: firstTrack?.title.orEmpty()
    val artist = firstTrack?.artist.orEmpty()
    val artworkUrl = firstTrack?.thumbnailUrl
    val totalRuntimeMs = albumTracks.sumOf { it.durationMs.coerceAtLeast(0L) }
    val accent = LocalAccentColor.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Base),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(26.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Base.copy(alpha = 0.36f),
                                    Base.copy(alpha = 0.76f),
                                    Base
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AlbumArtwork(artworkUrl = artworkUrl, sizeDp = 72)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = albumTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(
                                artist.takeIf { it.isNotBlank() },
                                firstTrack?.albumYear?.toString(),
                                "${albumTracks.size} songs",
                                formatAlbumDuration(totalRuntimeMs)
                            ).joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (albumTracks.isNotEmpty()) onPlayTracks(albumTracks, 0) },
                    enabled = albumTracks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Base
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        if (albumTracks.isNotEmpty()) {
                            onPlayTracks(albumTracks.shuffled(Random(System.currentTimeMillis())), 0)
                        }
                    },
                    enabled = albumTracks.isNotEmpty(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        itemsIndexed(albumTracks, key = { _, track -> track.id }) { index, track ->
            AlbumTrackRow(
                index = index + 1,
                track = track,
                isPlaying = track.id == currentPlayingTrackId,
                onClick = { onPlayTracks(albumTracks, index) }
            )
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    track: DownloadedTrack,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isPlaying) LocalAccentColor.current.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isPlaying) LocalAccentColor.current else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )
        AlbumArtwork(artworkUrl = track.thumbnailUrl, sizeDp = 36)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlaying) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    track.artist,
                    formatAlbumDuration(track.durationMs)
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    artworkUrl: String?,
    sizeDp: Int
) {
    if (artworkUrl != null) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}

private fun formatAlbumDuration(ms: Long): String? {
    if (ms <= 0) return null
    val totalSec = (ms / 1000).toInt()
    val seconds = totalSec % 60
    val minutes = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
