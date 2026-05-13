package com.shuckler.app.lastfm

import androidx.compose.runtime.compositionLocalOf

val LocalLastFmScrobbler = compositionLocalOf<LastFmScrobbler> {
    error("No LastFmScrobbler provided")
}
