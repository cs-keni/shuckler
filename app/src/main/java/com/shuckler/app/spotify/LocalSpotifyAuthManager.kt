package com.shuckler.app.spotify

import androidx.compose.runtime.compositionLocalOf

val LocalSpotifyAuthManager = compositionLocalOf<SpotifyAuthManager> {
    error("No SpotifyAuthManager provided")
}
