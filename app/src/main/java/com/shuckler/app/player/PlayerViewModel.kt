package com.shuckler.app.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext

    private val _musicPlayer = lazy { MusicPlayer(appContext) }
    val musicPlayer: MusicPlayer get() = _musicPlayer.value

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("How About a Song (Jubilife City)")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow("Pokémon X and Y (OST)")
    val currentTrackArtist: StateFlow<String> = _currentTrackArtist.asStateFlow()

    private var playbackStateJob: Job? = null

    init {
        startPlaybackStateUpdates()
    }

    private fun startPlaybackStateUpdates() {
        playbackStateJob?.cancel()
        playbackStateJob = viewModelScope.launch {
            while (isActive) {
                _isPlaying.value = musicPlayer.isPlaying
                delay(200) // Update UI every 200ms
            }
        }
    }

    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
    }

    fun play() {
        musicPlayer.play()
    }

    fun pause() {
        musicPlayer.pause()
    }

    override fun onCleared() {
        super.onCleared()
        playbackStateJob?.cancel()
        musicPlayer.release()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
                return PlayerViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
