package com.shuckler.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

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
        .flatMapLatest { service -> service?.currentTrackTitle ?: flowOf(DEFAULT_TRACK_TITLE) }

    val currentTrackArtist: Flow<String> = serviceConnection.service
        .flatMapLatest { service -> service?.currentTrackArtist ?: flowOf(DEFAULT_TRACK_ARTIST) }

    fun togglePlayPause() {
        val service = serviceConnection.service.value
        if (service != null) {
            applicationContext.startService(Intent(applicationContext, MusicPlayerService::class.java))
            service.togglePlayPause()
        } else {
            startPlaybackService()
        }
    }

    /**
     * Switch to and play a track from a file URI (e.g. downloaded track).
     * Uses service Intent so it works even before the service is bound.
     */
    fun playTrack(uri: Uri, title: String, artist: String) {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY_URI
                putExtra(MusicPlayerService.EXTRA_URI, uri.toString())
                putExtra(MusicPlayerService.EXTRA_TITLE, title)
                putExtra(MusicPlayerService.EXTRA_ARTIST, artist)
            }
        )
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
