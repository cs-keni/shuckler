package com.shuckler.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

/**
 * Callback to show "Downloads are Wi‑Fi only" snackbar when download is blocked.
 * Provided by NavGraph; used by SearchScreen, HomeScreen, PlaylistScreen.
 */
val LocalOnWifiOnlyBlocked = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Snackbar host state for showing error/retry snackbars from SearchScreen, HomeScreen, etc.
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }
