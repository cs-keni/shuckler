package com.shuckler.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shuckler.app.accessibility.LocalAccessibilityPreferences

val LocalAccentColor = compositionLocalOf { Amber }

private val ShucklerDarkColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Base,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = Text1,
    secondary = Amber,
    onSecondary = Base,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = Text1,
    background = Base,
    onBackground = Text1,
    surface = Surface,
    onSurface = Text1,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = Text2,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = Border,
    outlineVariant = BorderSubtle,
    error = Red,
    onError = Text1,
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
fun ShucklerTheme(content: @Composable () -> Unit) {
    val accessibilityPrefs = LocalAccessibilityPreferences.current
    val highContrast by accessibilityPrefs.highContrastFlow.collectAsState(initial = accessibilityPrefs.highContrast)
    val colorScheme = if (highContrast) ShucklerHighContrastColorScheme else ShucklerDarkColorScheme
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp)
    )
    CompositionLocalProvider(LocalAccentColor provides colorScheme.primary) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
