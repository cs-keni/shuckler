package com.shuckler.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CropCoverDialog(
    imageUri: String,
    onCropComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(android.net.Uri.parse(imageUri))?.use { input ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inJustDecodeBounds = false
                    }
                    BitmapFactory.decodeStream(input, null, opts)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val bmp = bitmap
    if (bmp == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading…", color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    val imgW = bmp.width.toFloat()
    val imgH = bmp.height.toFloat()
    val minDim = min(imgW, imgH)
    val defaultCropSize = (minDim * 0.85f).coerceIn(minDim * 0.25f, minDim)
    var cropOffsetX by remember { mutableFloatStateOf((imgW - defaultCropSize) / 2f) }
    var cropOffsetY by remember { mutableFloatStateOf((imgH - defaultCropSize) / 2f) }
    var cropSize by remember { mutableFloatStateOf(defaultCropSize) }
    var dragMode by remember { mutableStateOf(false) } // false = move, true = resize

    fun constrainCrop() {
        cropSize = cropSize.coerceIn(minDim * 0.2f, minDim)
        cropOffsetX = cropOffsetX.coerceIn(0f, imgW - cropSize)
        cropOffsetY = cropOffsetY.coerceIn(0f, imgH - cropSize)
    }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Text("Crop cover", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = {
                try {
                    val sw = cropSize.toInt().coerceAtLeast(1)
                    val sh = cropSize.toInt().coerceAtLeast(1)
                    val cropped = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(cropped)
                    val src = android.graphics.Rect(
                        cropOffsetX.toInt(),
                        cropOffsetY.toInt(),
                        (cropOffsetX + cropSize).toInt().coerceAtMost(bmp.width),
                        (cropOffsetY + cropSize).toInt().coerceAtMost(bmp.height)
                    )
                    val dst = android.graphics.Rect(0, 0, sw, sh)
                    canvas.drawBitmap(bmp, src, dst, null)
                    val maxOut = 512
                    val out = if (cropped.width > maxOut || cropped.height > maxOut) {
                        val s = min(maxOut.toFloat() / cropped.width, maxOut.toFloat() / cropped.height)
                        Bitmap.createScaledBitmap(cropped, (cropped.width * s).toInt(), (cropped.height * s).toInt(), true)
                    } else cropped
                    if (out != cropped) cropped.recycle()
                    onCropComplete(out)
                } catch (_: Exception) {
                    onDismiss()
                }
            }) { Text("Done") }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { layoutSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val lw = layoutSize.width.toFloat()
                            val lh = layoutSize.height.toFloat()
                            if (lw <= 0 || lh <= 0) return@detectDragGestures
                            val scale = min(lw / imgW, lh / imgH)
                            val drawW = imgW * scale
                            val drawH = imgH * scale
                            val dx = (lw - drawW) / 2f
                            val dy = (lh - drawH) / 2f
                            val bx = (offset.x - dx) / scale
                            val by = (offset.y - dy) / scale
                            val cornerX = cropOffsetX + cropSize
                            val cornerY = cropOffsetY + cropSize
                            val distToCorner = sqrt((bx - cornerX).pow(2) + (by - cornerY).pow(2))
                            dragMode = distToCorner < cropSize * 0.4f
                        },
                        onDrag = { change, dragAmount ->
                            val lw = layoutSize.width.toFloat()
                            val lh = layoutSize.height.toFloat()
                            if (lw <= 0 || lh <= 0) return@detectDragGestures
                            val scale = min(lw / imgW, lh / imgH)
                            val dx = (lw - imgW * scale) / 2f
                            val dy = (lh - imgH * scale) / 2f
                            val deltaBx = -dragAmount.x / scale
                            val deltaBy = -dragAmount.y / scale
                            if (dragMode) {
                                cropSize += (deltaBx + deltaBy) * 0.5f
                            } else {
                                cropOffsetX += deltaBx
                                cropOffsetY += deltaBy
                            }
                            constrainCrop()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0 || size.height <= 0) return@Canvas
                val scale = min(size.width / imgW, size.height / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                val offsetX = (size.width - drawW) / 2f
                val offsetY = (size.height - drawH) / 2f

                val cropLeft = offsetX + cropOffsetX * scale
                val cropTop = offsetY + cropOffsetY * scale
                val cropW = cropSize * scale
                val cropH = cropSize * scale
                val cornerRadius = 24f

                val path = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(
                        RoundRect(
                            rect = Rect(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH),
                            radiusX = cornerRadius,
                            radiusY = cornerRadius
                        )
                    )
                }
                clipPath(path, clipOp = ClipOp.Difference) {
                    drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                }

                val imgBitmap = bmp.asImageBitmap()
                drawImage(
                    imgBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
                )

                drawRect(
                    color = androidx.compose.ui.graphics.Color(0xFFE8B323).copy(alpha = 0.25f),
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(cropW, cropH)
                )
            }
        }
    }
    }
    }
}
