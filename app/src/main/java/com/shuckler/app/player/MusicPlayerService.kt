package com.shuckler.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shuckler.app.MainActivity
import com.shuckler.app.R
import com.shuckler.app.equalizer.EqualizerPreferences
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.widget.NowPlayingWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerService : Service() {

    private val binder = LocalBinder()

    private var mediaSession: MediaSession? = null
        get() {
            if (field == null) {
                field = MediaSession(this, "ShucklerPlayback").apply {
                    setCallback(object : MediaSession.Callback() {
                        override fun onPlay() {
                            this@MusicPlayerService.play()
                        }

                        override fun onPause() {
                            this@MusicPlayerService.pause()
                        }

                        override fun onSkipToNext() {
                            skipToNext()
                        }

                        override fun onSkipToPrevious() {
                            skipToPrevious()
                        }
                    })
                    setSessionActivity(
                        PendingIntent.getActivity(
                            this@MusicPlayerService,
                            0,
                            Intent(this@MusicPlayerService, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
            return field
        }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun attachEqualizerToSession(audioSessionId: Int) {
        // Session 0 is deprecated on modern devices - only attach when we have a real session from playback
        if (audioSessionId == 0) return
        equalizer?.release()
        equalizer = try {
            Equalizer(0, audioSessionId)
        } catch (_: Exception) {
            null
        }
        equalizer?.let { applyEqualizerFromPrefs() }
    }

    private val _visualizerFftData = MutableStateFlow<ByteArray?>(null)
    val visualizerFftData: StateFlow<ByteArray?> = _visualizerFftData.asStateFlow()

    private fun attachVisualizerToSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            visualizer?.release()
        } catch (_: Exception) { }
        visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, bytes: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        _visualizerFftData.value = fft?.copyOf()
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(UnstableApi::class)
    private val audioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEqualizerToSession(audioSessionId)
            attachVisualizerToSession(audioSessionId)
        }
    }

    private var _exoPlayer: ExoPlayer? = null
    private val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = ExoPlayer.Builder(applicationContext)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .build()
                    .apply {
                        repeatMode = _repeatMode.value
                        val uri = "android.resource://$packageName/${R.raw.test_song}".toUri()
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                        addListener(audioSessionListener)
                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                _isPlaying.value = playing
                                updateMediaSession()
                                updateNotification()
                            }
                        })
                    }
            }
            return _exoPlayer!!
        }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow(DefaultTrackInfo.TITLE)
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow(DefaultTrackInfo.ARTIST)
    val currentTrackArtist: StateFlow<String> = _currentTrackArtist.asStateFlow()

    private val _currentTrackThumbnailUrl = MutableStateFlow<String?>(null)
    val currentTrackThumbnailUrl: StateFlow<String?> = _currentTrackThumbnailUrl.asStateFlow()

    @Volatile
    private var currentArtworkBitmap: Bitmap? = null

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** When set (play from library), used for auto-delete-after-playback when track ends. */
    private var currentTrackId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Last time we saved playback position (for periodic save every 5 seconds). */
    private var lastSavePositionTimeMs: Long = 0L

    private val MIN_POSITION_TO_RESUME_MS = 10_000L
    private val MIN_REMAINING_TO_RESUME_MS = 5_000L

    /** Sleep timer: end time in ms (null = off). When reached, pause playback optionally with fade. */
    @Volatile
    private var sleepTimerEndMs: Long? = null

    /** When true, stop at end of current track instead of advancing. */
    @Volatile
    private var sleepTimerEndOfTrack: Boolean = false

    private var sleepTimerRunnable: Runnable? = null

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private fun getSleepTimerFadeLastMinute(): Boolean =
        (applicationContext as? ShucklerApplication)?.downloadManager?.sleepTimerFadeLastMinute ?: false

    fun startSleepTimer(durationMs: Long, endOfTrack: Boolean) {
        cancelSleepTimer()
        if (endOfTrack) {
            sleepTimerEndOfTrack = true
            sleepTimerEndMs = null
            _sleepTimerRemainingMs.value = SLEEP_TIMER_END_OF_TRACK
        } else {
            sleepTimerEndOfTrack = false
            sleepTimerEndMs = System.currentTimeMillis() + durationMs
            _sleepTimerRemainingMs.value = durationMs
            scheduleSleepTimerCheck()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerEndMs = null
        sleepTimerEndOfTrack = false
        sleepTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        sleepTimerRunnable = null
        _sleepTimerRemainingMs.value = null
    }

    private fun scheduleSleepTimerCheck() {
        val endMs = sleepTimerEndMs ?: return
        val now = System.currentTimeMillis()
        val remaining = (endMs - now).coerceAtLeast(0L)
        _sleepTimerRemainingMs.value = remaining
        if (remaining <= 0) {
            onSleepTimerFired()
            return
        }
        sleepTimerRunnable = Runnable {
            scheduleSleepTimerCheck()
        }
        mainHandler.postDelayed(sleepTimerRunnable!!, minOf(remaining, 1000L))
    }

    private fun onSleepTimerFired() {
        cancelSleepTimer()
        val fadeLastMinute = getSleepTimerFadeLastMinute()
        if (fadeLastMinute) {
            startSleepTimerFadeOut { pause() }
        } else {
            pause()
        }
    }

    private fun checkSleepTimerEndOfTrack() {
        if (sleepTimerEndOfTrack) {
            cancelSleepTimer()
            val fadeLastMinute = getSleepTimerFadeLastMinute()
            if (fadeLastMinute) {
                startSleepTimerFadeOut { pause() }
            } else {
                pause()
            }
        }
    }

    /** Fade out over 60 seconds (sleep timer), then run onComplete. */
    private fun startSleepTimerFadeOut(onComplete: () -> Unit) {
        val durationMs = 60_000
        val steps = (durationMs / 50).coerceAtLeast(2)
        val stepMs = durationMs / steps
        var step = 0
        fun runStep() {
            if (step > steps) {
                _exoPlayer?.volume = 0f
                onComplete()
                return
            }
            _exoPlayer?.volume = 1f - (step.toFloat() / steps)
            step += 1
            mainHandler.postDelayed({ runStep() }, stepMs.toLong())
        }
        runStep()
    }

    private fun getCrossfadeDurationMs(): Int =
        (applicationContext as? ShucklerApplication)?.downloadManager?.crossfadeDurationMs ?: 0

    private fun startFadeOut(onComplete: () -> Unit) {
        val durationMs = getCrossfadeDurationMs().coerceAtLeast(100)
        val steps = (durationMs / 50).coerceAtLeast(2)
        val stepMs = durationMs / steps
        var step = 0
        fun runStep() {
            if (step > steps) {
                _exoPlayer?.volume = 0f
                onComplete()
                return
            }
            _exoPlayer?.volume = 1f - (step.toFloat() / steps)
            step += 1
            mainHandler.postDelayed({ runStep() }, stepMs.toLong())
        }
        runStep()
    }

    private fun startFadeIn() {
        val durationMs = getCrossfadeDurationMs().coerceAtLeast(100)
        val steps = (durationMs / 50).coerceAtLeast(2)
        val stepMs = durationMs / steps
        var step = 0
        fun runStep() {
            if (step > steps) {
                _exoPlayer?.volume = 1f
                return
            }
            _exoPlayer?.volume = step.toFloat() / steps
            step += 1
            mainHandler.postDelayed({ runStep() }, stepMs.toLong())
        }
        runStep()
    }

    private val queue = mutableListOf<QueueItem>()
    private var currentQueueIndex = -1

    /** True after we've started crossfade-out for the current track (so we don't trigger twice). */
    private var crossfadeStartedForCurrentTrack = false

    private val _queueInfo = MutableStateFlow(0 to 0) // (current 1-based index, total size)
    val queueInfo: StateFlow<Pair<Int, Int>> = _queueInfo.asStateFlow()

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems: StateFlow<List<QueueItem>> = _queueItems.asStateFlow()

    /** Equalizer attached to ExoPlayer audio session. Null if not available or not yet attached. */
    @Volatile
    private var equalizer: Equalizer? = null
    private var visualizer: Visualizer? = null

    /** Call when user changes equalizer settings; reapplies from prefs. */
    fun applyEqualizerFromPrefs() {
        equalizer?.let { eq ->
            val enabled = EqualizerPreferences.isEnabled(this)
            eq.enabled = enabled
            if (!enabled) return@let
            val ourLevelsDb = EqualizerPreferences.getEffectiveBandLevelsDb(this)
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val n = eq.numberOfBands.toInt()
            val ourFreqHz = EqualizerPreferences.BAND_FREQUENCIES_HZ
            for (i in 0 until n) {
                val centerMhz = eq.getCenterFreq(i.toShort())
                val centerHz = (centerMhz / 1000).toInt()
                val nearest = ourFreqHz.minByOrNull { kotlin.math.abs(it - centerHz) } ?: ourFreqHz[0]
                val ourIndex = ourFreqHz.indexOf(nearest).coerceIn(0, ourLevelsDb.lastIndex)
                val db = ourLevelsDb.getOrElse(ourIndex) { 0 }
                val millibels = (db * 100).coerceIn(minMb, maxMb)
                eq.setBandLevel(i.toShort(), millibels.toShort())
            }
        }
    }

    private fun updateQueueInfo() {
        val total = queue.size
        val current = if (total > 0 && currentQueueIndex in queue.indices) currentQueueIndex + 1 else 0
        _queueInfo.value = current to total
        _queueItems.value = queue.toList()
    }

    /** Convert QueueItem to MediaItem for ExoPlayer. Supports chapter tracks (startMs/endMs). */
    private fun queueItemToMediaItem(item: QueueItem): MediaItem {
        val uri = item.uri.toUri()
        return if (item.startMs != null && item.endMs != null && item.startMs >= 0 && item.endMs > item.startMs) {
            MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(item.startMs)
                        .setEndPositionMs(item.endMs)
                        .build()
                )
                .build()
        } else {
            MediaItem.fromUri(uri)
        }
    }

    /** Update metadata (title, artist, etc.) from queue item at index. */
    private fun updateMetadataFromQueue(index: Int) {
        if (index !in queue.indices) return
        val item = queue[index]
        currentTrackId = item.trackId
        _currentTrackTitle.value = item.title
        _currentTrackArtist.value = item.artist
        _currentTrackThumbnailUrl.value = item.thumbnailUrl
        currentArtworkBitmap = null
        item.thumbnailUrl?.let { url -> loadArtworkForNotification(url) }
    }

    /** Track if we've added the playlist listener (for gapless mode) to avoid duplicates. */
    private var playlistListenerAdded = false

    /**
     * Sync queue to ExoPlayer using setMediaItems for gapless playback and preloading.
     * ExoPlayer will preload the next track and transition seamlessly when current ends.
     */
    private fun syncQueueToExoPlayer(startIndex: Int, startPositionMs: Long = 0L) {
        if (queue.isEmpty()) return
        val mediaItems = queue.map { queueItemToMediaItem(it) }
        _exoPlayer?.let { player ->
            if (!playlistListenerAdded) {
                player.addListener(createPlaylistListener())
                playlistListenerAdded = true
            }
            player.repeatMode = _repeatMode.value
            player.setMediaItems(mediaItems, startIndex, startPositionMs)
            player.prepare()
        } ?: run {
            playlistListenerAdded = true
            _exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    repeatMode = _repeatMode.value
                    addListener(audioSessionListener)
                    addListener(createPlaylistListener())
                    setMediaItems(mediaItems, startIndex, startPositionMs)
                    prepare()
                }
        }
        updateMetadataFromQueue(startIndex)
        queue[startIndex].trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
        (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
        updateQueueInfo()
    }

    /** Listener for playlist mode: onMediaItemTransition, onPlaybackStateChanged. */
    private fun createPlaylistListener(): Player.Listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            updateMediaSession()
            updateNotification()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = _exoPlayer?.currentMediaItemIndex ?: 0
            if (idx in queue.indices) {
                val prevTrackId = currentTrackId
                currentQueueIndex = idx
                updateMetadataFromQueue(idx)
                queue[idx].trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
                (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
                prevTrackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.considerAutoDeleteAfterPlayback(it) }
                updateQueueInfo()
                updateMediaSession()
                updateNotification()
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                updatePlaybackProgress()
            }
            if (playbackState == Player.STATE_ENDED) {
                val idx = _exoPlayer?.currentMediaItemIndex ?: 0
                if (idx in queue.indices) {
                    (applicationContext as? ShucklerApplication)?.downloadManager?.considerAutoDeleteAfterPlayback(currentTrackId)
                }
                currentTrackId = null
                if (queue.isEmpty() || _exoPlayer?.currentMediaItemIndex == queue.size - 1) {
                    if (_repeatMode.value != Player.REPEAT_MODE_ONE) pause()
                }
            }
        }
    }

    /**
     * Move a queue item from one position to another. Updates currentQueueIndex if it points to a moved item.
     */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        currentQueueIndex = when {
            currentQueueIndex == fromIndex -> toIndex
            currentQueueIndex == toIndex -> fromIndex
            fromIndex < currentQueueIndex && toIndex >= currentQueueIndex -> currentQueueIndex - 1
            fromIndex > currentQueueIndex && toIndex <= currentQueueIndex -> currentQueueIndex + 1
            else -> currentQueueIndex
        }
        if (useGaplessPlaylist() && _exoPlayer?.mediaItemCount == queue.size) {
            _exoPlayer?.moveMediaItem(fromIndex, toIndex)
        }
        updateQueueInfo()
        updateNotification()
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode
        _exoPlayer?.repeatMode = mode
    }

    fun cycleRepeatMode(): Int {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ONE
        }
        setRepeatMode(next)
        return next
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs.coerceIn(0L, (_durationMs.value).takeIf { it > 0 } ?: Long.MAX_VALUE))
    }

    private fun getStoredPlaybackSpeed(): Float =
        (applicationContext as? ShucklerApplication)?.downloadManager?.playbackSpeed ?: 1f

    fun setPlaybackSpeed(speed: Float) {
        val s = speed.coerceIn(0.5f, 2f)
        _playbackSpeed.value = s
        _exoPlayer?.setPlaybackSpeed(s)
        (applicationContext as? ShucklerApplication)?.downloadManager?.playbackSpeed = s
    }

    fun updatePlaybackProgress() {
        val endMs = sleepTimerEndMs
        if (endMs != null) {
            val remaining = (endMs - System.currentTimeMillis()).coerceAtLeast(0L)
            _sleepTimerRemainingMs.value = remaining
            if (remaining <= 0) {
                onSleepTimerFired()
            }
        }
        _exoPlayer?.let { player ->
            val position = player.currentPosition
            val dur = player.duration
            _playbackPositionMs.value = position
            _durationMs.value = if (dur == C.TIME_UNSET) 0L else dur
            // Periodic save for "Continue listening" (every 5 seconds)
            val now = System.currentTimeMillis()
            if (now - lastSavePositionTimeMs >= 5000L) {
                lastSavePositionTimeMs = now
                saveCurrentPositionIfNeeded()
            }
            // Start crossfade before track end (only when NOT using gapless playlist - ExoPlayer auto-advances)
            val crossfadeMs = getCrossfadeDurationMs()
            val remaining = dur - position
            if (!useGaplessPlaylist() && dur != C.TIME_UNSET && dur > 0 && crossfadeMs > 0 &&
                queue.isNotEmpty() && currentQueueIndex + 1 < queue.size &&
                _repeatMode.value != Player.REPEAT_MODE_ONE && !crossfadeStartedForCurrentTrack &&
                remaining in 1..crossfadeMs
            ) {
                crossfadeStartedForCurrentTrack = true
                startFadeOut {
                    saveCurrentPositionIfNeeded()
                    currentQueueIndex++
                    playQueueItemAt(currentQueueIndex, fadeIn = true)
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        _playbackSpeed.value = getStoredPlaybackSpeed()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_NEXT -> skipToNext()
            ACTION_PREVIOUS -> skipToPrevious()
            ACTION_PLAY_URI -> {
                val uriString = intent.getStringExtra(EXTRA_URI)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val trackId = intent.getStringExtra(EXTRA_TRACK_ID)
                val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL)
                val startMs = intent.getLongExtra(EXTRA_START_MS, -1L).takeIf { it >= 0 }
                val endMs = intent.getLongExtra(EXTRA_END_MS, -1L).takeIf { it >= 0 }
                if (uriString != null) {
                    val item = QueueItem(uriString, title, artist, trackId, thumbnailUrl, startMs, endMs)
                    queue.clear()
                    queue.add(item)
                    currentQueueIndex = 0
                    updateQueueInfo()
                    lastSavePositionTimeMs = System.currentTimeMillis()
                    if (useGaplessPlaylist()) {
                        syncQueueToExoPlayer(0)
                        play()
                    } else {
                        setMediaUri(uriString.toUri(), title, artist, trackId, thumbnailUrl, startMs, endMs)
                        trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
                        (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
                        play()
                    }
                }
            }
            ACTION_PLAY_WITH_QUEUE -> {
                val queueJson = intent.getStringExtra(EXTRA_QUEUE_JSON)
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                if (!queueJson.isNullOrBlank()) {
                    setQueueAndPlay(queueJson, startIndex)
                }
            }
            ACTION_ADD_TO_QUEUE_NEXT -> handleAddToQueue(intent, next = true)
            ACTION_ADD_TO_QUEUE_END -> handleAddToQueue(intent, next = false)
            ACTION_START_SLEEP_TIMER -> {
                val durationMs = intent.getLongExtra(EXTRA_SLEEP_DURATION_MS, 0L)
                val endOfTrack = intent.getBooleanExtra(EXTRA_SLEEP_END_OF_TRACK, false)
                startSleepTimer(durationMs, endOfTrack)
            }
            ACTION_CANCEL_SLEEP_TIMER -> cancelSleepTimer()
        }
        return START_STICKY
    }

    /**
     * Add a single track to the queue (from Library). When queue is empty or nothing is playing,
     * set queue to this item and start playback. Otherwise insert at current+1 (next) or append (end).
     */
    private fun handleAddToQueue(intent: Intent, next: Boolean) {
        val uriString = intent.getStringExtra(EXTRA_URI) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
        val trackId = intent.getStringExtra(EXTRA_TRACK_ID)
        val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL)
        val startMs = intent.getLongExtra(EXTRA_START_MS, -1L).takeIf { it >= 0 }
        val endMs = intent.getLongExtra(EXTRA_END_MS, -1L).takeIf { it >= 0 }
        val item = QueueItem(uriString, title, artist, trackId, thumbnailUrl, startMs, endMs)

        if (queue.isEmpty()) {
            queue.add(item)
            currentQueueIndex = 0
            updateQueueInfo()
            playQueueItemAt(0, fadeIn = false)
        } else {
            if (next) {
                val insertAt = (currentQueueIndex + 1).coerceIn(0, queue.size)
                queue.add(insertAt, item)
                if (currentQueueIndex >= insertAt) currentQueueIndex++
                if (useGaplessPlaylist() && _exoPlayer?.mediaItemCount == queue.size - 1) {
                    _exoPlayer?.addMediaItem(insertAt, queueItemToMediaItem(item))
                }
            } else {
                queue.add(item)
                if (useGaplessPlaylist() && _exoPlayer?.mediaItemCount == queue.size - 1) {
                    _exoPlayer?.addMediaItem(queueItemToMediaItem(item))
                }
            }
            updateQueueInfo()
            updateNotification()
        }
    }

    fun play() {
        mediaSession?.isActive = true
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        exoPlayer.setPlaybackSpeed(_playbackSpeed.value)
        exoPlayer.play()
        updateMediaSession()
    }

    fun pause() {
        saveCurrentPositionIfNeeded()
        cancelSleepTimer()
        exoPlayer.pause()
        updateMediaSession()
        updateNotification()
    }

    /** Save playback position for "Continue listening" / resume. Called on pause, track change, and periodically. */
    private fun saveCurrentPositionIfNeeded() {
        val trackId = currentTrackId ?: return
        val pos = _exoPlayer?.currentPosition ?: 0L
        (applicationContext as? ShucklerApplication)?.downloadManager?.updateLastPosition(trackId, pos)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun skipToNext() {
        if (queue.isEmpty()) return
        saveCurrentPositionIfNeeded()
        if (currentQueueIndex + 1 < queue.size) {
            if (useGaplessPlaylist() && _exoPlayer?.mediaItemCount == queue.size) {
                currentQueueIndex++
                _exoPlayer?.seekTo(currentQueueIndex, 0L)
                updateMetadataFromQueue(currentQueueIndex)
                queue[currentQueueIndex].trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
                (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
                updateQueueInfo()
            } else {
                val durationMs = getCrossfadeDurationMs()
                if (durationMs > 0) {
                    startFadeOut {
                        currentQueueIndex++
                        playQueueItemAt(currentQueueIndex, fadeIn = true)
                    }
                } else {
                    currentQueueIndex++
                    playQueueItemAt(currentQueueIndex)
                }
            }
        } else {
            pause()
        }
    }

    private fun skipToPrevious() {
        if (queue.isEmpty()) {
            _exoPlayer?.seekTo(0)
            return
        }
        saveCurrentPositionIfNeeded()
        val positionMs = _exoPlayer?.currentPosition ?: 0L
        if (useGaplessPlaylist() && _exoPlayer?.mediaItemCount == queue.size) {
            if (currentQueueIndex > 0 && positionMs < 3000) currentQueueIndex--
            else if (currentQueueIndex > 0) currentQueueIndex--
            if (currentQueueIndex >= 0) {
                _exoPlayer?.seekTo(currentQueueIndex, 0L)
                updateMetadataFromQueue(currentQueueIndex)
                queue[currentQueueIndex].trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
                (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
                updateQueueInfo()
            } else {
                exoPlayer.seekTo(0)
            }
        } else {
            if (positionMs < 3000 && currentQueueIndex > 0) {
                currentQueueIndex--
                playQueueItemAt(currentQueueIndex)
            } else if (currentQueueIndex > 0) {
                currentQueueIndex--
                playQueueItemAt(currentQueueIndex)
            } else {
                exoPlayer.seekTo(0)
            }
        }
    }

    /** Use gapless playlist mode (setMediaItems) when crossfade is off. */
    private fun useGaplessPlaylist(): Boolean = getCrossfadeDurationMs() == 0

    /**
     * Set the play queue and start at the given index. Call from ACTION_PLAY_WITH_QUEUE.
     */
    private fun setQueueAndPlay(queueJson: String, startIndex: Int) {
        val list = QueueItem.listFromJson(queueJson)
        if (list.isEmpty()) return
        queue.clear()
        queue.addAll(list)
        currentQueueIndex = startIndex.coerceIn(0, list.size - 1)
        updateQueueInfo()
        lastSavePositionTimeMs = System.currentTimeMillis()
        if (useGaplessPlaylist() && queue.size > 1) {
            syncQueueToExoPlayer(currentQueueIndex)
            play()
        } else {
            playQueueItemAt(currentQueueIndex, fadeIn = false)
        }
    }

    fun playQueueItemAt(index: Int, fadeIn: Boolean = false) {
        if (index !in queue.indices) return
        crossfadeStartedForCurrentTrack = false
        if (fadeIn) _exoPlayer?.volume = 0f
        if (useGaplessPlaylist() && queue.size > 1 && _exoPlayer?.mediaItemCount == queue.size) {
            _exoPlayer?.seekTo(index, 0L)
            currentQueueIndex = index
            updateMetadataFromQueue(index)
            queue[index].trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
            (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
            updateQueueInfo()
            play()
            if (fadeIn) startFadeIn()
        } else {
            val item = queue[index]
            setMediaUri(item.uri.toUri(), item.title, item.artist, item.trackId, item.thumbnailUrl, item.startMs, item.endMs)
            updateQueueInfo()
            item.trackId?.let { (applicationContext as? ShucklerApplication)?.downloadManager?.incrementPlayCount(it) }
            (applicationContext as? ShucklerApplication)?.listeningPersonalityManager?.recordPlaySession()
            play()
            if (fadeIn) startFadeIn()
        }
    }

    /**
     * Called when the current track ends (STATE_ENDED). Handles auto-delete and queue advance.
     * When using gapless playlist (setMediaItems), ExoPlayer auto-advances; skip manual advance.
     */
    private fun onCurrentTrackEnded() {
        saveCurrentPositionIfNeeded()
        if (sleepTimerEndOfTrack) {
            checkSleepTimerEndOfTrack()
            return
        }
        (applicationContext as? ShucklerApplication)?.downloadManager
            ?.considerAutoDeleteAfterPlayback(currentTrackId)
        currentTrackId = null
        if (useGaplessPlaylist() && queue.size > 1) return
        if (queue.isNotEmpty() && _repeatMode.value != Player.REPEAT_MODE_ONE) {
            if (currentQueueIndex + 1 < queue.size) {
                val durationMs = getCrossfadeDurationMs()
                if (durationMs > 0) {
                    startFadeOut {
                        currentQueueIndex++
                        playQueueItemAt(currentQueueIndex, fadeIn = true)
                    }
                } else {
                    currentQueueIndex++
                    playQueueItemAt(currentQueueIndex)
                }
            } else {
                pause()
            }
        }
    }

    /**
     * Switch playback to a file (e.g. downloaded track). Call before or instead of play().
     * @param trackId Optional library track id for auto-delete-after-playback when track ends.
     * @param thumbnailUrl Optional artwork URL for notification and UI.
     * @param startMs Optional start position for chapter/virtual tracks.
     * @param endMs Optional end position for chapter/virtual tracks.
     */
    fun setMediaUri(
        uri: Uri,
        title: String,
        artist: String,
        trackId: String? = null,
        thumbnailUrl: String? = null,
        startMs: Long? = null,
        endMs: Long? = null
    ) {
        currentTrackId = trackId
        lastSavePositionTimeMs = System.currentTimeMillis()
        _currentTrackTitle.value = title
        _currentTrackArtist.value = artist
        _currentTrackThumbnailUrl.value = thumbnailUrl
        currentArtworkBitmap = null
        thumbnailUrl?.let { url -> loadArtworkForNotification(url) }
        val mediaItem = if (startMs != null && endMs != null && startMs >= 0 && endMs > startMs) {
            MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
        } else {
            MediaItem.fromUri(uri)
        }
        val playbackEndedListener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                updateMediaSession()
                updateNotification()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    updatePlaybackProgress()
                }
                if (playbackState == Player.STATE_ENDED) {
                    onCurrentTrackEnded()
                }
                // Resume from last position when ready (Phase 32: Continue listening)
                if (playbackState == Player.STATE_READY && trackId != null) {
                    val dm = (applicationContext as? ShucklerApplication)?.downloadManager ?: return
                    val lastPos = dm.getLastPosition(trackId) ?: return
                    val dur = _exoPlayer?.duration ?: 0L
                    val effectiveDur = if (dur != C.TIME_UNSET && dur > 0) dur else Long.MAX_VALUE
                    if (lastPos >= MIN_POSITION_TO_RESUME_MS && lastPos < effectiveDur - MIN_REMAINING_TO_RESUME_MS) {
                        _exoPlayer?.seekTo(lastPos)
                        lastSavePositionTimeMs = System.currentTimeMillis()
                    }
                }
            }
        }
        _exoPlayer?.let { player ->
            player.addListener(playbackEndedListener)
            player.repeatMode = _repeatMode.value
            player.setMediaItem(mediaItem)
            player.prepare()
        } ?: run {
            _exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    repeatMode = _repeatMode.value
                    addListener(audioSessionListener)
                    addListener(playbackEndedListener)
                    setMediaItem(mediaItem)
                    prepare()
                }
        }
        updateMediaSession()
        updateNotification()
    }

    private fun updateMediaSession() {
        mediaSession?.let { session ->
            val state = if (_exoPlayer?.isPlaying == true) {
                PlaybackState.STATE_PLAYING
            } else {
                PlaybackState.STATE_PAUSED
            }
            val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, exoPlayer.currentPosition, 1f)
                    .build()
            )
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, _currentTrackTitle.value)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, _currentTrackArtist.value)
                    .build()
            )
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
        // Update home screen widget
        NowPlayingWidgetProvider.updateAllWidgets(
            applicationContext,
            _currentTrackTitle.value,
            _currentTrackArtist.value,
            _exoPlayer?.isPlaying == true,
            currentArtworkBitmap
        )
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = _exoPlayer?.isPlaying == true
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_pause),
                getServicePendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_play),
                getServicePendingIntent(ACTION_PLAY)
            )
        }

        val previousAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            getString(R.string.notification_previous),
            getServicePendingIntent(ACTION_PREVIOUS)
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            getString(R.string.notification_next),
            getServicePendingIntent(ACTION_NEXT)
        )

        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2) // prev, play/pause, next
        // MediaStyle.setMediaSession() expects MediaSessionCompat.Token; we use platform MediaSession.
        // Notification still shows prev/play/next; lock screen uses platform session from updateMediaSession().

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(_currentTrackTitle.value)
            .setContentText(_currentTrackArtist.value)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(mediaStyle)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        currentArtworkBitmap?.let { builder.setLargeIcon(it) }
        return builder.build()
    }

    private fun loadArtworkForNotification(url: String) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.getInputStream().use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                        inJustDecodeBounds = false
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, opts)
                    if (bitmap != null && _currentTrackThumbnailUrl.value == url) {
                        val maxSize = 256
                        val scale = minOf(
                            maxSize.toFloat() / bitmap.width,
                            maxSize.toFloat() / bitmap.height
                        ).coerceAtMost(1f)
                        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                        val scaled = bitmap.scale(w, h, true)
                        if (scaled != bitmap) bitmap.recycle()
                        currentArtworkBitmap = scaled
                        mainHandler.post { updateNotification() }
                    }
                }
            } catch (_: Exception) { }
        }.start()
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        equalizer?.release()
        equalizer = null
        try { visualizer?.release() } catch (_: Exception) { }
        visualizer = null
        _exoPlayer?.release()
        _exoPlayer = null
    }

    companion object {
        /** Sentinel for "end of track" mode: remaining is this value (not a real ms count). */
        const val SLEEP_TIMER_END_OF_TRACK: Long = -1L
        const val ACTION_PLAY = "com.shuckler.app.PLAY"
        const val ACTION_PAUSE = "com.shuckler.app.PAUSE"
        const val ACTION_TOGGLE = "com.shuckler.app.TOGGLE"
        const val ACTION_NEXT = "com.shuckler.app.NEXT"
        const val ACTION_PREVIOUS = "com.shuckler.app.PREVIOUS"
        const val ACTION_PLAY_URI = "com.shuckler.app.PLAY_URI"
        const val ACTION_PLAY_WITH_QUEUE = "com.shuckler.app.PLAY_WITH_QUEUE"
        const val ACTION_ADD_TO_QUEUE_NEXT = "com.shuckler.app.ADD_TO_QUEUE_NEXT"
        const val ACTION_ADD_TO_QUEUE_END = "com.shuckler.app.ADD_TO_QUEUE_END"
        const val ACTION_START_SLEEP_TIMER = "com.shuckler.app.START_SLEEP_TIMER"
        const val ACTION_CANCEL_SLEEP_TIMER = "com.shuckler.app.CANCEL_SLEEP_TIMER"
        const val EXTRA_URI = "uri"
        const val EXTRA_SLEEP_DURATION_MS = "sleep_duration_ms"
        const val EXTRA_SLEEP_END_OF_TRACK = "sleep_end_of_track"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_QUEUE_JSON = "queue_json"
        const val EXTRA_START_INDEX = "start_index"
        const val EXTRA_THUMBNAIL_URL = "thumbnail_url"
        const val EXTRA_START_MS = "start_ms"
        const val EXTRA_END_MS = "end_ms"
        private const val CHANNEL_ID = "shuckler_playback"
        private const val NOTIFICATION_ID = 1
    }
}
