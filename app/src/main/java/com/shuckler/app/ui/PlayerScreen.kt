package com.shuckler.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onSizeChanged
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.shuckler.app.accessibility.LocalAccessibilityPreferences
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.lyrics.LyricsResult
import com.shuckler.app.ui.EqualizerDialog
import com.shuckler.app.player.DefaultTrackInfo
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.palette.graphics.Palette
import com.shuckler.app.ui.theme.ShucklerBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onCollapse: (() -> Unit)? = null,
    fromMiniPlayer: Boolean = false,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
) {
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val trackTitle by viewModel.currentTrackTitle.collectAsState(initial = DefaultTrackInfo.TITLE)
    val trackArtist by viewModel.currentTrackArtist.collectAsState(initial = DefaultTrackInfo.ARTIST)
    val repeatMode by viewModel.repeatMode.collectAsState(initial = Player.REPEAT_MODE_OFF)
    val positionMs by viewModel.playbackPositionMs.collectAsState(initial = 0L)
    val durationMs by viewModel.durationMs.collectAsState(initial = 0L)
    val queueInfo by viewModel.queueInfo.collectAsState(initial = 0 to 0)
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList())
    val thumbnailUrl by viewModel.currentTrackThumbnailUrl.collectAsState(initial = null)
    val playbackSpeed by viewModel.playbackSpeed.collectAsState(initial = 1f)
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState(initial = null)
    val lyricsResult by viewModel.lyricsResult.collectAsState(initial = LyricsResult.NotFound)
    val visualizerFftData by viewModel.visualizerFftData.collectAsState(initial = null)
    val downloadManager = LocalDownloadManager.current
    var albumColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(thumbnailUrl) {
        albumColor = thumbnailUrl?.let { url ->
            withContext(Dispatchers.IO) {
                try {
                    val conn = java.net.URL(url).openConnection()
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val stream = conn.getInputStream()
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    stream?.close()
                    bitmap?.let { bmp ->
                        Palette.from(bmp).generate().dominantSwatch?.rgb?.let { rgb ->
                            Color(0xFF000000.toInt() or rgb)
                        }
                    }
                } catch (_: Exception) { null }
            }
        }
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val musicService by LocalMusicServiceConnection.current.service.collectAsState(initial = null)
    val view = LocalView.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            viewModel.updatePlaybackProgress()
        }
    }
    val isDefaultTrack = trackTitle == DefaultTrackInfo.TITLE && trackArtist == DefaultTrackInfo.ARTIST
    LaunchedEffect(trackTitle, trackArtist) {
        if (!isDefaultTrack) viewModel.loadLyrics(trackArtist, trackTitle)
    }

    if (showQueueSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                val currentIndex = (queueInfo.first - 1).coerceIn(0, queueItems.size)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(queueItems) { index, item ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playQueueItemAt(index)
                                    showQueueSheet = false
                                }
                                .padding(12.dp)
                                .then(
                                    if (isCurrent) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "Now playing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.reorderQueue(index, (index - 1).coerceAtLeast(0)) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.reorderQueue(index, (index + 1).coerceAtMost(queueItems.size - 1)) },
                                    enabled = index < queueItems.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val accessibilityPrefs = LocalAccessibilityPreferences.current
    val reduceMotion by accessibilityPrefs.reduceMotionFlow.collectAsState(initial = accessibilityPrefs.reduceMotion)
    if (showSettingsDialog) {
        SettingsDialog(
            autoDeleteAfterPlayback = downloadManager.autoDeleteAfterPlayback,
            onAutoDeleteChange = { downloadManager.autoDeleteAfterPlayback = it },
            crossfadeDurationMs = downloadManager.crossfadeDurationMs,
            onCrossfadeChange = { downloadManager.crossfadeDurationMs = it },
            downloadQuality = downloadManager.downloadQuality,
            onDownloadQualityChange = { downloadManager.downloadQuality = it },
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            onStartSleepTimer = { durationMs, endOfTrack -> viewModel.startSleepTimer(durationMs, endOfTrack) },
            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
            sleepTimerFadeLastMinute = downloadManager.sleepTimerFadeLastMinute,
            onSleepTimerFadeChange = { downloadManager.sleepTimerFadeLastMinute = it },
            onEqualizerClick = { showSettingsDialog = false; showEqualizerDialog = true },
            reduceMotion = accessibilityPrefs.reduceMotion,
            onReduceMotionChange = { accessibilityPrefs.reduceMotion = it },
            highContrast = accessibilityPrefs.highContrast,
            onHighContrastChange = { accessibilityPrefs.highContrast = it },
            onDismiss = { showSettingsDialog = false }
        )
    }
    if (showEqualizerDialog) {
        EqualizerDialog(
            service = musicService,
            onDismiss = { showEqualizerDialog = false }
        )
    }

    val topGradientColor = albumColor?.let { lerp(it, Color.Black, 0.4f) } ?: ShucklerBlack
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topGradientColor, ShucklerBlack),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCollapse != null) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Collapse player",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!reduceMotion) {
                visualizerFftData?.let { fft ->
                    AudioVisualizerCanvas(
                        fftData = fft,
                        modifier = Modifier.fillMaxSize(),
                        barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        barCount = 32
                    )
                }
            }
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Album art for $trackTitle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(260.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "No album art",
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = trackTitle,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = trackArtist,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (queueInfo.second > 1) {
            Text(
                text = "Track ${queueInfo.first} of ${queueInfo.second}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (queueItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { showQueueSheet = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "View queue",
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Queue (${queueItems.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isDefaultTrack) {
            LyricsSection(
                lyricsResult = lyricsResult,
                positionMs = positionMs
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPlaybackTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        var isSeekDragging by remember { mutableStateOf(false) }
        var seekPosition by remember { mutableStateOf(0f) }
        val maxDuration = (durationMs.takeIf { it > 0 } ?: 1L).toFloat()
        var seekBarWidthPx by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { seekBarWidthPx = it.width }
                .pointerInput(seekBarWidthPx) {
                    if (seekBarWidthPx <= 0) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val isLeft = offset.x < seekBarWidthPx / 2
                            val deltaMs = if (isLeft) -10_000L else 10_000L
                            val newPos = (positionMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
                            viewModel.seekTo(newPos)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                    )
                }
        ) {
            Slider(
                value = if (isSeekDragging) seekPosition else positionMs.toFloat().coerceIn(0f, maxDuration),
                onValueChange = {
                    seekPosition = it
                    isSeekDragging = true
                },
                onValueChangeFinished = {
                    viewModel.seekTo(seekPosition.toLong())
                    isSeekDragging = false
                },
                valueRange = 0f..maxDuration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.skipToPrevious()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            FilledIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.togglePlayPause()
                },
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.skipToNext()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
        fun speedLabel(s: Float) =
            if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"
        var speedMenuExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = repeatMode == Player.REPEAT_MODE_ONE,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.cycleLoopMode()
                },
                label = { Text("Loop") }
            )
            Box {
                FilterChip(
                    selected = false,
                    onClick = { speedMenuExpanded = true },
                    label = { Text(speedLabel(playbackSpeed)) }
                )
                DropdownMenu(
                    expanded = speedMenuExpanded,
                    onDismissRequest = { speedMenuExpanded = false }
                ) {
                    speedOptions.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text(speedLabel(speed)) },
                            onClick = {
                                viewModel.setPlaybackSpeed(speed)
                                speedMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (sleepTimerRemainingMs != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sleepTimerRemainingMs == com.shuckler.app.player.MusicPlayerService.SLEEP_TIMER_END_OF_TRACK)
                        "Sleep: at end of track"
                    else
                        "Sleep: stops in ${sleepTimerRemainingMs!! / 60_000} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.cancelSleepTimer() }
                ) {
                    Text("Cancel")
                }
            }
        }

    }
    }
}

