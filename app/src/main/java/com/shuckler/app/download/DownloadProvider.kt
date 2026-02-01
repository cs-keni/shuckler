package com.shuckler.app.download

import androidx.compose.runtime.compositionLocalOf

val LocalDownloadManager = compositionLocalOf<DownloadManager> {
    error("No DownloadManager provided")
}
