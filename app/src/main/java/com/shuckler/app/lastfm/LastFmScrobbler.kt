package com.shuckler.app.lastfm

import android.content.Context
import android.content.SharedPreferences
import com.shuckler.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest

class LastFmScrobbler(context: Context) {
    private val apiKey get() = BuildConfig.LAST_FM_API_KEY
    private val apiSecret get() = BuildConfig.LAST_FM_API_SECRET
    private val prefs: SharedPreferences = context.getSharedPreferences("lastfm", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    var sessionKey: String?
        get() = prefs.getString("session_key", null)
        set(value) { prefs.edit().putString("session_key", value).apply() }

    var username: String?
        get() = prefs.getString("username", null)
        set(value) { prefs.edit().putString("username", value).apply() }

    val isConfigured: Boolean get() = apiKey.isNotBlank() && apiSecret.isNotBlank()
    val isConnected: Boolean get() = isConfigured && !sessionKey.isNullOrBlank()

    fun getAuthUrl(): String =
        "https://www.last.fm/api/auth/?api_key=$apiKey&cb=shuckler://lastfm/callback"

    suspend fun exchangeToken(token: String): Boolean {
        if (!isConfigured) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val sig = md5("api_key${apiKey}methodauth.getSessiontoken${token}${apiSecret}")
                val body = FormBody.Builder()
                    .add("method", "auth.getSession")
                    .add("api_key", apiKey)
                    .add("token", token)
                    .add("api_sig", sig)
                    .add("format", "json")
                    .build()
                val resp = client.newCall(
                    Request.Builder().url("https://ws.audioscrobbler.com/2.0/").post(body).build()
                ).execute()
                val json = JSONObject(resp.body?.string() ?: return@runCatching false)
                val session = json.optJSONObject("session") ?: return@runCatching false
                val sk = session.optString("key").takeIf { it.isNotBlank() } ?: return@runCatching false
                sessionKey = sk
                username = session.optString("name").takeIf { it.isNotBlank() }
                true
            }.getOrDefault(false)
        }
    }

    fun disconnect() {
        prefs.edit().remove("session_key").remove("username").apply()
    }

    suspend fun updateNowPlaying(artist: String, track: String) {
        val sk = sessionKey?.takeIf { it.isNotBlank() } ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                post(sortedMapOf(
                    "api_key" to apiKey,
                    "artist" to artist,
                    "method" to "track.updateNowPlaying",
                    "sk" to sk,
                    "track" to track
                ))
            }
        }
    }

    suspend fun scrobble(artist: String, track: String, timestampSec: Long) {
        val sk = sessionKey?.takeIf { it.isNotBlank() } ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                post(sortedMapOf(
                    "api_key" to apiKey,
                    "artist" to artist,
                    "method" to "track.scrobble",
                    "sk" to sk,
                    "timestamp" to timestampSec.toString(),
                    "track" to track
                ))
            }
        }
    }

    private fun post(params: Map<String, String>) {
        val sig = signParams(params)
        val body = FormBody.Builder().apply {
            params.forEach { (k, v) -> add(k, v) }
            add("api_sig", sig)
            add("format", "json")
        }.build()
        client.newCall(
            Request.Builder().url("https://ws.audioscrobbler.com/2.0/").post(body).build()
        ).execute().close()
    }

    private fun signParams(params: Map<String, String>): String =
        md5(params.entries.joinToString("") { "${it.key}${it.value}" } + apiSecret)

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
