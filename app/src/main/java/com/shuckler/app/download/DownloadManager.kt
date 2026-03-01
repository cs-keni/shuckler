package com.shuckler.app.download

import android.content.Context
import com.shuckler.app.ShucklerApplication
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.shuckler.app.youtube.YouTubeRepository
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles downloading audio from URLs and persisting track metadata.
 * Saves files to app-specific external storage (Music directory).
 */
class DownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _downloads = MutableStateFlow<List<DownloadedTrack>>(emptyList())
    val downloads: StateFlow<List<DownloadedTrack>> = _downloads.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val _lastDownloadError = MutableStateFlow<String?>(null)
    val lastDownloadError: StateFlow<String?> = _lastDownloadError.asStateFlow()

    private val _lastFailedDownloadId = MutableStateFlow<String?>(null)
    val lastFailedDownloadId: StateFlow<String?> = _lastFailedDownloadId.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    /** Total bytes used by completed downloads (from metadata). Recomputed when needed. */
    fun getTotalStorageUsed(): Long = _downloads.value
        .filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
        .sumOf { it.fileSizeBytes.takeIf { size -> size > 0 } ?: run { kotlin.runCatching { File(it.filePath).length() }.getOrNull() ?: 0L } }

    /** Approximate free space on the volume where we store audio (in bytes). */
    fun getAvailableSpace(): Long = kotlin.runCatching {
        audioDir.usableSpace
    }.getOrNull() ?: 0L

    private val audioDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir.resolve("music")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val metadataFile: File
        get() = context.filesDir.resolve(METADATA_FILENAME)

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autoDeleteAfterPlayback: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DELETE_AFTER_PLAYBACK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_DELETE_AFTER_PLAYBACK, value).apply()
        }

    /** Crossfade duration in ms when advancing to next track (0 = off). */
    var crossfadeDurationMs: Int
        get() = prefs.getInt(KEY_CROSSFADE_DURATION_MS, 0).coerceIn(0, 15000)
        set(value) {
            prefs.edit().putInt(KEY_CROSSFADE_DURATION_MS, value.coerceIn(0, 15000)).apply()
        }

    /** Download quality for YouTube: "best", "high", or "data_saver". Affects stream choice (bitrate) and format preference (M4A preferred). */
    var downloadQuality: String
        get() = prefs.getString(KEY_DOWNLOAD_QUALITY, "best") ?: "best"
        set(value) {
            val v = when (value) {
                "high", "data_saver" -> value
                else -> "best"
            }
            prefs.edit().putString(KEY_DOWNLOAD_QUALITY, v).apply()
        }

    /** Playback speed (e.g. 1f = normal). Persisted so it survives app restart. */
    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1f).coerceIn(0.5f, 2f)
        set(value) {
            prefs.edit().putFloat(KEY_PLAYBACK_SPEED, value.coerceIn(0.5f, 2f)).apply()
        }

    /** When sleep timer fires (or at end of track), fade out over last 60 seconds instead of abrupt stop. */
    var sleepTimerFadeLastMinute: Boolean
        get() = prefs.getBoolean(KEY_SLEEP_TIMER_FADE_LAST_MINUTE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SLEEP_TIMER_FADE_LAST_MINUTE, value).apply()
        }

    /** Default tab on launch: "home", "search", "library", "analytics". */
    var defaultTab: String
        get() = prefs.getString(KEY_DEFAULT_TAB, "home") ?: "home"
        set(value) {
            prefs.edit().putString(KEY_DEFAULT_TAB, value).apply()
        }

    /** When true, downloads only start when connected to Wi-Fi. */
    var wifiOnlyDownloads: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY_DOWNLOADS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WIFI_ONLY_DOWNLOADS, value).apply()
        }

    /** Returns true if connected to Wi-Fi (or Ethernet). Used for wifiOnlyDownloads check. */
    fun isConnectedToWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    init {
        scope.launch {
            _downloads.value = withContext(Dispatchers.IO) { loadMetadata() }
        }
    }

    /**
     * Start downloading from [url]. Optional [title], [artist], and [thumbnailUrl] for display.
     * Use for direct MP3/audio URLs. For YouTube, prefer [startDownloadFromYouTube] (refetches URL on retry).
     * Returns the download id; progress and completion are exposed via [progress] and [downloads].
     */
    fun startDownload(url: String, title: String? = null, artist: String? = null, thumbnailUrl: String? = null): String {
        _lastDownloadError.value = null
        val id = UUID.randomUUID().toString()
        val safeTitle = title?.takeIf { it.isNotBlank() } ?: "Track ${id.take(8)}"
        val safeArtist = artist?.takeIf { it.isNotBlank() } ?: "Unknown"

        val job = scope.launch {
            runDownload(id, url, safeTitle, safeArtist, thumbnailUrl)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id) }

        return id
    }

    /**
     * Start downloading from YouTube video URL. Fetches a fresh stream URL on each retry attempt,
     * which helps avoid "unexpected end of stream" when YouTube's temporary URLs expire.
     * When wifiOnlyDownloads is on and not on Wi-Fi, invokes onWifiOnlyBlocked and returns empty string.
     */
    fun startDownloadFromYouTube(
        videoUrl: String,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        onWifiOnlyBlocked: (() -> Unit)? = null
    ): String {
        if (wifiOnlyDownloads && !isConnectedToWifi()) {
            onWifiOnlyBlocked?.invoke()
            return ""
        }
        _lastDownloadError.value = null
        val id = UUID.randomUUID().toString()
        val safeTitle = title?.takeIf { it.isNotBlank() } ?: "Track ${id.take(8)}"
        val safeArtist = artist?.takeIf { it.isNotBlank() } ?: "Unknown"

        val job = scope.launch {
            runDownloadFromYouTube(id, videoUrl, safeTitle, safeArtist, thumbnailUrl)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id) }

        return id
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
    }

    /**
     * Retry a failed download. Uses existing track id and metadata.
     * No-op if track is not FAILED or has no sourceUrl.
     * When wifiOnlyDownloads is on and not on Wi-Fi, invokes onWifiOnlyBlocked and returns.
     */
    fun retryDownload(id: String, onWifiOnlyBlocked: (() -> Unit)? = null) {
        if (wifiOnlyDownloads && !isConnectedToWifi()) {
            onWifiOnlyBlocked?.invoke()
            return
        }
        val track = _downloads.value.find { it.id == id } ?: return
        if (track.status != DownloadStatus.FAILED || track.sourceUrl.isBlank()) return
        _lastDownloadError.value = null
        _lastFailedDownloadId.value = null
        _lastFailedDownloadId.value = null
        val job = scope.launch {
            runDownloadFromYouTube(id, track.sourceUrl, track.title, track.artist, track.thumbnailUrl)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id) }
    }

    /**
     * Update last playback position for a track. Used for "Continue listening" / resume from position.
     * Persists metadata.
     */
    fun updateLastPosition(id: String, positionMs: Long) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val updated = track.copy(lastPositionMs = positionMs.coerceAtLeast(0L))
                _downloads.value = list.map { if (it.id == id) updated else it }
                saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
            }
        }
    }

    /**
     * Get last playback position for a track, or null if none or track not found.
     */
    fun getLastPosition(trackId: String): Long? =
        _downloads.value.find { it.id == trackId }?.lastPositionMs?.takeIf { it > 0 }

    /**
     * Set or clear shuffle exclusion for a track. When untilMs is null, removes exclusion.
     * When untilMs > 0, track is excluded from shuffle until that timestamp.
     */
    fun setExcludedFromShuffle(id: String, untilMs: Long?) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val updated = track.copy(excludedFromShuffleUntilMs = untilMs)
                _downloads.value = list.map { if (it.id == id) updated else it }
                saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
            }
        }
    }

    /**
     * Filter tracks for shuffle: exclude those with excludedFromShuffleUntilMs in the future.
     */
    fun filterForShuffle(tracks: List<DownloadedTrack>): List<DownloadedTrack> {
        val now = System.currentTimeMillis()
        return tracks.filter { t ->
            t.excludedFromShuffleUntilMs == null || t.excludedFromShuffleUntilMs!! <= now
        }
    }

    /**
     * Increment play count for a track (e.g. when played from library). Persists metadata.
     */
    fun incrementPlayCount(id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val now = System.currentTimeMillis()
                val updated = track.copy(playCount = track.playCount + 1, lastPlayedMs = now)
                _downloads.value = list.map { if (it.id == id) updated else it }
                saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
            }
        }
    }

    /**
     * Set or clear favorite status for a track. Persists metadata.
     */
    fun setFavorite(id: String, favorite: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val updated = track.copy(isFavorite = favorite)
                _downloads.value = list.map { if (it.id == id) updated else it }
                saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
            }
        }
    }

    /**
     * Called when playback of a track ends naturally. If auto-delete is enabled and the track
     * is not a favorite, deletes the track. No-op if trackId is null (e.g. not from library).
     */
    fun considerAutoDeleteAfterPlayback(trackId: String?) {
        if (trackId == null || !autoDeleteAfterPlayback) return
        scope.launch {
            withContext(Dispatchers.IO) {
                val track = _downloads.value.find { it.id == trackId } ?: return@withContext
                if (!track.isFavorite) deleteTrack(trackId)
            }
        }
    }

    private val MIN_DURATION_FOR_CHAPTERS_MS = 10 * 60 * 1000L // 10 minutes

    /** True if track can be split by chapters: YouTube source, long enough (or unknown duration), not already a chapter. */
    fun canSplitByChapters(track: DownloadedTrack): Boolean {
        if (track.isChapterTrack) return false
        if (track.filePath.isBlank() || track.sourceUrl.isBlank()) return false
        if (!track.sourceUrl.contains("youtube.com") && !track.sourceUrl.contains("youtu.be")) return false
        if (track.durationMs > 0 && track.durationMs < MIN_DURATION_FOR_CHAPTERS_MS) return false
        return true
    }

    /**
     * Split a long track into chapter tracks. Fetches chapters from YouTube, creates virtual tracks
     * (same file, startMs/endMs), removes the original. No-op if no chapters or track ineligible.
     * Returns the new chapter track IDs, or empty on failure.
     */
    fun splitTrackByChapters(trackId: String, onComplete: (List<String>) -> Unit = {}) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == trackId } ?: return@withContext emptyList<String>()
                if (!canSplitByChapters(track)) return@withContext emptyList<String>()
                val chapters = YouTubeRepository.getChapters(track.sourceUrl)
                if (chapters.size < 2) return@withContext emptyList<String>()
                val filePath = track.filePath
                val artist = track.artist
                val thumb = track.thumbnailUrl
                val newTracks = chapters.mapIndexed { idx, ch ->
                    DownloadedTrack(
                        id = UUID.randomUUID().toString(),
                        title = ch.title,
                        artist = artist,
                        filePath = filePath,
                        sourceUrl = track.sourceUrl,
                        durationMs = ch.endMs - ch.startMs,
                        fileSizeBytes = 0L, // Shared file
                        downloadDateMs = track.downloadDateMs,
                        thumbnailUrl = thumb,
                        startMs = ch.startMs,
                        endMs = ch.endMs
                    )
                }
                val withoutOriginal = list.filter { it.id != trackId }
                _downloads.value = withoutOriginal + newTracks
                saveMetadata(_downloads.value)
                newTracks.map { it.id }
            }
            onComplete(result)
        }
    }

    /**
     * Delete a single track: remove from metadata. Deletes file only if no other track references it
     * (chapter tracks share a file; don't delete until last reference is removed).
     * Also removes the track from all playlists.
     */
    fun deleteTrack(id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val remaining = list.filter { it.id != id }
                _downloads.value = remaining
                saveMetadata(remaining)
                if (track.filePath.isNotBlank()) {
                    val othersShareFile = remaining.any { it.filePath == track.filePath }
                    if (!othersShareFile) {
                        try { File(track.filePath).delete() } catch (_: Exception) { }
                    }
                }
            }
            (context.applicationContext as? ShucklerApplication)
                ?.playlistManager?.removeTrackFromAllPlaylists(id)
        }
    }

    /**
     * Delete all completed tracks (files and metadata). Does not cancel in-progress downloads.
     */
    fun clearAllDownloads() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                list.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
                    .forEach { try { File(it.filePath).delete() } catch (_: Exception) { } }
                _downloads.value = emptyList()
                saveMetadata(emptyList())
            }
        }
    }

    private suspend fun runDownloadFromYouTube(id: String, videoUrl: String, title: String, artist: String, thumbnailUrl: String? = null) {
        withContext(Dispatchers.IO) {
            val maxAttempts = 5
            var lastError: Exception? = null
            var partialFile: File? = null
            var resumeFromByte: Long = 0L
            for (attempt in 1..maxAttempts) {
                if (attempt > 1) delay(2000L) // 2 sec between retries to avoid hammering
                val streamResult = YouTubeRepository.getAudioStreamUrl(videoUrl, downloadQuality)
                val streamUrl = when (streamResult) {
                    is YouTubeRepository.AudioStreamResult.Success -> streamResult.info.url
                    is YouTubeRepository.AudioStreamResult.Failure -> {
                        lastError = Exception(streamResult.message)
                        Log.w(TAG, "runDownloadFromYouTube attempt $attempt: failed to get stream URL - ${streamResult.message}")
                        continue
                    }
                }
                val result = runDownloadAttempt(id, streamUrl, title, artist, thumbnailUrl, partialFile, resumeFromByte)
                when (result) {
                    is DownloadAttemptResult.Success -> return@withContext
                    is DownloadAttemptResult.Failure -> {
                        lastError = Exception(result.message)
                        partialFile = result.partialFile
                        resumeFromByte = result.bytesDownloaded
                        if (partialFile == null || resumeFromByte <= 0) {
                            partialFile = null
                            resumeFromByte = 0L
                        }
                        Log.w(TAG, "runDownloadFromYouTube attempt $attempt failed: ${result.message}, bytes so far: $resumeFromByte")
                    }
                }
            }
            // Clean up orphaned partial file on final failure
            partialFile?.let { try { it.delete() } catch (_: Exception) { } }
            clearProgress(id)
            failDownload(id, videoUrl, title, artist, lastError?.message ?: "Failed after $maxAttempts attempts")
        }
    }

    private sealed class DownloadAttemptResult {
        data object Success : DownloadAttemptResult()
        data class Failure(val message: String, val partialFile: File?, val bytesDownloaded: Long) : DownloadAttemptResult()
    }

    /**
     * Single download attempt. Supports resumable download via Range header when [resumeFromByte] > 0.
     * Returns Success, or Failure with partialFile/bytesDownloaded for resume on next retry.
     */
    private fun runDownloadAttempt(
        id: String,
        urlString: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        existingPartialFile: File? = null,
        resumeFromByte: Long = 0L
    ): DownloadAttemptResult {
        var connection: HttpURLConnection? = null
        var outputStream: FileOutputStream? = null
        var outFile: File? = null
        val isResume = resumeFromByte > 0L && existingPartialFile != null && existingPartialFile.exists()
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 300_000 // 5 min - YouTube can be slow
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (isResume) {
                connection.setRequestProperty("Range", "bytes=$resumeFromByte-")
            }
            connection.connect()

            val responseCode = connection.responseCode
            when {
                responseCode == 416 -> {
                    // Range not satisfiable - server might have changed; retry from scratch
                    Log.w(TAG, "runDownloadAttempt: 416 Range not satisfiable, will retry from scratch")
                    DownloadAttemptResult.Failure("Range not satisfiable", null, 0L)
                }
                responseCode in 200..299 -> { /* OK or 206 Partial Content */ }
                else -> {
                    Log.w(TAG, "runDownloadAttempt: HTTP $responseCode for $urlString")
                    return DownloadAttemptResult.Failure("HTTP $responseCode", if (isResume) existingPartialFile else null, if (isResume) resumeFromByte else 0L)
                }
            }

            val is206 = responseCode == 206
            if (isResume && !is206) {
                // Server ignored Range, sent full content - cannot append; retry from scratch
                Log.w(TAG, "runDownloadAttempt: requested Range but got $responseCode, retry from scratch")
                return DownloadAttemptResult.Failure("Server sent full content instead of partial", null, 0L)
            }

            outFile = if (isResume) existingPartialFile!! else {
                File(audioDir, suggestFileName(urlString, connection.contentType))
            }
            val file = outFile!!
            val contentRange = connection.getHeaderField("Content-Range")
            val totalSize = parseTotalFromContentRange(contentRange)
            val contentLength = connection.contentLengthLong.takeIf { it > 0 }
                ?: totalSize?.minus(resumeFromByte)?.takeIf { it > 0 }
            val totalExpected = if (totalSize != null) totalSize else (contentLength?.plus(resumeFromByte))

            if (!isResume && totalExpected != null && totalExpected > 0) {
                val available = getAvailableSpace()
                if (available < totalExpected) {
                    failDownload(id, urlString, title, artist, "Not enough space. Need ${totalExpected / (1024 * 1024)} MB, only ${available / (1024 * 1024)} MB free.")
                    return DownloadAttemptResult.Success
                }
            }

            file.parentFile?.mkdirs()
            outputStream = FileOutputStream(file, isResume)

            val inputStream = connection.inputStream
            val buffer = ByteArray(BUFFER_SIZE)
            var totalRead: Long = resumeFromByte
            var read: Int
            var lastProgressTime = System.currentTimeMillis()
            var lastProgressBytes = totalRead

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                totalRead += read
                val now = System.currentTimeMillis()
                val elapsedSec = (now - lastProgressTime) / 1000.0
                val bytesPerSecond = if (elapsedSec >= 0.5 && totalRead > lastProgressBytes) {
                    ((totalRead - lastProgressBytes) / elapsedSec).toLong()
                } else 0L
                if (elapsedSec >= 0.5) {
                    lastProgressTime = now
                    lastProgressBytes = totalRead
                }
                val percent = if (totalExpected != null && totalExpected > 0) {
                    ((totalRead * 100) / totalExpected).toInt().coerceIn(0, 99)
                } else null
                updateProgress(id, totalRead, totalExpected ?: totalRead, percent ?: 0, bytesPerSecond)
            }

            completeDownload(id, file.absolutePath, urlString, title, artist, file.length(), thumbnailUrl)
            outputStream.close()
            outputStream = null
            clearProgress(id)
            DownloadAttemptResult.Success
        } catch (e: Exception) {
            Log.w(TAG, "runDownloadAttempt failed: ${e.message}", e)
            val partial = outFile ?: existingPartialFile
            val bytes = partial?.takeIf { it.exists() }?.length() ?: 0L
            DownloadAttemptResult.Failure(e.message ?: "Unknown error", partial.takeIf { bytes > 0 }, bytes)
        } finally {
            outputStream?.closeQuietly()
            connection?.disconnect()
        }
    }

    /** Parse "bytes 100-499/5000" -> 5000L */
    private fun parseTotalFromContentRange(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        val slash = header.indexOf('/')
        if (slash < 0) return null
        return header.substring(slash + 1).trim().toLongOrNull()
    }

    private suspend fun runDownload(id: String, urlString: String, title: String, artist: String, thumbnailUrl: String? = null) {
        withContext(Dispatchers.IO) {
            when (val result = runDownloadAttempt(id, urlString, title, artist, thumbnailUrl, null, 0L)) {
                is DownloadAttemptResult.Success -> { }
                is DownloadAttemptResult.Failure -> {
                    clearProgress(id)
                    failDownload(id, urlString, title, artist, result.message)
                }
            }
        }
    }

    private fun suggestFileName(urlString: String, contentType: String?): String {
        val fromUrl = try {
            URL(urlString).path.substringAfterLast('/').takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
        if (!fromUrl.isNullOrBlank() && fromUrl.contains(".")) return sanitizeFileName(fromUrl)
        val ext = when {
            contentType?.contains("mpeg") == true -> "mp3"
            contentType?.contains("mp4") == true || contentType?.contains("m4a") == true || contentType?.contains("aac") == true -> "m4a"
            contentType?.contains("ogg") == true -> "ogg"
            contentType?.contains("webm") == true -> "webm"
            contentType?.contains("wav") == true -> "wav"
            else -> "mp3"
        }
        return "download_${System.currentTimeMillis()}.$ext"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(128)
    }

    private fun updateProgress(id: String, bytesDownloaded: Long, totalBytes: Long, percent: Int, bytesPerSecond: Long = 0L) {
        _progress.value = _progress.value + (id to DownloadProgress(id, bytesDownloaded, totalBytes, percent, bytesPerSecond))
    }

    private fun clearProgress(id: String) {
        _progress.value = _progress.value - id
    }

    private fun completeDownload(id: String, filePath: String, sourceUrl: String, title: String, artist: String, fileSizeBytes: Long = 0L, thumbnailUrl: String? = null) {
        Log.d(TAG, "completeDownload: $title -> $filePath")
        val track = DownloadedTrack(
            id = id,
            title = title,
            artist = artist,
            filePath = filePath,
            sourceUrl = sourceUrl,
            status = DownloadStatus.COMPLETED,
            downloadProgress = 100,
            fileSizeBytes = fileSizeBytes,
            downloadDateMs = System.currentTimeMillis(),
            thumbnailUrl = thumbnailUrl
        )
        _downloads.value = _downloads.value + track
        saveMetadata(_downloads.value)
    }

    private fun failDownload(id: String, sourceUrl: String, title: String, artist: String, errorMessage: String) {
        Log.e(TAG, "failDownload: $title - $errorMessage")
        _lastDownloadError.value = errorMessage
        _lastFailedDownloadId.value = id
        val track = DownloadedTrack(
            id = id,
            title = title,
            artist = artist,
            filePath = "",
            sourceUrl = sourceUrl,
            status = DownloadStatus.FAILED,
            downloadProgress = 0,
            errorMessage = errorMessage
        )
        _downloads.value = _downloads.value + track
        saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
    }

    private fun loadMetadata(): List<DownloadedTrack> {
        if (!metadataFile.exists()) return emptyList()
        return try {
            val json = metadataFile.readText()
            val arr = JSONArray(json)
                List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val path = obj.optString(KEY_FILE_PATH, "")
                val size = obj.optLong(KEY_FILE_SIZE, 0L)
                val startMs = obj.optLong(KEY_START_MS, -1L).takeIf { it >= 0 }
                val endMs = obj.optLong(KEY_END_MS, -1L).takeIf { it >= 0 }
                DownloadedTrack(
                    id = obj.optString(KEY_ID, ""),
                    title = obj.optString(KEY_TITLE, ""),
                    artist = obj.optString(KEY_ARTIST, ""),
                    filePath = path,
                    sourceUrl = obj.optString(KEY_SOURCE_URL, ""),
                    durationMs = obj.optLong(KEY_DURATION_MS, 0L),
                    fileSizeBytes = if (size > 0) size else runCatching { File(path).length() }.getOrNull() ?: 0L,
                    downloadDateMs = obj.optLong(KEY_DOWNLOAD_DATE_MS, 0L),
                    playCount = obj.optInt(KEY_PLAY_COUNT, 0),
                    lastPlayedMs = obj.optLong(KEY_LAST_PLAYED_MS, 0L),
                    isFavorite = obj.optBoolean(KEY_IS_FAVORITE, false),
                    thumbnailUrl = obj.optString(KEY_THUMBNAIL_URL, "").takeIf { it.isNotBlank() },
                    status = DownloadStatus.COMPLETED,
                    downloadProgress = 100,
                    startMs = startMs,
                    endMs = endMs,
                    lastPositionMs = obj.optLong(KEY_LAST_POSITION_MS, 0L),
                    excludedFromShuffleUntilMs = obj.optLong(KEY_EXCLUDED_FROM_SHUFFLE_MS, -1L).takeIf { it > 0 },
                    moodTags = parseMoodTags(obj.optJSONArray(KEY_MOOD_TAGS))
                )
            }.filter { it.filePath.isNotBlank() && File(it.filePath).exists() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveMetadata(tracks: List<DownloadedTrack>) {
        try {
            val arr = JSONArray()
            tracks.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }
                .forEach { track ->
                    arr.put(JSONObject().apply {
                        put(KEY_ID, track.id)
                        put(KEY_TITLE, track.title)
                        put(KEY_ARTIST, track.artist)
                        put(KEY_FILE_PATH, track.filePath)
                        put(KEY_SOURCE_URL, track.sourceUrl)
                        put(KEY_DURATION_MS, track.durationMs)
                        put(KEY_FILE_SIZE, track.fileSizeBytes)
                        put(KEY_DOWNLOAD_DATE_MS, track.downloadDateMs)
                        put(KEY_PLAY_COUNT, track.playCount)
                        put(KEY_LAST_PLAYED_MS, track.lastPlayedMs)
                        put(KEY_IS_FAVORITE, track.isFavorite)
                        put(KEY_THUMBNAIL_URL, track.thumbnailUrl ?: "")
                        track.startMs?.let { put(KEY_START_MS, it) }
                        track.endMs?.let { put(KEY_END_MS, it) }
                        if (track.lastPositionMs > 0) put(KEY_LAST_POSITION_MS, track.lastPositionMs)
                        track.excludedFromShuffleUntilMs?.let { put(KEY_EXCLUDED_FROM_SHUFFLE_MS, it) }
                        if (track.moodTags.isNotEmpty()) {
                            put(KEY_MOOD_TAGS, JSONArray(track.moodTags.toList()))
                        }
                    })
                }
            metadataFile.writeText(arr.toString())
        } catch (_: Exception) { }
    }

    private fun parseMoodTags(arr: org.json.JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return try {
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i, "").takeIf { it.isNotBlank() }
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Set mood tags for a track. Persists metadata.
     */
    fun setMoodTags(id: String, tags: Set<String>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                val updated = track.copy(moodTags = tags)
                _downloads.value = list.map { if (it.id == id) updated else it }
                saveMetadata(_downloads.value.filter { it.status == DownloadStatus.COMPLETED })
            }
        }
    }

    private fun FileOutputStream.closeQuietly() {
        try { close() } catch (_: Exception) { }
    }

    companion object {
        private const val TAG = "DownloadManager"
        private const val METADATA_FILENAME = "downloads.json"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0"
        private const val BUFFER_SIZE = 65536 // 64KB - larger buffer for faster reads, fewer round-trips
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_FILE_PATH = "filePath"
        private const val KEY_SOURCE_URL = "sourceUrl"
        private const val KEY_DURATION_MS = "durationMs"
        private const val KEY_FILE_SIZE = "fileSizeBytes"
        private const val KEY_DOWNLOAD_DATE_MS = "downloadDateMs"
        private const val KEY_PLAY_COUNT = "playCount"
        private const val KEY_LAST_PLAYED_MS = "lastPlayedMs"
        private const val KEY_IS_FAVORITE = "isFavorite"
        private const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        private const val KEY_START_MS = "startMs"
        private const val KEY_END_MS = "endMs"
        private const val KEY_LAST_POSITION_MS = "lastPositionMs"
        private const val KEY_EXCLUDED_FROM_SHUFFLE_MS = "excludedFromShuffleUntilMs"
        private const val KEY_MOOD_TAGS = "moodTags"
        private const val PREFS_NAME = "shuckler_settings"
        private const val KEY_AUTO_DELETE_AFTER_PLAYBACK = "auto_delete_after_playback"
        private const val KEY_CROSSFADE_DURATION_MS = "crossfade_duration_ms"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_SLEEP_TIMER_FADE_LAST_MINUTE = "sleep_timer_fade_last_minute"
        private const val KEY_DEFAULT_TAB = "default_tab"
        private const val KEY_WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"
    }
}
