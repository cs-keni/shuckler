package com.shuckler.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.ui.theme.Amber
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.SurfaceHigh
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

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
    val thumbnailUrl by viewModel.currentTrackThumbnailUrl.collectAsState(initial = null)
    val positionMs by viewModel.playbackPositionMs.collectAsState(initial = 0L)
    val durationMs by viewModel.durationMs.collectAsState(initial = 0L)
    val fftData by viewModel.visualizerFftData.collectAsState(initial = null)
    val view = LocalView.current
    val context = LocalContext.current
    val accentColor = com.shuckler.app.ui.theme.LocalAccentColor.current

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            viewModel.updatePlaybackProgress()
            delay(16)
        }
    }

    val rawProgress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "pill_progress")

    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val playPauseIsPressed by playPauseInteractionSource.collectIsPressedAsState()
    val playPauseScale by animateFloatAsState(
        targetValue = if (playPauseIsPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playPauseScale"
    )

    val skipInteractionSource = remember { MutableInteractionSource() }
    val skipIsPressed by skipInteractionSource.collectIsPressedAsState()
    val skipScale by animateFloatAsState(
        targetValue = if (skipIsPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "skipScale"
    )

    // Whole-pill press scale
    val pillInteractionSource = remember { MutableInteractionSource() }
    val pillIsPressed by pillInteractionSource.collectIsPressedAsState()
    val pillScale by animateFloatAsState(
        targetValue = if (pillIsPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillScale"
    )

    // Floating pill container with horizontal padding to create the "floating" effect
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .scale(pillScale)
            .pointerInput(onTap) {
                // Swipe-up gesture: threshold 36dp, non-consuming (coexists with tap)
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalY = 0f
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        totalY += change.positionChange().y
                        if (totalY < -36.dp.toPx() && !fired) {
                            fired = true
                            onTap()
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceHigh)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = pillInteractionSource,
                    indication = null,
                    onClick = onTap
                )
        ) {
            // Progress line along the bottom edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(animatedProgress)
                    .height(1.5.dp)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Album art — 34dp, 7dp radius
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Text2
                        )
                    }
                }

                // Track title — DM Serif Display via MaterialTheme.typography.titleSmall
                Text(
                    text = trackTitle.ifBlank { "Nothing playing" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    color = Text1,
                    modifier = Modifier.weight(1f)
                )

                // Live amplitude bars (8 bars, 2dp wide, driven by FFT)
                if (isPlaying) {
                    AmplitudeBars(
                        fftData = fftData,
                        accentColor = accentColor,
                        modifier = Modifier
                            .size(width = 26.dp, height = 20.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Play/pause — 30dp circular button
                IconButton(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        viewModel.togglePlayPause()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .scale(playPauseScale),
                    interactionSource = playPauseInteractionSource
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Skip next
                IconButton(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.skipToNext()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .scale(skipScale),
                    interactionSource = skipInteractionSource
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Text2,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmplitudeBars(
    fftData: ByteArray?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Animate bar heights; randomize when no FFT to keep feel alive
    var barHeights by remember { mutableStateOf(FloatArray(8) { 0.3f }) }

    LaunchedEffect(fftData) {
        if (fftData != null && fftData.size >= 8) {
            val step = fftData.size / 8
            barHeights = FloatArray(8) { i ->
                (abs(fftData[i * step].toInt()) / 128f).coerceIn(0.1f, 1f)
            }
        } else {
            // Randomize slightly so bars look alive even without FFT
            barHeights = FloatArray(8) { (Random.nextFloat() * 0.5f + 0.15f) }
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val gap = (size.width - barWidth * 8) / 7
        barHeights.forEachIndexed { i, heightFraction ->
            val barH = size.height * heightFraction
            val x = i * (barWidth + gap)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = accentColor.copy(alpha = 0.85f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
    }
}
