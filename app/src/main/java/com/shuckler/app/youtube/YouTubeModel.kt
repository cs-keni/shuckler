package com.shuckler.app.youtube

/**
 * A YouTube search result (video) for display and download.
 */
data class YouTubeSearchResult(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val uploaderName: String?
) {
    val durationFormatted: String
        get() {
            if (durationSeconds <= 0) return ""
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return if (m >= 60) {
                val h = m / 60
                "%d:%02d:%02d".format(h, m % 60, s)
            } else {
                "%d:%02d".format(m, s)
            }
        }
}
