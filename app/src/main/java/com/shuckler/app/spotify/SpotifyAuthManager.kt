package com.shuckler.app.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Spotify OAuth state. Persists code verifier across browser redirect.
 */
class SpotifyAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun saveCodeVerifier(verifier: String) {
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    fun getCodeVerifier(): String? = prefs.getString(KEY_CODE_VERIFIER, null)?.takeIf { it.isNotBlank() }

    fun clearCodeVerifier() {
        prefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }

    fun setAccessToken(token: String?) {
        _accessToken.value = token
    }

    fun clearToken() {
        _accessToken.value = null
    }

    /**
     * Handle redirect intent. Returns true if it was a Spotify callback.
     */
    suspend fun handleCallback(intent: Intent?, clientId: String): Boolean {
        val code = SpotifyRepository.extractCodeFromRedirect(intent) ?: return false
        val verifier = getCodeVerifier() ?: return false
        clearCodeVerifier()
        val token = SpotifyRepository.exchangeCodeForToken(clientId, code, verifier)
        if (token != null) {
            setAccessToken(token)
            return true
        }
        return false
    }

    companion object {
        private const val PREFS_NAME = "spotify_auth"
        private const val KEY_CODE_VERIFIER = "code_verifier"
    }
}
