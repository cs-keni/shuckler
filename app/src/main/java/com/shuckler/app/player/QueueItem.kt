package com.shuckler.app.player

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single item in the play queue (URI + metadata for display and auto-delete).
 */
data class QueueItem(
    val uri: String,
    val title: String,
    val artist: String,
    val trackId: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_URI, uri)
        put(KEY_TITLE, title)
        put(KEY_ARTIST, artist)
        put(KEY_TRACK_ID, trackId ?: JSONObject.NULL)
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_TRACK_ID = "trackId"

        fun fromJson(obj: JSONObject): QueueItem = QueueItem(
            uri = obj.getString(KEY_URI),
            title = obj.optString(KEY_TITLE, ""),
            artist = obj.optString(KEY_ARTIST, ""),
            trackId = obj.optString(KEY_TRACK_ID, "").takeIf { it.isNotBlank() }
        )

        fun listToJson(list: List<QueueItem>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJson(json: String): List<QueueItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { i -> fromJson(arr.getJSONObject(i)) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
