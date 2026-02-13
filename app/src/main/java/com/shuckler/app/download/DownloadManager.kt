package com.shuckler.app.download

import android.content.Context
import com.shuckler.app.ShucklerApplication
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

    init {
        scope.launch {
            _downloads.value = withContext(Dispatchers.IO) { loadMetadata() }
        }
    }

    /**
     * Start downloading from [url]. Optional [title], [artist], and [thumbnailUrl] for display.
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

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
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

    /**
     * Delete a single track: remove file from disk and from metadata.
     * Also removes the track from all playlists.
     * No-op if id not found or file already missing.
     */
    fun deleteTrack(id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = _downloads.value
                val track = list.find { it.id == id } ?: return@withContext
                if (track.filePath.isNotBlank()) {
                    try { File(track.filePath).delete() } catch (_: Exception) { }
                }
                _downloads.value = list.filter { it.id != id }
                saveMetadata(_downloads.value)
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

    private suspend fun runDownload(id: String, urlString: String, title: String, artist: String, thumbnailUrl: String? = null) {
        withContext(Dispatchers.IO) {
            val maxAttempts = 2
            var lastError: Exception? = null
            for (attempt in 1..maxAttempts) {
                var connection: HttpURLConnection? = null
                var outputStream: FileOutputStream? = null
                try {
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 120_000
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        failDownload(id, urlString, title, artist, "HTTP $responseCode")
                        return@withContext
                    }

                    val contentLength = connection.contentLengthLong.takeIf { it > 0 }
                    if (contentLength != null && contentLength > 0) {
                        val available = getAvailableSpace()
                        if (available < contentLength) {
                            failDownload(id, urlString, title, artist, "Not enough space. Need ${contentLength / (1024 * 1024)} MB, only ${available / (1024 * 1024)} MB free.")
                            return@withContext
                        }
                    }

                    val fileName = suggestFileName(urlString, connection.contentType)
                    val file = File(audioDir, fileName)
                    file.parentFile?.mkdirs()
                    outputStream = FileOutputStream(file)

                    val inputStream = connection.inputStream
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalRead: Long = 0
                    var read: Int
                    var lastProgressTime = System.currentTimeMillis()
                    var lastProgressBytes = 0L

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
                        val percent = if (contentLength != null && contentLength > 0) {
                            ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else null
                        updateProgress(id, totalRead, contentLength ?: totalRead, percent ?: 0, bytesPerSecond)
                    }

                    // Complete and persist before close() so we don't lose the track if close() throws
                    completeDownload(id, file.absolutePath, urlString, title, artist, file.length(), thumbnailUrl)
                    outputStream.close()
                    outputStream = null
                    clearProgress(id)
                    return@withContext
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < maxAttempts) {
                        Log.w(TAG, "Download attempt $attempt failed (${e.message}), retrying...")
                    }
                } finally {
                    outputStream?.closeQuietly()
                    connection?.disconnect()
                }
            }
            clearProgress(id)
            failDownload(id, urlString, title, artist, lastError?.message ?: "Unknown error")
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
                    downloadProgress = 100
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
                    })
                }
            metadataFile.writeText(arr.toString())
        } catch (_: Exception) { }
    }

    private fun FileOutputStream.closeQuietly() {
        try { close() } catch (_: Exception) { }
    }

    companion object {
        private const val TAG = "DownloadManager"
        private const val METADATA_FILENAME = "downloads.json"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0"
        private const val DEFAULT_BUFFER_SIZE = 8192
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
        private const val PREFS_NAME = "shuckler_settings"
        private const val KEY_AUTO_DELETE_AFTER_PLAYBACK = "auto_delete_after_playback"
        private const val KEY_CROSSFADE_DURATION_MS = "crossfade_duration_ms"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_SLEEP_TIMER_FADE_LAST_MINUTE = "sleep_timer_fade_last_minute"
    }
}
