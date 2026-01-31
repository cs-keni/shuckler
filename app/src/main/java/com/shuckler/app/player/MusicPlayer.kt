package com.shuckler.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shuckler.app.R

/**
 * Basic music player using Media3 ExoPlayer.
 * Handles play/pause and audio focus automatically.
 */
class MusicPlayer(context: Context) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(audioAttributes, true)  // true = handle audio focus
        .setHandleAudioBecomingNoisy(true)  // Pause when headphones unplugged
        .build()

    val player: Player get() = exoPlayer

    var isPlaying: Boolean
        get() = exoPlayer.isPlaying
        private set(_) {}

    init {
        // Load the test song from res/raw
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.test_song}")
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun release() {
        exoPlayer.release()
    }
}
