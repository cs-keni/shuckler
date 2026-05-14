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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import kotlin.random.Random

@Composable
fun ArtistDetailScreen(
    artistName: String,
    tracks: List<DownloadedTrack>,
    currentPlayingTrackId: String?,
    onBack: () -> Unit,
    onPlayTracks: (tracks: List<DownloadedTrack>, index: Int) -> Unit
) {
    val artistTracks = remember(tracks) {
        tracks.sortedWith(compareByDescending<DownloadedTrack> { it.playCount }.thenBy { it.title.lowercase() })
    }
    val heroArt = remember(artistTracks) { artistTracks.firstOrNull { it.thumbnailUrl != null }?.thumbnailUrl }
    val albumGroups = remember(artistTracks) {
        artistTracks
            .groupBy { albumGroupKey(it) }
            .map { (key, groupTracks) ->
                val sortedTracks = groupTracks.sortedBy { track -> track.title.lowercase() }
                val firstTrack = sortedTracks.first()
                ArtistAlbumGroup(
                    key = key,
                    title = firstTrack.albumTitle?.takeIf { it.isNotBlank() } ?: firstTrack.title,
                    year = firstTrack.albumYear,
                    artworkUrl = firstTrack.thumbnailUrl,
                    tracks = sortedTracks
                )
            }
            .sortedWith(
                compareByDescending<ArtistAlbumGroup> { group -> group.tracks.sumOf { it.playCount } }
                    .thenBy { it.title.lowercase() }
            )
    }
    var selectedAlbumTracks by remember { mutableStateOf<List<DownloadedTrack>?>(null) }
    val accent = LocalAccentColor.current

    selectedAlbumTracks?.let { albumTracks ->
        AlbumDetailScreen(
            tracks = albumTracks,
            currentPlayingTrackId = currentPlayingTrackId,
            onBack = { selectedAlbumTracks = null },
            onPlayTracks = onPlayTracks
        )
        return
    }

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
                if (heroArt != null) {
                    AsyncImage(
                        model = heroArt,
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
                                    Base.copy(alpha = 0.42f),
                                    Base.copy(alpha = 0.72f),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Text2)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Text1,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artistTracks.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                    onClick = { if (artistTracks.isNotEmpty()) onPlayTracks(artistTracks, 0) },
                    enabled = artistTracks.isNotEmpty(),
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
                        if (artistTracks.isNotEmpty()) {
                            val shuffled = artistTracks.shuffled(Random(System.currentTimeMillis()))
                            onPlayTracks(shuffled, 0)
                        }
                    },
                    enabled = artistTracks.isNotEmpty(),
                    border = BorderStroke(1.dp, Border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = Text1, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelLarge, color = Text1)
                }
            }
        }

        item {
            SectionLabel("Songs")
        }

        itemsIndexed(artistTracks, key = { _, track -> track.id }) { index, track ->
            ArtistSongRow(
                index = index + 1,
                track = track,
                isPlaying = track.id == currentPlayingTrackId,
                onClick = { onPlayTracks(artistTracks, index) }
            )
        }

        if (albumGroups.isNotEmpty()) {
            item {
                SectionLabel("Albums")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albumGroups, key = { group -> group.key }) { group ->
                        ArtistAlbumCard(
                            album = group,
                            onClick = { selectedAlbumTracks = group.tracks }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Text1,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun ArtistSongRow(
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
                if (isPlaying) LocalAccentColor.current.copy(alpha = 0.12f) else Base
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isPlaying) LocalAccentColor.current else Text2,
            modifier = Modifier.width(24.dp)
        )
        if (track.thumbnailUrl != null) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Text2, modifier = Modifier.size(20.dp))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlaying) LocalAccentColor.current else Text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    formatArtistDuration(track.durationMs),
                    if (track.playCount > 0) "${track.playCount} plays" else null
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: ArtistAlbumGroup,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        if (album.artworkUrl != null) {
            AsyncImage(
                model = album.artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Text2, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            text = album.title,
            style = MaterialTheme.typography.labelMedium,
            color = Text1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = listOfNotNull(album.year?.toString(), "${album.tracks.size} songs").joinToString(" / "),
            style = MaterialTheme.typography.labelSmall,
            color = Text2,
            maxLines = 1
        )
    }
}

private data class ArtistAlbumGroup(
    val key: String,
    val title: String,
    val year: Int?,
    val artworkUrl: String?,
    val tracks: List<DownloadedTrack>
)

private fun albumGroupKey(track: DownloadedTrack): String {
    val albumTitle = track.albumTitle?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    return if (albumTitle != null) {
        "album:${track.artist.trim().lowercase()}:$albumTitle"
    } else {
        "art:${track.thumbnailUrl ?: track.id}"
    }
}

private fun formatArtistDuration(ms: Long): String? {
    if (ms <= 0) return null
    val totalSec = (ms / 1000).toInt()
    val seconds = totalSec % 60
    val minutes = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
