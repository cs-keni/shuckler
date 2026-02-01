package com.shuckler.app.youtube

import android.util.Log
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
     * Normalize YouTube URL to canonical form (https://www.youtube.com/watch?v=VIDEO_ID).
     * Helps avoid parsing issues when search returns alternate formats.
     */
    private fun normalizeYouTubeUrl(input: String): String {
        val trimmed = input.trim()
        // youtu.be/VIDEO_ID
        val shortMatch = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})").find(trimmed)
        if (shortMatch != null) {
            return "https://www.youtube.com/watch?v=${shortMatch.groupValues[1]}"
        }
        // watch?v=VIDEO_ID or /embed/VIDEO_ID or /v/VIDEO_ID
        val idMatch = Regex("(?:watch\\?v=|/embed/|/v/)([a-zA-Z0-9_-]{11})").find(trimmed)
        if (idMatch != null) {
            return "https://www.youtube.com/watch?v=${idMatch.groupValues[1]}"
        }
        return trimmed
    }

    /**
     * Get a direct audio stream URL for a YouTube video URL. Runs on IO.
     * Returns null on error or if no audio stream.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): AudioStreamInfo? = withContext(Dispatchers.IO) {
        if (videoUrl.isBlank()) return@withContext null
        ensureInitialized()
        val normalizedUrl = normalizeYouTubeUrl(videoUrl)
        try {
            val extractor: StreamExtractor = youtube.getStreamExtractor(normalizedUrl)
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                Log.w(TAG, "getAudioStreamUrl: no audio streams for $normalizedUrl")
                return@withContext null
            }
            // Prefer higher bitrate
            val best = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
            val streamUrl = best.content
            if (streamUrl.isNullOrBlank()) {
                Log.w(TAG, "getAudioStreamUrl: best stream has no content URL")
                return@withContext null
            }
            AudioStreamInfo(
                url = streamUrl,
                title = extractor.name,
                uploaderName = extractor.uploaderName
            )
        } catch (t: Throwable) {
            Log.e(TAG, "getAudioStreamUrl failed for $normalizedUrl", t)
            null
        }
    }

    private const val TAG = "YouTubeRepository"

    data class AudioStreamInfo(
        val url: String,
        val title: String,
        val uploaderName: String
    )
}
