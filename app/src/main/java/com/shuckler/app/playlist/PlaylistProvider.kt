package com.shuckler.app.playlist

import androidx.compose.runtime.compositionLocalOf

val LocalPlaylistManager = compositionLocalOf<PlaylistManager> {
    error("No PlaylistManager provided")
}
