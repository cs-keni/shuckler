package com.shuckler.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ShucklerDarkColorScheme = darkColorScheme(
    primary = ShucklerNeonYellow,
    onPrimary = ShucklerBlack,
    primaryContainer = ShucklerSurfaceDark,
    onPrimaryContainer = ShucklerOnSurface,
    secondary = ShucklerYellowOnBlack,
    onSecondary = ShucklerBlack,
    background = ShucklerBlack,
    onBackground = ShucklerOnSurface,
    surface = ShucklerBlack,
    onSurface = ShucklerOnSurface,
    surfaceVariant = ShucklerBlackElevated,
    onSurfaceVariant = ShucklerOnSurfaceVariant,
    outline = ShucklerOnSurfaceVariant.copy(alpha = 0.5f),
    surfaceContainerHigh = ShucklerBlackElevated
)

@Composable
fun ShucklerTheme(
    content: @Composable () -> Unit
) {
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    MaterialTheme(
        colorScheme = ShucklerDarkColorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
