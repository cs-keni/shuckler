package com.shuckler.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.shuckler.app.ui.theme.LocalAccentColor

@Composable fun accentAmbient()    = LocalAccentColor.current.copy(alpha = 0.15f)
@Composable fun accentWash()       = LocalAccentColor.current.copy(alpha = 0.07f)
@Composable fun accentChipBg()     = LocalAccentColor.current.copy(alpha = 0.12f)
@Composable fun accentChipBorder() = LocalAccentColor.current.copy(alpha = 0.40f)
@Composable fun accentAlbumGroup() = LocalAccentColor.current.copy(alpha = 0.05f)

/**
 * Adds a spring press-scale effect that coexists with [Modifier.clickable].
 * Uses non-consuming pointer input so clicks still register on the wrapped element.
 */
@Composable
fun Modifier.pressScale(targetScale: Float = 0.94f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}
