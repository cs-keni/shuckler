package com.shuckler.app.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * YouTube search and audio URL extraction using NewPipe Extractor 0.25.1 with OkHttp.
 * Call [ensureInitialized] once before use (e.g. on first search).
 */
object YouTubeRepository {

    @Volatile
    private var initialized = false

    private val httpClient = OkHttpClient()

    /**
     * Call once before using search or getAudioUrl. Thread-safe.
     */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                NewPipe.init(OkHttpDownloader(httpClient))
                initialized = true
            } catch (_: Throwable) {
                // Init failed (e.g. NewPipe already init with different Downloader)
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
     * Result of getting an audio stream URL: either success with [AudioStreamInfo] or failure with a message.
     */
    sealed class AudioStreamResult {
        data class Success(val info: AudioStreamInfo) : AudioStreamResult()
        data class Failure(val message: String) : AudioStreamResult()
    }

    /**
     * Get a direct audio stream URL for a YouTube video URL. Runs on IO.
     * Returns [AudioStreamResult.Success] or [AudioStreamResult.Failure] with an error message.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): AudioStreamResult = withContext(Dispatchers.IO) {
        if (videoUrl.isBlank()) return@withContext AudioStreamResult.Failure("No video URL")
        ensureInitialized()
        val normalizedUrl = normalizeYouTubeUrl(videoUrl)
        try {
            val extractor: StreamExtractor = youtube.getStreamExtractor(normalizedUrl)
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                val msg = "No audio streams found for this video."
                Log.w(TAG, "getAudioStreamUrl: $msg $normalizedUrl")
                return@withContext AudioStreamResult.Failure(msg)
            }
            // Prefer higher bitrate
            val best = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
            val streamUrl = best.content
            if (streamUrl.isBlank()) {
                val msg = "Stream URL was empty."
                Log.w(TAG, "getAudioStreamUrl: $msg")
                return@withContext AudioStreamResult.Failure(msg)
            }
            AudioStreamResult.Success(
                AudioStreamInfo(
                    url = streamUrl,
                    title = extractor.name,
                    uploaderName = extractor.uploaderName
                )
            )
        } catch (t: Throwable) {
            val msg = t.message?.take(200) ?: t.javaClass.simpleName
            Log.e(TAG, "getAudioStreamUrl failed for $normalizedUrl", t)
            AudioStreamResult.Failure(msg)
        }
    }

    private const val TAG = "YouTubeRepository"

    data class AudioStreamInfo(
        val url: String,
        val title: String,
        val uploaderName: String
    )
}
