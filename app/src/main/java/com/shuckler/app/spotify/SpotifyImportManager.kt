package com.shuckler.app.spotify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shuckler.app.MainActivity
import com.shuckler.app.download.DownloadManager
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.playlist.PlaylistManager
import com.shuckler.app.youtube.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class ImportState { SEARCHING, NOT_FOUND, QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class ImportTrackRecord(
    val importId: String,
    val playlistKey: String,
    val playlistName: String,
    val trackTitle: String,
    val trackArtist: String,
    val trackAlbum: String?,
    val trackAlbumYear: Int?,
    var state: ImportState = ImportState.SEARCHING,
    var downloadId: String? = null,
    var shucklerPlaylistId: String? = null
)

data class ImportProgress(
    val importId: String,
    val total: Int,
    val completed: Int,
    val failed: Int,
    val notFound: Int,
    val isFinished: Boolean
) {
    val matched: Int get() = completed
    val terminal: Int get() = completed + failed + notFound
}

class SpotifyImportManager(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val playlistManager: PlaylistManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _progress = MutableStateFlow<ImportProgress?>(null)
    val progress: StateFlow<ImportProgress?> = _progress.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val importTables = mutableMapOf<String, MutableList<ImportTrackRecord>>()
    private var activeImportId: String? = null
    private var collectJob: Job? = null

    fun getImportRecords(importId: String): List<ImportTrackRecord> =
        importTables[importId]?.toList() ?: emptyList()

    fun getNotFoundTracks(importId: String): List<ImportTrackRecord> =
        importTables[importId]?.filter { it.state == ImportState.NOT_FOUND } ?: emptyList()

    fun startImport(
        selectedItems: List<SelectedImportItem>,
        onWifiOnlyBlocked: () -> Unit = {}
    ) {
        val importId = UUID.randomUUID().toString()
        activeImportId = importId

        val records = mutableListOf<ImportTrackRecord>()
        for (item in selectedItems) {
            val shucklerPlaylist = playlistManager.createPlaylist(item.displayName, item.description)
            for (track in item.tracks) {
                records.add(
                    ImportTrackRecord(
                        importId = importId,
                        playlistKey = item.key,
                        playlistName = item.displayName,
                        trackTitle = track.title,
                        trackArtist = track.artist,
                        trackAlbum = track.album,
                        trackAlbumYear = track.albumYear,
                        shucklerPlaylistId = shucklerPlaylist.id
                    )
                )
            }
        }

        importTables[importId] = records
        persistImportTable(importId, records)
        _isImporting.value = true
        emitProgress(importId)

        // Start the download state observer BEFORE firing any download jobs
        collectJob?.cancel()
        collectJob = scope.launch {
            downloadManager.downloads.collect { downloads ->
                val table = importTables[importId] ?: return@collect
                var changed = false
                for (record in table) {
                    val dlId = record.downloadId ?: continue
                    val dl = downloads.find { it.id == dlId } ?: continue
                    val newState = when (dl.status) {
                        DownloadStatus.DOWNLOADING -> ImportState.DOWNLOADING
                        DownloadStatus.COMPLETED -> ImportState.COMPLETED
                        DownloadStatus.FAILED -> ImportState.FAILED
                        else -> continue
                    }
                    if (record.state != newState) {
                        record.state = newState
                        if (newState == ImportState.COMPLETED) {
                            record.shucklerPlaylistId?.let { pid ->
                                playlistManager.addTrackToPlaylist(pid, dlId)
                            }
                        }
                        changed = true
                    }
                }
                if (changed) {
                    persistImportTable(importId, table)
                    emitProgress(importId)
                    checkCompletion(importId)
                }
            }
        }

        // Scan existing completed downloads to close the startup race window
        val existingCompleted = downloadManager.downloads.value
            .filter { it.status == DownloadStatus.COMPLETED }
            .map { it.id }
            .toSet()
        for (record in records) {
            val dlId = record.downloadId ?: continue
            if (dlId in existingCompleted && record.state != ImportState.COMPLETED) {
                record.state = ImportState.COMPLETED
                record.shucklerPlaylistId?.let { pid ->
                    playlistManager.addTrackToPlaylist(pid, dlId)
                }
            }
        }

        // Fire download jobs with 100ms spacing between launches to throttle YouTube search
        scope.launch {
            for (record in records) {
                delay(100)
                launch { searchAndDownload(record, onWifiOnlyBlocked) }
            }
        }
    }

    private suspend fun searchAndDownload(record: ImportTrackRecord, onWifiOnlyBlocked: () -> Unit) {
        val query = "${record.trackTitle} ${record.trackArtist}"
        val results = try {
            withContext(Dispatchers.IO) {
                retryWithBackoff { YouTubeRepository.search(query) }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val best = results.firstOrNull()
        if (best == null) {
            record.state = ImportState.NOT_FOUND
            val importId = record.importId
            persistImportTable(importId, importTables[importId] ?: return)
            emitProgress(importId)
            checkCompletion(importId)
            return
        }

        record.state = ImportState.QUEUED
        emitProgress(record.importId)

        val downloadId = downloadManager.startDownloadFromYouTube(
            videoUrl = best.url,
            title = record.trackTitle,
            artist = record.trackArtist,
            thumbnailUrl = best.thumbnailUrl,
            onWifiOnlyBlocked = onWifiOnlyBlocked,
            albumTitle = record.trackAlbum,
            albumYear = record.trackAlbumYear
        )

        if (downloadId.isBlank()) {
            record.state = ImportState.FAILED
        } else {
            record.downloadId = downloadId
            // State transitions from QUEUED → DOWNLOADING → COMPLETED/FAILED are
            // handled by the downloads collector set up before firing jobs
        }

        val importId = record.importId
        persistImportTable(importId, importTables[importId] ?: return)
        emitProgress(importId)
    }

    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var delayMs = 1000L
        var lastException: Exception? = null
        repeat(5) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < 4) delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(60_000L)
            }
        }
        throw lastException ?: Exception("retryWithBackoff failed")
    }

    private fun checkCompletion(importId: String) {
        val records = importTables[importId] ?: return
        val terminal = setOf(ImportState.COMPLETED, ImportState.NOT_FOUND, ImportState.FAILED)
        if (records.all { it.state in terminal } && _isImporting.value) {
            _isImporting.value = false
            collectJob?.cancel()
            postCompletionNotification(importId, records)
        }
    }

    private fun emitProgress(importId: String) {
        val records = importTables[importId] ?: return
        val terminal = setOf(ImportState.COMPLETED, ImportState.NOT_FOUND, ImportState.FAILED)
        _progress.value = ImportProgress(
            importId = importId,
            total = records.size,
            completed = records.count { it.state == ImportState.COMPLETED },
            failed = records.count { it.state == ImportState.FAILED },
            notFound = records.count { it.state == ImportState.NOT_FOUND },
            isFinished = records.all { it.state in terminal }
        )
    }

    private fun postCompletionNotification(importId: String, records: List<ImportTrackRecord>) {
        val matched = records.count { it.state == ImportState.COMPLETED }
        val notFound = records.count { it.state == ImportState.NOT_FOUND }
        val title = "$matched ${if (matched == 1) "song" else "songs"} rescued"
        val body = if (notFound > 0) "$notFound couldn't be found — tap to review" else "Your library is ready"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, SCREEN_IMPORT_COMPLETE)
            putExtra(EXTRA_IMPORT_ID, importId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE_COMPLETE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_IMPORT_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_IMPORT_COMPLETE, notification)
        } catch (_: SecurityException) {}
    }

    fun cancelImport(importId: String) {
        if (activeImportId != importId) return
        collectJob?.cancel()
        _isImporting.value = false
        val terminal = setOf(ImportState.COMPLETED, ImportState.NOT_FOUND, ImportState.FAILED)
        importTables[importId]?.forEach { record ->
            if (record.state !in terminal) record.state = ImportState.FAILED
        }
        emitProgress(importId)
    }

    fun resolveNotFoundTrack(
        importId: String,
        record: ImportTrackRecord,
        youtubeUrl: String,
        onWifiOnlyBlocked: () -> Unit = {}
    ) {
        scope.launch {
            val downloadId = downloadManager.startDownloadFromYouTube(
                videoUrl = youtubeUrl,
                title = record.trackTitle,
                artist = record.trackArtist,
                thumbnailUrl = null,
                onWifiOnlyBlocked = onWifiOnlyBlocked,
                albumTitle = record.trackAlbum,
                albumYear = record.trackAlbumYear
            )
            if (downloadId.isNotBlank()) {
                record.downloadId = downloadId
                record.state = ImportState.QUEUED
                // The active collector will pick up state transitions from here
            }
            emitProgress(importId)
        }
    }

    private fun persistImportTable(importId: String, records: List<ImportTrackRecord>) {
        try {
            val arr = JSONArray()
            for (r in records) {
                arr.put(JSONObject().apply {
                    put("importId", r.importId)
                    put("playlistKey", r.playlistKey)
                    put("playlistName", r.playlistName)
                    put("trackTitle", r.trackTitle)
                    put("trackArtist", r.trackArtist)
                    r.trackAlbum?.let { put("trackAlbum", it) }
                    r.trackAlbumYear?.let { put("trackAlbumYear", it) }
                    put("state", r.state.name)
                    r.downloadId?.let { put("downloadId", it) }
                    r.shucklerPlaylistId?.let { put("shucklerPlaylistId", it) }
                })
            }
            importTableFile(importId).writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist import table", e)
        }
    }

    fun loadImportTable(importId: String): List<ImportTrackRecord>? {
        val file = importTableFile(importId)
        if (!file.exists()) return null
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ImportTrackRecord(
                    importId = o.getString("importId"),
                    playlistKey = o.getString("playlistKey"),
                    playlistName = o.getString("playlistName"),
                    trackTitle = o.getString("trackTitle"),
                    trackArtist = o.getString("trackArtist"),
                    trackAlbum = o.optString("trackAlbum").takeIf { it.isNotBlank() },
                    trackAlbumYear = o.optInt("trackAlbumYear", -1).takeIf { it >= 0 },
                    state = ImportState.valueOf(o.getString("state")),
                    downloadId = o.optString("downloadId").takeIf { it.isNotBlank() },
                    shucklerPlaylistId = o.optString("shucklerPlaylistId").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load import table", e)
            null
        }
    }

    private fun importTableFile(importId: String): File =
        File(context.filesDir, "import_$importId.json")

    companion object {
        private const val TAG = "SpotifyImportManager"
        const val EXTRA_SCREEN = "extra_screen"
        const val EXTRA_IMPORT_ID = "extra_import_id"
        const val SCREEN_IMPORT_COMPLETE = "import_complete"
        const val SCREEN_MISMATCH_REVIEW = "mismatch_review"
        const val CHANNEL_IMPORT_COMPLETE = "spotify_import_complete"
        const val CHANNEL_IMPORT_PROGRESS = "spotify_import_progress"
        const val NOTIF_IMPORT_COMPLETE = 2001
        const val NOTIF_IMPORT_PROGRESS = 2002
        private const val REQUEST_CODE_COMPLETE = 2001
    }
}

data class SelectedImportItem(
    val key: String,
    val displayName: String,
    val description: String?,
    val tracks: List<SpotifyRepository.SpotifyTrack>
)
