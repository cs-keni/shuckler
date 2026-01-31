package com.shuckler.app.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages binding to MusicPlayerService and exposes the service for UI control.
 */
class MusicServiceConnection {

    private val _service = MutableStateFlow<MusicPlayerService?>(null)
    val service: StateFlow<MusicPlayerService?> = _service.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as? MusicPlayerService.LocalBinder
            _service.value = musicBinder?.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _service.value = null
        }
    }

    fun bind(context: Context) {
        context.bindService(
            Intent(context, MusicPlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbind(context: Context) {
        context.unbindService(connection)
        _service.value = null
    }
}
