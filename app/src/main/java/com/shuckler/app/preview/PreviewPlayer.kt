package com.shuckler.app.preview

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight player for previewing YouTube audio (30–60 seconds) without saving.
 * Runs in a separate ExoPlayer instance; stops when user leaves Search or after max duration.
 */
object PreviewPlayer {

    private const val PREVIEW_DURATION_MS = 60_000L

    private var _player: ExoPlayer? = null
    private var contextRef: Context? = null
    private var progressJob: Job? = null

    /** YouTube video URL we're previewing (for UI matching), not the stream URL. */
    private val _previewingVideoUrl = MutableStateFlow<String?>(null)
    val previewingVideoUrl: StateFlow<String?> = _previewingVideoUrl.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    val previewDurationMs: Long get() = PREVIEW_DURATION_MS

    val isPlaying: Boolean get() = _player?.isPlaying == true

    /**
     * Start preview playback. Stops automatically after [PREVIEW_DURATION_MS].
     * Call [stop] when user navigates away or taps Stop.
     * @param videoUrl YouTube video URL (for UI matching with search results)
     * @param streamUrl Direct audio stream URL from getAudioStreamUrl
     */
    fun play(context: Context, videoUrl: String, streamUrl: String) {
        stop()
        contextRef = context.applicationContext
        val ctx = contextRef ?: return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        _player = ExoPlayer.Builder(ctx)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                play()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) stop()
                    }
                })
            }
        _previewingVideoUrl.value = videoUrl
        _positionMs.value = 0L
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(300)
                val pos = _player?.currentPosition ?: 0L
                _positionMs.value = pos
                if (pos >= PREVIEW_DURATION_MS) {
                    stop()
                    break
                }
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        _player?.release()
        _player = null
        contextRef = null
        _previewingVideoUrl.value = null
        _positionMs.value = 0L
    }

    fun isPreviewing(videoUrl: String): Boolean = _previewingVideoUrl.value == videoUrl
}
