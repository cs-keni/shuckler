package com.shuckler.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.math.sin

private data class Firefly(
    val baseXFrac: Float,
    val baseYFrac: Float,
    val driftXAmp: Float,
    val driftYAmp: Float,
    val driftXFreq: Float,
    val driftYFreq: Float,
    val driftXPhase: Float,
    val driftYPhase: Float,
    val blinkFreq: Float,
    val blinkPhase: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val glowRadius: Float,
)

private fun buildFireflies(count: Int = 28): List<Firefly> {
    val rng = java.util.Random(0x5F3759DFL)
    fun f() = rng.nextFloat()
    val tau = (2.0 * Math.PI).toFloat()
    return List(count) {
        Firefly(
            baseXFrac = f(),
            baseYFrac = f(),
            driftXAmp = f() * 0.07f + 0.02f,
            driftYAmp = f() * 0.07f + 0.02f,
            driftXFreq = (f() * 0.25f + 0.08f) * 0.001f,
            driftYFreq = (f() * 0.25f + 0.08f) * 0.001f,
            driftXPhase = f() * tau,
            driftYPhase = f() * tau,
            blinkFreq = (f() * 0.35f + 0.15f) * 0.001f,
            blinkPhase = f() * tau,
            minAlpha = f() * 0.03f,
            maxAlpha = f() * 0.18f + 0.06f,
            glowRadius = f() * 22f + 14f,
        )
    }
}

private val fireflies = buildFireflies()

@Composable
fun FireflyBackground(modifier: Modifier = Modifier) {
    var time by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (isActive) {
            withFrameMillis { time = System.currentTimeMillis() - start }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.toFloat()
        fireflies.forEach { f ->
            val cx = (f.baseXFrac + f.driftXAmp * sin(f.driftXFreq * t + f.driftXPhase)) * size.width
            val cy = (f.baseYFrac + f.driftYAmp * sin(f.driftYFreq * t + f.driftYPhase)) * size.height
            val sinBlink = sin(f.blinkFreq * t + f.blinkPhase)
            val alpha = f.minAlpha + (f.maxAlpha - f.minAlpha) * (sinBlink * 0.5f + 0.5f)
            val radius = f.glowRadius * density

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF5CC).copy(alpha = alpha),
                        Color(0xFFFFDDA0).copy(alpha = alpha * 0.3f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}
