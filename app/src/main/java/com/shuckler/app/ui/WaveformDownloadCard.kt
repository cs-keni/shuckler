package com.shuckler.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shuckler.app.download.DownloadProgress
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.ui.theme.Border
import com.shuckler.app.ui.theme.Green
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Red
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3
import kotlin.random.Random

private const val BAR_COUNT = 40

@Composable
fun WaveformDownloadCard(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    progress: DownloadProgress,
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val animatedPercent by animateFloatAsState(
        targetValue = progress.percent / 100f,
        animationSpec = tween(400),
        label = "waveformFill"
    )

    // Stable randomized bar heights — seeded by title so they're consistent per track
    val barHeights = remember(title) {
        val rng = Random(title.hashCode())
        FloatArray(BAR_COUNT) { rng.nextFloat() * 0.75f + 0.25f }
    }

    val speedLabel = if (progress.bytesPerSecond > 0) formatDownloadSpeed(progress.bytesPerSecond) else null
    val statusLabel = when (status) {
        DownloadStatus.PENDING -> "QUEUED"
        DownloadStatus.DOWNLOADING -> null
        DownloadStatus.COMPLETED -> "DONE"
        DownloadStatus.FAILED -> "FAILED"
    }
    val statusColor = when (status) {
        DownloadStatus.COMPLETED -> Green
        DownloadStatus.FAILED -> Red
        DownloadStatus.PENDING -> Text3
        DownloadStatus.DOWNLOADING -> accentColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: art + title + percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            modifier = Modifier.size(18.dp), tint = Text2)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        color = Text1, maxLines = 1)
                    Text(artist, style = MaterialTheme.typography.bodySmall,
                        color = Text2, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (statusLabel != null) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor
                        )
                    } else {
                        Text(
                            text = "${progress.percent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                    }
                    if (speedLabel != null) {
                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Text3
                        )
                    }
                }
            }

            // Waveform frontier visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                val barW = (size.width - (BAR_COUNT - 1) * 2.dp.toPx()) / BAR_COUNT
                val gap = 2.dp.toPx()
                val filledCount = (animatedPercent * BAR_COUNT).toInt()
                val frontierIndex = filledCount.coerceIn(0, BAR_COUNT - 1)

                barHeights.forEachIndexed { i, heightFrac ->
                    val barH = size.height * heightFrac
                    val x = i * (barW + gap)
                    val y = (size.height - barH) / 2f
                    val isFilled = i < filledCount
                    val isFrontier = i == frontierIndex && animatedPercent > 0f

                    val color = when {
                        isFrontier -> accentColor
                        isFilled -> accentColor.copy(alpha = 0.55f)
                        else -> Text3.copy(alpha = 0.4f)
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )

                    // Glow on frontier bar
                    if (isFrontier) {
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.3f),
                            topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                            size = Size(barW + 4.dp.toPx(), barH + 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }
        }
    }
}

private fun formatDownloadSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000 -> "${bytesPerSecond / 1_000} KB/s"
        else -> "$bytesPerSecond B/s"
    }
}
