package com.shuckler.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
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

    val repeatMode: Flow<Int> = serviceConnection.service
        .flatMapLatest { service -> service?.repeatMode ?: flowOf(Player.REPEAT_MODE_OFF) }

    val playbackPositionMs: Flow<Long> = serviceConnection.service
        .flatMapLatest { service -> service?.playbackPositionMs ?: flowOf(0L) }

    val durationMs: Flow<Long> = serviceConnection.service
        .flatMapLatest { service -> service?.durationMs ?: flowOf(0L) }

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
     * @param trackId Optional library track id; when set, used for auto-delete-after-playback if enabled.
     */
    fun playTrack(uri: Uri, title: String, artist: String, trackId: String? = null) {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY_URI
                putExtra(MusicPlayerService.EXTRA_URI, uri.toString())
                putExtra(MusicPlayerService.EXTRA_TITLE, title)
                putExtra(MusicPlayerService.EXTRA_ARTIST, artist)
                trackId?.let { putExtra(MusicPlayerService.EXTRA_TRACK_ID, it) }
            }
        )
    }

    fun cycleLoopMode() {
        serviceConnection.service.value?.cycleRepeatMode()
    }

    fun seekTo(positionMs: Long) {
        serviceConnection.service.value?.seekTo(positionMs)
    }

    fun updatePlaybackProgress() {
        serviceConnection.service.value?.updatePlaybackProgress()
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
