package com.shuckler.app.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class PlayerViewModel(
    private val context: Context,
    private val serviceConnection: MusicServiceConnection
) : ViewModel() {

    val isPlaying: Flow<Boolean> = serviceConnection.service
        .flatMapLatest { service ->
            service?.isPlaying ?: flowOf(false)
        }

    val currentTrackTitle: Flow<String> = serviceConnection.service
        .map { service -> service?.currentTrackTitle ?: "How About a Song (Jubilife City)" }

    val currentTrackArtist: Flow<String> = serviceConnection.service
        .map { service -> service?.currentTrackArtist ?: "Pokémon X and Y (OST)" }

    fun togglePlayPause() {
        val service = serviceConnection.service.value
        if (service != null) {
            // Start service so it stays alive when activity unbinds
            context.startService(Intent(context, MusicPlayerService::class.java))
            service.togglePlayPause()
        } else {
            // Service not bound yet - start it; onStartCommand will handle play
            startPlaybackService()
        }
    }

    fun play() {
        val service = serviceConnection.service.value
        if (service != null) {
            context.startService(Intent(context, MusicPlayerService::class.java))
            service.play()
        } else {
            startPlaybackService()
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pause() {
        serviceConnection.service.value?.pause()
    }

    class Factory(
        private val context: Context,
        private val serviceConnection: MusicServiceConnection
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
                return PlayerViewModel(context, serviceConnection) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
