package com.shuckler.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shuckler.app.MainActivity
import com.shuckler.app.R
import com.shuckler.app.ShucklerApplication
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

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** When set (play from library), used for auto-delete-after-playback when track ends. */
    private var currentTrackId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

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
            step++
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
            step++
            mainHandler.postDelayed({ runStep() }, stepMs.toLong())
        }
        runStep()
    }

    private val queue = mutableListOf<QueueItem>()
    private var currentQueueIndex = -1

    private val _queueInfo = MutableStateFlow(0 to 0) // (current 1-based index, total size)
    val queueInfo: StateFlow<Pair<Int, Int>> = _queueInfo.asStateFlow()

    private fun updateQueueInfo() {
        val total = queue.size
        val current = if (total > 0 && currentQueueIndex in queue.indices) currentQueueIndex + 1 else 0
        _queueInfo.value = current to total
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

    fun updatePlaybackProgress() {
        _exoPlayer?.let { player ->
            _playbackPositionMs.value = player.currentPosition
            val dur = player.duration
            _durationMs.value = if (dur == C.TIME_UNSET) 0L else dur
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

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
                if (uriString != null) {
                    queue.clear()
                    currentQueueIndex = -1
                    updateQueueInfo()
                    setMediaUri(uriString.toUri(), title, artist, trackId, thumbnailUrl)
                    play()
                }
            }
            ACTION_PLAY_WITH_QUEUE -> {
                val queueJson = intent.getStringExtra(EXTRA_QUEUE_JSON)
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                if (!queueJson.isNullOrBlank()) {
                    setQueueAndPlay(queueJson, startIndex)
                }
            }
        }
        return START_STICKY
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
        exoPlayer.play()
        updateMediaSession()
    }

    fun pause() {
        exoPlayer.pause()
        updateMediaSession()
        updateNotification()
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

    private fun skipToPrevious() {
        if (queue.isEmpty()) {
            _exoPlayer?.seekTo(0)
            return
        }
        val positionMs = _exoPlayer?.currentPosition ?: 0L
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
        playQueueItemAt(currentQueueIndex, fadeIn = false)
    }

    private fun playQueueItemAt(index: Int, fadeIn: Boolean = false) {
        if (index !in queue.indices) return
        if (fadeIn) _exoPlayer?.volume = 0f
        val item = queue[index]
        val uri = Uri.parse(item.uri)
        setMediaUri(uri, item.title, item.artist, item.trackId, item.thumbnailUrl)
        updateQueueInfo()
        play()
        if (fadeIn) startFadeIn()
    }

    /**
     * Called when the current track ends (STATE_ENDED). Handles auto-delete and queue advance.
     */
    private fun onCurrentTrackEnded() {
        (applicationContext as? ShucklerApplication)?.downloadManager
            ?.considerAutoDeleteAfterPlayback(currentTrackId)
        currentTrackId = null
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
     */
    fun setMediaUri(uri: Uri, title: String, artist: String, trackId: String? = null, thumbnailUrl: String? = null) {
        currentTrackId = trackId
        _currentTrackTitle.value = title
        _currentTrackArtist.value = artist
        _currentTrackThumbnailUrl.value = thumbnailUrl
        currentArtworkBitmap = null
        thumbnailUrl?.let { url -> loadArtworkForNotification(url) }
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
            }
        }
        _exoPlayer?.let { player ->
            player.addListener(playbackEndedListener)
            player.repeatMode = _repeatMode.value
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        } ?: run {
            _exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    repeatMode = _repeatMode.value
                    addListener(playbackEndedListener)
                    setMediaItem(MediaItem.fromUri(uri))
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
                        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
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
        _exoPlayer?.release()
        _exoPlayer = null
    }

    companion object {
        const val ACTION_PLAY = "com.shuckler.app.PLAY"
        const val ACTION_PAUSE = "com.shuckler.app.PAUSE"
        const val ACTION_TOGGLE = "com.shuckler.app.TOGGLE"
        const val ACTION_NEXT = "com.shuckler.app.NEXT"
        const val ACTION_PREVIOUS = "com.shuckler.app.PREVIOUS"
        const val ACTION_PLAY_URI = "com.shuckler.app.PLAY_URI"
        const val ACTION_PLAY_WITH_QUEUE = "com.shuckler.app.PLAY_WITH_QUEUE"
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_QUEUE_JSON = "queue_json"
        const val EXTRA_START_INDEX = "start_index"
        const val EXTRA_THUMBNAIL_URL = "thumbnail_url"
        private const val CHANNEL_ID = "shuckler_playback"
        private const val NOTIFICATION_ID = 1
    }
}
