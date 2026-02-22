package com.shuckler.app.download

/**
 * Status of a download.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

/**
 * Represents a downloaded or in-progress track.
 */
data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val filePath: String,
    val sourceUrl: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val downloadDateMs: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedMs: Long = 0L,
    val isFavorite: Boolean = false,
    val thumbnailUrl: String? = null,
    val status: DownloadStatus = DownloadStatus.COMPLETED,
    val downloadProgress: Int = 100,
    val errorMessage: String? = null,
    /** When set, this is a virtual/chapter track: play only [startMs..endMs] of the file. */
    val startMs: Long? = null,
    val endMs: Long? = null
) {
    val isChapterTrack: Boolean get() = startMs != null && endMs != null
}

/**
 * Progress of an active download.
 */
data class DownloadProgress(
    val id: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int,
    val bytesPerSecond: Long = 0L
)
