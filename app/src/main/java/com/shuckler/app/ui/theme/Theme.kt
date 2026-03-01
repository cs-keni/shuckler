package com.shuckler.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shuckler.app.accessibility.LocalAccessibilityPreferences

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

private val ShucklerHighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFEB3B),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFEB3B),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color.White.copy(alpha = 0.5f),
    surfaceContainerHigh = Color(0xFF262626)
)

@Composable
fun ShucklerTheme(
    content: @Composable () -> Unit
) {
    val accessibilityPrefs = LocalAccessibilityPreferences.current
    val highContrast by accessibilityPrefs.highContrastFlow.collectAsState(initial = accessibilityPrefs.highContrast)
    val colorScheme = if (highContrast) ShucklerHighContrastColorScheme else ShucklerDarkColorScheme
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
