package com.shuckler.app.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Spotify Web API client for playlist import.
 * Uses OAuth 2.0 Authorization Code with PKCE (no client secret needed).
 */
object SpotifyRepository {

    private const val REDIRECT_URI = "shuckler://spotify-callback"
    private const val AUTH_URL = "https://accounts.spotify.com/authorize"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val API_BASE = "https://api.spotify.com/v1"
    private const val SCOPES = "playlist-read-private playlist-read-collaborative"
    private const val TAG = "SpotifyRepository"

    private val httpClient = OkHttpClient()

    data class SpotifyPlaylist(
        val id: String,
        val name: String,
        val description: String?,
        val trackCount: Int,
        val imageUrl: String?
    )

    data class SpotifyTrack(
        val title: String,
        val artist: String,
        val album: String?
    )

    /**
     * Build the authorization URL for OAuth. Store [codeVerifier] for later token exchange.
     */
    fun buildAuthUrl(clientId: String, codeVerifier: String): String {
        val challenge = createCodeChallenge(codeVerifier)
        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    fun createCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun createCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Extract authorization code from redirect URI.
     */
    fun extractCodeFromRedirect(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme == "shuckler" && uri.host == "spotify-callback") {
            return uri.getQueryParameter("code")
        }
        return null
    }

    /**
     * Exchange authorization code for access token.
     */
    suspend fun exchangeCodeForToken(clientId: String, code: String, codeVerifier: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("client_id", clientId)
                    .add("code_verifier", codeVerifier)
                    .build()
                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Token exchange failed: ${response.code} ${response.body?.string()}")
                    return@withContext null
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                json.optString("access_token").takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                Log.e(TAG, "Token exchange error", e)
                null
            }
        }

    /**
     * Fetch user's playlists.
     */
    suspend fun getPlaylists(accessToken: String): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        val all = mutableListOf<SpotifyPlaylist>()
        var url = "$API_BASE/me/playlists?limit=50"
        while (url.isNotBlank()) {
            val list = fetchPlaylistsPage(accessToken, url) ?: break
            all.addAll(list.first)
            url = list.second
        }
        all
    }

    private fun fetchPlaylistsPage(token: String, url: String): Pair<List<SpotifyPlaylist>, String>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string() ?: "{}")
            val items = json.optJSONArray("items") ?: JSONArray()
            val playlists = (0 until items.length()).mapNotNull { i ->
                val obj = items.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = obj.optString("name", "Untitled")
                val desc = obj.optString("description", "").takeIf { it.isNotBlank() }
                val tracks = obj.optJSONObject("tracks")
                val count = tracks?.optInt("total", 0) ?: 0
                val images = obj.optJSONArray("images")
                val imageUrl = (0 until (images?.length() ?: 0))
                    .mapNotNull { j -> images?.optJSONObject(j)?.optString("url")?.takeIf { it.isNotBlank() } }
                    .firstOrNull()
                SpotifyPlaylist(id = id, name = name, description = desc, trackCount = count, imageUrl = imageUrl)
            }
            val next = json.optString("next", "").takeIf { it.isNotBlank() } ?: ""
            Pair(playlists, next)
        } catch (e: Exception) {
            Log.e(TAG, "Fetch playlists error", e)
            null
        }
    }

    /**
     * Fetch tracks from a playlist.
     */
    suspend fun getPlaylistTracks(accessToken: String, playlistId: String): List<SpotifyTrack> =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<SpotifyTrack>()
            var url = "$API_BASE/playlists/$playlistId/tracks?limit=100"
            while (url.isNotBlank()) {
                val list = fetchTracksPage(accessToken, url) ?: break
                all.addAll(list.first)
                url = list.second
            }
            all
        }

    private fun fetchTracksPage(token: String, url: String): Pair<List<SpotifyTrack>, String>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string() ?: "{}")
            val items = json.optJSONArray("items") ?: JSONArray()
            val tracks = (0 until items.length()).mapNotNull { i ->
                val obj = items.optJSONObject(i) ?: return@mapNotNull null
                val track = obj.optJSONObject("track") ?: return@mapNotNull null
                if (track.isNull("id")) return@mapNotNull null // Local file or unavailable
                val title = track.optString("name", "Unknown")
                val artists = track.optJSONArray("artists")
                val artist = (0 until (artists?.length() ?: 0))
                    .mapNotNull { j -> artists?.optJSONObject(j)?.optString("name") }
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() } ?: "Unknown"
                val album = track.optJSONObject("album")?.optString("name")?.takeIf { it.isNotBlank() }
                SpotifyTrack(title = title, artist = artist, album = album)
            }
            val next = json.optString("next", "").takeIf { it.isNotBlank() } ?: ""
            Pair(tracks, next)
        } catch (e: Exception) {
            Log.e(TAG, "Fetch tracks error", e)
            null
        }
    }
}