@Composable
private fun LyricsSection(
    lyricsResult: LyricsResult,
    positionMs: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (lyricsResult) {
            is LyricsResult.Loading -> {
                Text(
                    text = "Loading lyrics…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            is LyricsResult.NotFound -> {
                Box(modifier = Modifier.heightIn(min = 48.dp))
            }
            is LyricsResult.Plain -> {
                // Plain lyrics: show full text scrollable (no sync possible)
                val scrollState = rememberScrollState()
                Text(
                    text = lyricsResult.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 96.dp)
                        .verticalScroll(scrollState)
                )
            }
            is LyricsResult.Synced -> {
                val lines = lyricsResult.lines
                val currentIndex = lines.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
                val nextIndex = (currentIndex + 1).coerceAtMost(lines.lastIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Main line (currently playing) - prominent, slides up when exiting
                        AnimatedContent(
                            targetState = currentIndex,
                            transitionSpec = {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 280),
                                    initialOffsetY = { 48 }
                                ) + fadeIn(animationSpec = tween(durationMillis = 280))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(durationMillis = 280),
                                        targetOffsetY = { -48 }
                                    ) + fadeOut(animationSpec = tween(durationMillis = 280)))
                            },
                            label = "lyric_main"
                        ) { index ->
                            val rawText = lines.getOrNull(index)?.text?.trim() ?: ""
                            val text = if (rawText.isBlank()) "♪ ♪ ♪" else rawText
                            Text(
                                text = text,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Next line (about to play) - smaller, faded, doesn't compete with main
                        val nextText = if (nextIndex > currentIndex) lines.getOrNull(nextIndex)?.text?.takeIf { it.isNotBlank() } else null
                        AnimatedContent(
                            targetState = nextIndex,
                            transitionSpec = {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 280),
                                    initialOffsetY = { 24 }
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(durationMillis = 280),
                                        targetOffsetY = { -24 }
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200)))
                            },
                            label = "lyric_next"
                        ) { idx ->
                            val raw = if (idx > currentIndex) lines.getOrNull(idx)?.text?.trim() else null
                            val lineText = when {
                                raw == null -> null
                                raw.isBlank() -> "♪ ♪ ♪"
                                else -> raw
                            }
                            if (lineText != null) {
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(0.5f)
                                )
                            } else {
                                Box(modifier = Modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioVisualizerCanvas(
    fftData: ByteArray,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White.copy(alpha = 0.3f),
    barCount: Int = 32
) {
    val magnitudes = remember(fftData, barCount) {
        if (fftData.size < 4) return@remember FloatArray(0)
        val result = FloatArray(barCount)
        val numBuckets = (fftData.size - 2) / 2
        if (numBuckets <= 0) return@remember result
        for (i in 0 until barCount) {
            val bucketIdx = (i * numBuckets / barCount).coerceIn(0, numBuckets - 1)
            val r = fftData[2 + bucketIdx * 2].toInt()
            val im = fftData[2 + bucketIdx * 2 + 1].toInt()
            val magnitude = kotlin.math.hypot(r.toDouble(), im.toDouble()).toFloat() / 128f
            result[i] = magnitude.coerceIn(0f, 1f)
        }
        result
    }
    Canvas(modifier = modifier) {
        if (magnitudes.isEmpty()) return@Canvas
        val barWidth = size.width / (barCount * 2 - 1)
        val gap = barWidth * 0.5f
        val maxBarHeight = size.height * 0.4f
        for (i in magnitudes.indices) {
            val x = (i * (barWidth + gap)) + gap
            val h = magnitudes[i] * maxBarHeight
            val top = (size.height - h) / 2
            drawRect(
                color = barColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, h)
            )
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = (ms / 1000).toInt()
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

