package com.shuckler.app.spotify

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException
import java.security.MessageDigest

class SpotifyAuthManager(private val context: Context) {

    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val encryptedPrefs: SharedPreferences? = createEncryptedPrefs()

    private fun createEncryptedPrefs(): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENC_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: GeneralSecurityException) {
        // Key rotation (uninstall/reinstall wipes Keystore) — clear and re-auth
        Log.w(TAG, "Keystore rotation detected, clearing tokens", e)
        context.deleteSharedPreferences(ENC_PREFS_NAME)
        null
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences unavailable", e)
        null
    }

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    init {
        checkScopeVersion()
        val persisted = encryptedPrefs?.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = encryptedPrefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L
        if (persisted != null && System.currentTimeMillis() < expiresAt - TOKEN_REFRESH_MARGIN_MS) {
            _accessToken.value = persisted
        }
    }

    private fun checkScopeVersion() {
        val current = computeScopeHash()
        val stored = plainPrefs.getString(KEY_SCOPE_HASH, null)
        if (stored != null && stored != current) {
            Log.i(TAG, "Scope version changed — clearing tokens for re-auth")
            clearPersistedTokens()
        }
        plainPrefs.edit().putString(KEY_SCOPE_HASH, current).apply()
    }

    fun saveCodeVerifier(verifier: String) {
        plainPrefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    fun getCodeVerifier(): String? =
        plainPrefs.getString(KEY_CODE_VERIFIER, null)?.takeIf { it.isNotBlank() }

    fun clearCodeVerifier() {
        plainPrefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }

    fun clearToken() {
        _accessToken.value = null
        clearPersistedTokens()
    }

    fun getStoredRefreshToken(): String? =
        encryptedPrefs?.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getTokenExpiresAt(): Long =
        encryptedPrefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L

    fun isTokenExpiringSoon(): Boolean =
        System.currentTimeMillis() >= getTokenExpiresAt() - TOKEN_REFRESH_MARGIN_MS

    /**
     * Returns a valid access token, silently refreshing if expiring soon.
     * Returns null if no token is available or refresh fails (caller should trigger re-auth).
     */
    suspend fun getValidToken(clientId: String): String? {
        val token = encryptedPrefs?.getString(KEY_ACCESS_TOKEN, null) ?: return _accessToken.value
        if (!isTokenExpiringSoon()) return token

        val refreshToken = getStoredRefreshToken() ?: return null
        val response = SpotifyRepository.refreshAccessToken(clientId, refreshToken)
        return if (response != null) {
            persistTokenResponse(response)
            response.accessToken
        } else {
            // Refresh failed — clear tokens; caller triggers re-auth
            clearToken()
            null
        }
    }

    /**
     * Handle redirect intent from Spotify OAuth. Returns true if a token was obtained.
     */
    suspend fun handleCallback(intent: Intent?, clientId: String): Boolean {
        val code = SpotifyRepository.extractCodeFromRedirect(intent) ?: return false
        val verifier = getCodeVerifier() ?: return false
        clearCodeVerifier()
        val response = SpotifyRepository.exchangeCodeForToken(clientId, code, verifier)
        return if (response != null) {
            persistTokenResponse(response)
            true
        } else {
            false
        }
    }

    private fun persistTokenResponse(response: SpotifyRepository.TokenResponse) {
        val expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
        encryptedPrefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, response.accessToken)
            ?.also { ed -> response.refreshToken?.let { ed.putString(KEY_REFRESH_TOKEN, it) } }
            ?.putLong(KEY_EXPIRES_AT, expiresAt)
            ?.apply()
        _accessToken.value = response.accessToken
    }

    private fun clearPersistedTokens() {
        encryptedPrefs?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.remove(KEY_EXPIRES_AT)
            ?.apply()
    }

    private fun computeScopeHash(): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(SpotifyRepository.SCOPES.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    companion object {
        private const val TAG = "SpotifyAuthManager"
        private const val PREFS_NAME = "spotify_auth"
        private const val ENC_PREFS_NAME = "spotify_auth_enc"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_SCOPE_HASH = "scope_hash"
        private const val TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000L
    }
}
