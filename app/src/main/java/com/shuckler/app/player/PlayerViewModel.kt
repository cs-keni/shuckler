package com.shuckler.app.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val applicationContext: Context,
    private val serviceConnection: MusicServiceConnection
) : ViewModel() {

    val isPlaying: Flow<Boolean> = serviceConnection.service
        .flatMapLatest { service ->
            service?.isPlaying ?: flowOf(false)
        }

    val currentTrackTitle: Flow<String> = serviceConnection.service
        .map { service -> service?.currentTrackTitle ?: DEFAULT_TRACK_TITLE }

    val currentTrackArtist: Flow<String> = serviceConnection.service
        .map { service -> service?.currentTrackArtist ?: DEFAULT_TRACK_ARTIST }

    fun togglePlayPause() {
        val service = serviceConnection.service.value
        if (service != null) {
            applicationContext.startService(Intent(applicationContext, MusicPlayerService::class.java))
            service.togglePlayPause()
        } else {
            startPlaybackService()
        }
    }

    private fun startPlaybackService() {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY
            }
        )
    }

    class Factory(
        private val context: Context,
        private val serviceConnection: MusicServiceConnection
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
                return PlayerViewModel(context.applicationContext, serviceConnection) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val DEFAULT_TRACK_TITLE = DefaultTrackInfo.TITLE
        private const val DEFAULT_TRACK_ARTIST = DefaultTrackInfo.ARTIST
    }
}
