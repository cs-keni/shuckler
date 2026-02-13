package com.shuckler.app.playlist

import android.content.Context
import android.graphics.Bitmap
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
import java.util.UUID

/**
 * Manages user-created playlists and their entries. Persists to JSON in filesDir.
 */
class PlaylistManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val playlistsFile: File
        get() = context.filesDir.resolve(PLAYLISTS_FILENAME)

    private val coversDir: File
        get() = context.filesDir.resolve(COVERS_DIR).also { if (!it.exists()) it.mkdirs() }

    private val _allEntries = MutableStateFlow<List<PlaylistEntry>>(emptyList())
    val allEntries: StateFlow<List<PlaylistEntry>> = _allEntries.asStateFlow()
    private val entriesFile: File get() = context.filesDir.resolve(ENTRIES_FILENAME)

    init {
        scope.launch {
            _playlists.value = withContext(Dispatchers.IO) { loadPlaylists() }
            _allEntries.value = withContext(Dispatchers.IO) { loadEntries() }
        }
    }

    fun getEntries(playlistId: String): List<PlaylistEntry> =
        _allEntries.value.filter { it.playlistId == playlistId }.sortedBy { it.position }

    private fun loadPlaylists(): List<Playlist> {
        if (!playlistsFile.exists()) return emptyList()
        return try {
            val json = playlistsFile.readText()
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                Playlist(
                    id = obj.getString(KEY_ID),
                    name = obj.getString(KEY_NAME),
                    description = obj.optString(KEY_DESCRIPTION, "").takeIf { it.isNotBlank() },
                    coverImagePath = obj.optString(KEY_COVER_PATH, "").takeIf { it.isNotBlank() }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadEntries(): List<PlaylistEntry> {
        if (!entriesFile.exists()) return emptyList()
        return try {
            val json = entriesFile.readText()
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                PlaylistEntry(
                    playlistId = obj.getString(KEY_PLAYLIST_ID),
                    trackId = obj.getString(KEY_TRACK_ID),
                    position = obj.getInt(KEY_POSITION)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(list: List<Playlist>) {
        try {
            val arr = JSONArray()
            list.forEach { p ->
                arr.put(JSONObject().apply {
                    put(KEY_ID, p.id)
                    put(KEY_NAME, p.name)
                    put(KEY_DESCRIPTION, p.description ?: "")
                    put(KEY_COVER_PATH, p.coverImagePath ?: "")
                })
            }
            playlistsFile.writeText(arr.toString())
        } catch (_: Exception) { }
    }

    private fun saveEntries(entries: List<PlaylistEntry>) {
        try {
            val arr = JSONArray()
            entries.sortedBy { it.position }.forEach { e ->
                arr.put(JSONObject().apply {
                    put(KEY_PLAYLIST_ID, e.playlistId)
                    put(KEY_TRACK_ID, e.trackId)
                    put(KEY_POSITION, e.position)
                })
            }
            entriesFile.writeText(arr.toString())
            _allEntries.value = loadEntries()
        } catch (_: Exception) { }
    }

    fun createPlaylist(name: String, description: String? = null, coverImagePath: String? = null): Playlist {
        val id = UUID.randomUUID().toString()
        val p = Playlist(id = id, name = name, description = description, coverImagePath = coverImagePath)
        val updated = _playlists.value + p
        _playlists.value = updated
        scope.launch {
            withContext(Dispatchers.IO) { savePlaylists(updated) }
        }
        return p
    }

    fun updatePlaylist(playlist: Playlist) {
        val updated = _playlists.value.map { if (it.id == playlist.id) playlist else it }
        _playlists.value = updated
        scope.launch {
            withContext(Dispatchers.IO) { savePlaylists(updated) }
        }
    }

    fun deletePlaylist(id: String) {
        val p = _playlists.value.find { it.id == id } ?: return
        p.coverImagePath?.let { path ->
            try { File(path).delete() } catch (_: Exception) { }
        }
        val updated = _playlists.value.filter { it.id != id }
        _playlists.value = updated
        val allEntries = loadEntries().filter { it.playlistId != id }
        scope.launch {
            withContext(Dispatchers.IO) {
                savePlaylists(updated)
                saveEntries(allEntries)
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val entries = loadEntries()
                if (entries.any { it.playlistId == playlistId && it.trackId == trackId }) return@withContext
                val maxPos = entries.filter { it.playlistId == playlistId }.maxOfOrNull { it.position } ?: -1
                saveEntries(entries + PlaylistEntry(playlistId, trackId, maxPos + 1))
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val entries = loadEntries()
                    .filterNot { it.playlistId == playlistId && it.trackId == trackId }
                val playlistEntries = entries.filter { it.playlistId == playlistId }
                val reordered = playlistEntries.sortedBy { it.position }.mapIndexed { i, e ->
                    PlaylistEntry(e.playlistId, e.trackId, i)
                }
                saveEntries(entries.filter { it.playlistId != playlistId } + reordered)
            }
        }
    }

    fun reorderTrack(playlistId: String, trackId: String, newPosition: Int) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val entries = loadEntries().filter { it.playlistId == playlistId }.sortedBy { it.position }
                val index = entries.indexOfFirst { it.trackId == trackId }
                if (index < 0) return@withContext
                val reordered = entries.toMutableList()
                val item = reordered.removeAt(index)
                reordered.add(newPosition.coerceIn(0, reordered.size), item)
                val updated = loadEntries().filter { it.playlistId != playlistId } +
                    reordered.mapIndexed { i, e -> PlaylistEntry(e.playlistId, e.trackId, i) }
                saveEntries(updated)
            }
        }
    }

    /**
     * Called when a track is deleted from the library. Removes it from all playlists.
     */
    fun removeTrackFromAllPlaylists(trackId: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                saveEntries(loadEntries().filter { it.trackId != trackId })
            }
        }
    }

    fun getCoverFilePath(playlistId: String): String = coversDir.resolve("$playlistId.jpg").absolutePath

    suspend fun saveCoverFromBitmap(playlistId: String, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(getCoverFilePath(playlistId))
            destFile.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCoverFromUri(playlistId: String, sourceUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(getCoverFilePath(playlistId))
            context.contentResolver.openInputStream(android.net.Uri.parse(sourceUri))?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val bitmap = android.graphics.BitmapFactory.decodeFile(destFile.absolutePath)
            if (bitmap != null) {
                val maxSize = 512
                val scale = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height).coerceAtMost(1f)
                } else 1f
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, w, h, true)
                if (scaled != bitmap) bitmap.recycle()
                destFile.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
                scaled.recycle()
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PLAYLISTS_FILENAME = "playlists.json"
        private const val ENTRIES_FILENAME = "playlist_entries.json"
        private const val COVERS_DIR = "playlist_covers"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COVER_PATH = "coverPath"
        private const val KEY_PLAYLIST_ID = "playlistId"
        private const val KEY_TRACK_ID = "trackId"
        private const val KEY_POSITION = "position"
    }
}
