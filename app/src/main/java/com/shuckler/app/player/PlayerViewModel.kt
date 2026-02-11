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

    val currentTrackThumbnailUrl: Flow<String?> = serviceConnection.service
        .flatMapLatest { service -> service?.currentTrackThumbnailUrl ?: flowOf(null) }

    val repeatMode: Flow<Int> = serviceConnection.service
        .flatMapLatest { service -> service?.repeatMode ?: flowOf(Player.REPEAT_MODE_OFF) }

    val playbackSpeed: Flow<Float> = serviceConnection.service
        .flatMapLatest { service -> service?.playbackSpeed ?: flowOf(1f) }

    val playbackPositionMs: Flow<Long> = serviceConnection.service
        .flatMapLatest { service -> service?.playbackPositionMs ?: flowOf(0L) }

    val durationMs: Flow<Long> = serviceConnection.service
        .flatMapLatest { service -> service?.durationMs ?: flowOf(0L) }

    val queueInfo: Flow<Pair<Int, Int>> = serviceConnection.service
        .flatMapLatest { service -> service?.queueInfo ?: flowOf(0 to 0) }

    val queueItems: Flow<List<QueueItem>> = serviceConnection.service
        .flatMapLatest { service -> service?.queueItems ?: flowOf(emptyList()) }

    val sleepTimerRemainingMs: Flow<Long?> = serviceConnection.service
        .flatMapLatest { service -> service?.sleepTimerRemainingMs ?: flowOf(null) }

    fun playQueueItemAt(index: Int) {
        serviceConnection.service.value?.playQueueItemAt(index)
    }

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
     * No queue; Next/Previous will be no-op or seek to 0.
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

    /**
     * Play with a queue (e.g. full library). Next/Previous will move through the queue.
     * @param items Ordered list of queue items
     * @param startIndex Index in [items] to start playing
     */
    fun playTrackWithQueue(items: List<QueueItem>, startIndex: Int) {
        if (items.isEmpty()) return
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY_WITH_QUEUE
                putExtra(MusicPlayerService.EXTRA_QUEUE_JSON, QueueItem.listToJson(items))
                putExtra(MusicPlayerService.EXTRA_START_INDEX, startIndex.coerceIn(0, items.size - 1))
            }
        )
    }

    /**
     * Add a track to play next (insert after current) or to the end of the queue.
     * When queue is empty or nothing is playing, starts playback with this track.
     */
    fun addToQueueNext(item: QueueItem) {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_ADD_TO_QUEUE_NEXT
                putExtra(MusicPlayerService.EXTRA_URI, item.uri)
                putExtra(MusicPlayerService.EXTRA_TITLE, item.title)
                putExtra(MusicPlayerService.EXTRA_ARTIST, item.artist)
                item.trackId?.let { putExtra(MusicPlayerService.EXTRA_TRACK_ID, it) }
                item.thumbnailUrl?.let { putExtra(MusicPlayerService.EXTRA_THUMBNAIL_URL, it) }
            }
        )
    }

    fun addToQueueEnd(item: QueueItem) {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_ADD_TO_QUEUE_END
                putExtra(MusicPlayerService.EXTRA_URI, item.uri)
                putExtra(MusicPlayerService.EXTRA_TITLE, item.title)
                putExtra(MusicPlayerService.EXTRA_ARTIST, item.artist)
                item.trackId?.let { putExtra(MusicPlayerService.EXTRA_TRACK_ID, it) }
                item.thumbnailUrl?.let { putExtra(MusicPlayerService.EXTRA_THUMBNAIL_URL, it) }
            }
        )
    }

    fun skipToNext() {
        applicationContext.startService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_NEXT
            }
        )
    }

    fun skipToPrevious() {
        applicationContext.startService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PREVIOUS
            }
        )
    }

    fun cycleLoopMode() {
        serviceConnection.service.value?.cycleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        serviceConnection.service.value?.setPlaybackSpeed(speed)
    }

    fun seekTo(positionMs: Long) {
        serviceConnection.service.value?.seekTo(positionMs)
    }

    fun updatePlaybackProgress() {
        serviceConnection.service.value?.updatePlaybackProgress()
    }

    fun startSleepTimer(durationMs: Long, endOfTrack: Boolean) {
        applicationContext.startForegroundService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_START_SLEEP_TIMER
                putExtra(MusicPlayerService.EXTRA_SLEEP_DURATION_MS, durationMs)
                putExtra(MusicPlayerService.EXTRA_SLEEP_END_OF_TRACK, endOfTrack)
            }
        )
        serviceConnection.service.value?.startSleepTimer(durationMs, endOfTrack)
    }

    fun cancelSleepTimer() {
        applicationContext.startService(
            Intent(applicationContext, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_CANCEL_SLEEP_TIMER
            }
        )
        serviceConnection.service.value?.cancelSleepTimer()
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
