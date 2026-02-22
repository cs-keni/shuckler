package com.shuckler.app.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fetches lyrics from LRCLIB (https://lrclib.net) - free, no API key.
 * Caches results by (artist, title) in app storage to avoid repeated requests.
 */
class LyricsRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File
        get() = File(context.filesDir, LYRICS_CACHE_DIR).apply { mkdirs() }

    /**
     * Fetch lyrics for the given track. Uses cache first; on cache miss, fetches from API.
     * Returns [LyricsResult.Synced], [LyricsResult.Plain], or [LyricsResult.NotFound].
     */
    suspend fun getLyrics(artist: String, title: String): LyricsResult = withContext(Dispatchers.IO) {
        if (artist.isBlank() && title.isBlank()) return@withContext LyricsResult.NotFound
        val cacheKey = cacheKeyFor(artist, title)
        val cached = readCache(cacheKey)
        if (cached != null) return@withContext cached

        val result = fetchFromApi(artist, title)
        if (result is LyricsResult.Synced || result is LyricsResult.Plain) {
            writeCache(cacheKey, result)
        }
        result
    }

    private fun fetchFromApi(artist: String, title: String): LyricsResult {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lrclib.net/api/search?track_name=$encodedTitle&artist_name=$encodedArtist"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Shuckler/1.0 (https://github.com/shuckler)")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return LyricsResult.NotFound
            val body = response.body?.string() ?: return LyricsResult.NotFound
            parseSearchResponse(body)
        } catch (_: Exception) {
            LyricsResult.NotFound
        }
    }

    private fun parseSearchResponse(json: String): LyricsResult {
        return try {
            val arr = JSONArray(json)
            if (arr.length() == 0) return LyricsResult.NotFound
            val first = arr.getJSONObject(0)
            val synced = first.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
            val plain = first.optString("plainLyrics", "").takeIf { it.isNotBlank() }
            when {
                synced != null -> LyricsResult.Synced(parseLrc(synced))
                plain != null -> LyricsResult.Plain(plain)
                else -> LyricsResult.NotFound
            }
        } catch (_: Exception) {
            LyricsResult.NotFound
        }
    }

    /**
     * Parse LRC format: [mm:ss.xx] or [mm:ss] lines.
     * Lines without timestamps are dropped (or we could attach to previous timestamp).
     */
    private fun parseLrc(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d+):(\d+)\.?(\d*)?\](.*)""")
        for (line in lrc.split("\n")) {
            val match = regex.find(line.trim()) ?: continue
            val (m, s, cs, text) = match.destructured
            val minutes = m.toIntOrNull() ?: 0
            val seconds = s.toIntOrNull() ?: 0
            val centiseconds = cs.take(2).padEnd(2, '0').toIntOrNull() ?: 0
            val timestampMs = (minutes * 60_000L) + (seconds * 1000L) + (centiseconds * 10L)
            lines.add(LyricLine(timestampMs = timestampMs, text = text.trim()))
        }
        return lines.sortedBy { it.timestampMs }
    }

    private fun cacheKeyFor(artist: String, title: String): String {
        val normalized = "${artist.lowercase().trim()}|${title.lowercase().trim()}"
        return normalized.hashCode().toString(16)
    }

    private fun readCache(key: String): LyricsResult? {
        return try {
            val file = File(cacheDir, "$key.json")
            if (!file.exists()) return null
            val json = JSONObject(file.readText())
            when (json.optString("type", "")) {
                "synced" -> {
                    val arr = json.getJSONArray("lines")
                    val lines = List(arr.length()) { i ->
                        val obj = arr.getJSONObject(i)
                        LyricLine(
                            timestampMs = obj.getLong("ts"),
                            text = obj.getString("text")
                        )
                    }
                    LyricsResult.Synced(lines)
                }
                "plain" -> LyricsResult.Plain(json.getString("text"))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(key: String, result: LyricsResult) {
        try {
            val json = when (result) {
                is LyricsResult.Synced -> JSONObject().apply {
                    put("type", "synced")
                    put("lines", JSONArray().apply {
                        result.lines.forEach { line ->
                            put(JSONObject().apply {
                                put("ts", line.timestampMs)
                                put("text", line.text)
                            })
                        }
                    })
                }
                is LyricsResult.Plain -> JSONObject().apply {
                    put("type", "plain")
                    put("text", result.text)
                }
                else -> return
            }
            val file = File(cacheDir, "$key.json")
            file.writeText(json.toString())
        } catch (_: Exception) { }
    }

    companion object {
        private const val LYRICS_CACHE_DIR = "lyrics_cache"
    }
}
