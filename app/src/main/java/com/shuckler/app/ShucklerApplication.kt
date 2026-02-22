package com.shuckler.app

import android.app.Application
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.lyrics.LyricsRepository
import com.shuckler.app.playlist.PlaylistManager

class ShucklerApplication : Application() {

    val downloadManager: DownloadManager by lazy {
        DownloadManager(applicationContext)
    }

    val playlistManager: PlaylistManager by lazy {
        PlaylistManager(applicationContext)
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(applicationContext)
    }
}
