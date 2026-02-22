package com.shuckler.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.shuckler.app.equalizer.EqualizerPreferences
import com.shuckler.app.player.MusicPlayerService
import com.shuckler.app.ui.theme.ShucklerBlack
import com.shuckler.app.ui.theme.ShucklerNeonYellow
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun EqualizerDialog(
    service: MusicPlayerService?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(EqualizerPreferences.isEnabled(context)) }
    var presetIndex by remember { mutableIntStateOf(EqualizerPreferences.getPresetIndex(context)) }
    var bandLevels by remember {
        mutableStateOf(EqualizerPreferences.getEffectiveBandLevelsDb(context))
    }
    var presetMenuExpanded by remember { mutableStateOf(false) }
    var graphSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingBand by remember { mutableStateOf<Int?>(null) }

    fun persistAndApply() {
        EqualizerPreferences.setBandLevelsDb(context, bandLevels)
        EqualizerPreferences.setPresetIndex(context, presetIndex)
        service?.applyEqualizerFromPrefs()
    }

    fun onPresetSelected(index: Int) {
        presetIndex = index
        presetMenuExpanded = false
        if (index in EqualizerPreferences.PRESETS.indices) {
            bandLevels = EqualizerPreferences.PRESETS[index].second.copyOf()
            EqualizerPreferences.setBandLevelsDb(context, bandLevels)
        }
        EqualizerPreferences.setPresetIndex(context, presetIndex)
        service?.applyEqualizerFromPrefs()
    }

    val presetLabel = when {
        presetIndex in EqualizerPreferences.PRESETS.indices -> EqualizerPreferences.PRESETS[presetIndex].first
        else -> "Custom"
    }

    val freqLabels = listOf("60", "150", "400", "1k", "2.4k", "15k")
    val minDb = EqualizerPreferences.MIN_DB
    val maxDb = EqualizerPreferences.MAX_DB
    val rangeDb = maxDb - minDb
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { 4.dp.toPx() }  // ~25% of original 16.dp
    val hitRadiusPx = with(density) { 20.dp.toPx() }  // Touch target stays larger for usability

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equalizer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            EqualizerPreferences.setEnabled(context, it)
                            service?.applyEqualizerFromPrefs()
                        }
                    )
                }

                // Preset dropdown with chevron
                Text(
                    text = "Preset",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { presetMenuExpanded = true }
                            .padding(12.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = presetLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand presets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = presetMenuExpanded,
                        onDismissRequest = { presetMenuExpanded = false }
                    ) {
                        EqualizerPreferences.PRESETS.forEachIndexed { index, (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { onPresetSelected(index) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom") },
                            onClick = {
                                presetIndex = -1
                                EqualizerPreferences.setPresetIndex(context, -1)
                                presetMenuExpanded = false
                                service?.applyEqualizerFromPrefs()
                            }
                        )
                    }
                }

                // Interactive EQ graph: dots on curve, gradient fill, grid lines
                Text(
                    text = "Bands",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .onSizeChanged { graphSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = graphSize.width.toFloat()
                                    val h = graphSize.height.toFloat()
                                    if (w <= 0 || h <= 0) return@detectDragGestures
                                    val n = bandLevels.size
                                    val stepX = w / (n - 1).coerceAtLeast(1)
                                    val band = ((offset.x / stepX).roundToInt()).coerceIn(0, n - 1)
                                    fun dbToY(db: Int): Float =
                                        h * (1f - (db - minDb).toFloat() / rangeDb.coerceAtLeast(1))
                                    val dotY = dbToY(bandLevels[band])
                                    val dist = sqrt((offset.x - band * stepX).pow(2) + (offset.y - dotY).pow(2))
                                    if (dist <= hitRadiusPx) draggingBand = band
                                },
                                onDrag = { change, _ ->
                                    val band = draggingBand ?: return@detectDragGestures
                                    val w = graphSize.width.toFloat()
                                    val h = graphSize.height.toFloat()
                                    if (h <= 0) return@detectDragGestures
                                    val y = (change.position.y).coerceIn(0f, h)
                                    val frac = 1f - y / h
                                    val db = (minDb + frac * rangeDb).roundToInt().coerceIn(minDb, maxDb)
                                    bandLevels = bandLevels.copyOf().apply { this[band] = db }
                                    presetIndex = -1
                                    persistAndApply()
                                },
                                onDragEnd = { draggingBand = null },
                                onDragCancel = { draggingBand = null }
                            )
                        }
                ) {
                    val w = graphSize.width.toFloat()
                    val h = graphSize.height.toFloat()
                    val n = bandLevels.size

                    fun dbToY(db: Int): Float =
                        h * (1f - (db - minDb).toFloat() / rangeDb.coerceAtLeast(1))

                    if (w > 0 && h > 0 && n >= 2) {
                        val stepX = w / (n - 1)
                        val zeroY = dbToY(0)
                        val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

                        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            // 1. Grid: vertical lines at each band, horizontal line at 0dB
                            for (i in 0 until n) {
                                val x = i * stepX
                                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                            }
                            drawLine(gridColor, Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1.5f)

                            // 2. Gradient fill under curve (neon yellow at top -> black at bottom)
                            val fillPath = Path().apply {
                                moveTo(0f, dbToY(bandLevels[0]))
                                for (i in 1 until n) {
                                    lineTo(i * stepX, dbToY(bandLevels[i]))
                                }
                                lineTo((n - 1) * stepX, h)
                                lineTo(0f, h)
                                close()
                            }
                            clipPath(fillPath, clipOp = ClipOp.Intersect) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(ShucklerNeonYellow, ShucklerBlack),
                                        startY = 0f,
                                        endY = h
                                    )
                                )
                            }

                            // 3. Curve line
                            val linePath = Path().apply {
                                moveTo(0f, dbToY(bandLevels[0]))
                                for (i in 1 until n) {
                                    lineTo(i * stepX, dbToY(bandLevels[i]))
                                }
                            }
                            drawPath(
                                linePath,
                                ShucklerNeonYellow.copy(alpha = 0.9f),
                                style = Stroke(width = 2.5f)
                            )

                            // 4. Draggable dots on the curve
                            for (i in 0 until n) {
                                val cx = i * stepX
                                val cy = dbToY(bandLevels[i])
                                drawCircle(
                                    color = ShucklerNeonYellow,
                                    radius = dotRadiusPx,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                    radius = dotRadiusPx * 0.6f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }

                // Frequency labels below graph
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    freqLabels.forEach { label ->
                        Text(
                            text = label + " Hz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
