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
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
                if (uriString != null) {
                    setMediaUri(uriString.toUri(), title, artist)
                    play()
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
        // Single track: no-op for now
    }

    private fun skipToPrevious() {
        exoPlayer.seekTo(0)
    }

    /**
     * Switch playback to a file (e.g. downloaded track). Call before or instead of play().
     */
    fun setMediaUri(uri: Uri, title: String, artist: String) {
        _currentTrackTitle.value = title
        _currentTrackArtist.value = artist
        _exoPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        } ?: run {
            _exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
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
            .build()
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
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        private const val CHANNEL_ID = "shuckler_playback"
        private const val NOTIFICATION_ID = 1
    }
}
