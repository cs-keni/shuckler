package com.shuckler.app.player

import androidx.compose.runtime.compositionLocalOf

val LocalMusicServiceConnection = compositionLocalOf<MusicServiceConnection> {
    error("No MusicServiceConnection provided")
}
