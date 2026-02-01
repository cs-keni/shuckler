package com.shuckler.app.youtube

import io.github.shalva97.initNewPipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * YouTube search and audio URL extraction using NewPipe Extractor (via NewValve).
 * Call [ensureInitialized] once before use (e.g. on first search).
 */
object YouTubeRepository {

    @Volatile
    private var initialized = false

    /**
     * Call once before using search or getAudioUrl. Thread-safe.
     */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                initNewPipe()
                initialized = true
            } catch (_: Throwable) {
                // NewValve init failed (e.g. missing OkHttp)
            }
        }
    }

    private val youtube: StreamingService
        get() = ServiceList.YouTube

    /**
     * Search YouTube. Runs on IO. Returns empty list on error.
     */
    suspend fun search(query: String): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        ensureInitialized()
        try {
            val extractor = youtube.getSearchExtractor(query.trim())
            extractor.fetchPage()
            val page = extractor.initialPage
            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val durationSec = try {
                        item.duration
                    } catch (_: Throwable) { 0L }
                    val thumbUrl = item.thumbnails.firstOrNull()?.url
                    YouTubeSearchResult(
                        id = item.url ?: "",
                        title = item.name ?: "",
                        url = item.url ?: "",
                        thumbnailUrl = thumbUrl,
                        durationSeconds = durationSec,
                        uploaderName = item.uploaderName
                    )
                }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Get a direct audio stream URL for a YouTube video URL. Runs on IO.
     * Returns null on error or if no audio stream.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): AudioStreamInfo? = withContext(Dispatchers.IO) {
        if (videoUrl.isBlank()) return@withContext null
        ensureInitialized()
        try {
            val extractor: StreamExtractor = youtube.getStreamExtractor(videoUrl)
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) return@withContext null
            // Prefer higher bitrate
            val best = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
            val url = best.content
            if (url.isNullOrBlank()) return@withContext null
            AudioStreamInfo(
                url = url,
                title = extractor.name,
                uploaderName = extractor.uploaderName
            )
        } catch (_: Throwable) {
            null
        }
    }

    data class AudioStreamInfo(
        val url: String,
        val title: String,
        val uploaderName: String
    )
}
