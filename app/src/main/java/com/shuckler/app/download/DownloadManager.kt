package com.shuckler.app.download

import android.content.Context
import android.os.Environment
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

    private val activeJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private val audioDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir.resolve("music")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val metadataFile: File
        get() = context.filesDir.resolve(METADATA_FILENAME)

    init {
        scope.launch {
            _downloads.value = withContext(Dispatchers.IO) { loadMetadata() }
        }
    }

    /**
     * Start downloading from [url]. Optional [title] and [artist] for display.
     * Returns the download id; progress and completion are exposed via [progress] and [downloads].
     */
    fun startDownload(url: String, title: String? = null, artist: String? = null): String {
        val id = UUID.randomUUID().toString()
        val safeTitle = title?.takeIf { it.isNotBlank() } ?: "Track ${id.take(8)}"
        val safeArtist = artist?.takeIf { it.isNotBlank() } ?: "Unknown"

        val job = scope.launch {
            runDownload(id, url, safeTitle, safeArtist)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id) }

        return id
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
    }

    private suspend fun runDownload(id: String, urlString: String, title: String, artist: String) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var outputStream: FileOutputStream? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    failDownload(id, urlString, title, artist, "HTTP $responseCode")
                    return@withContext
                }

                val contentLength = connection.contentLengthLong.takeIf { it > 0 }
                val fileName = suggestFileName(urlString, connection.contentType)
                val file = File(audioDir, fileName)
                file.parentFile?.mkdirs()
                outputStream = FileOutputStream(file)

                val inputStream = connection.inputStream
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalRead: Long = 0
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    totalRead += read
                    val percent = if (contentLength != null && contentLength > 0) {
                        ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    } else null
                    updateProgress(id, totalRead, contentLength ?: totalRead, percent ?: 0)
                }

                outputStream.close()
                outputStream = null
                completeDownload(id, file.absolutePath, urlString, title, artist)
            } catch (e: Exception) {
                failDownload(id, urlString, title, artist, e.message ?: "Unknown error")
            } finally {
                outputStream?.closeQuietly()
                connection?.disconnect()
                clearProgress(id)
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
            contentType?.contains("ogg") == true -> "ogg"
            contentType?.contains("wav") == true -> "wav"
            else -> "mp3"
        }
        return "download_${System.currentTimeMillis()}.$ext"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(128)
    }

    private fun updateProgress(id: String, bytesDownloaded: Long, totalBytes: Long, percent: Int) {
        _progress.value = _progress.value + (id to DownloadProgress(id, bytesDownloaded, totalBytes, percent))
    }

    private fun clearProgress(id: String) {
        _progress.value = _progress.value - id
    }

    private fun completeDownload(id: String, filePath: String, sourceUrl: String, title: String, artist: String) {
        val track = DownloadedTrack(
            id = id,
            title = title,
            artist = artist,
            filePath = filePath,
            sourceUrl = sourceUrl,
            status = DownloadStatus.COMPLETED,
            downloadProgress = 100
        )
        _downloads.value = _downloads.value + track
        saveMetadata(_downloads.value)
    }

    private fun failDownload(id: String, sourceUrl: String, title: String, artist: String, errorMessage: String) {
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
                DownloadedTrack(
                    id = obj.optString(KEY_ID, ""),
                    title = obj.optString(KEY_TITLE, ""),
                    artist = obj.optString(KEY_ARTIST, ""),
                    filePath = obj.optString(KEY_FILE_PATH, ""),
                    sourceUrl = obj.optString(KEY_SOURCE_URL, ""),
                    durationMs = obj.optLong(KEY_DURATION_MS, 0L),
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
                    })
                }
            metadataFile.writeText(arr.toString())
        } catch (_: Exception) { }
    }

    private fun FileOutputStream.closeQuietly() {
        try { close() } catch (_: Exception) { }
    }

    companion object {
        private const val METADATA_FILENAME = "downloads.json"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_FILE_PATH = "filePath"
        private const val KEY_SOURCE_URL = "sourceUrl"
        private const val KEY_DURATION_MS = "durationMs"
    }
}
