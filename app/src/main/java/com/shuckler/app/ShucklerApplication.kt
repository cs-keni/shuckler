package com.shuckler.app

import android.app.Application
import com.shuckler.app.download.DownloadManager

class ShucklerApplication : Application() {

    val downloadManager: DownloadManager by lazy {
        DownloadManager(applicationContext)
    }
}
