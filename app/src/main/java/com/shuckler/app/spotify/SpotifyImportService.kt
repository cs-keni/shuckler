package com.shuckler.app.spotify

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shuckler.app.ShucklerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpotifyImportService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var importId: String? = null

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getStringExtra(SpotifyImportManager.EXTRA_IMPORT_ID) ?: return
            (application as? ShucklerApplication)?.spotifyImportManager?.cancelImport(id)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, cancelReceiver,
            IntentFilter(ACTION_CANCEL_IMPORT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        importId = intent?.getStringExtra(SpotifyImportManager.EXTRA_IMPORT_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SpotifyImportManager.NOTIF_IMPORT_PROGRESS,
                buildProgressNotification(null),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SpotifyImportManager.NOTIF_IMPORT_PROGRESS, buildProgressNotification(null))
        }

        val app = application as? ShucklerApplication ?: run { stopSelf(); return START_NOT_STICKY }
        val manager = app.spotifyImportManager

        progressJob = scope.launch {
            manager.progress.collect { progress ->
                if (progress == null) return@collect
                if (progress.importId != importId) return@collect

                updateProgressNotification(progress)

                if (progress.isFinished) {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun buildProgressNotification(progress: ImportProgress?): android.app.Notification {
        val cancelIntent = Intent(ACTION_CANCEL_IMPORT).apply {
            putExtra(SpotifyImportManager.EXTRA_IMPORT_ID, importId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (progress != null) {
            "Rescuing your music… ${progress.terminal} of ${progress.total} done"
        } else {
            "Starting import…"
        }

        return NotificationCompat.Builder(this, SpotifyImportManager.CHANNEL_IMPORT_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Rescuing your music")
            .setContentText(text)
            .setProgress(progress?.total ?: 0, progress?.terminal ?: 0, progress == null)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun updateProgressNotification(progress: ImportProgress) {
        val notification = buildProgressNotification(progress)
        try {
            NotificationManagerCompat.from(this)
                .notify(SpotifyImportManager.NOTIF_IMPORT_PROGRESS, notification)
        } catch (_: SecurityException) {}
    }

    override fun onTimeout(startId: Int) {
        // Android 15+ dataSync 6-hour limit reached — persist state, stop gracefully
        val manager = (application as? ShucklerApplication)?.spotifyImportManager
        importId?.let { id -> manager?.cancelImport(id) }
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        try { unregisterReceiver(cancelReceiver) } catch (_: Exception) {}
        NotificationManagerCompat.from(this).cancel(SpotifyImportManager.NOTIF_IMPORT_PROGRESS)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Static nested receiver class registered in manifest for explicit broadcast targeting
    class CancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handled by the inline cancelReceiver inside the running service
        }
    }

    companion object {
        const val ACTION_CANCEL_IMPORT = "com.shuckler.app.ACTION_CANCEL_IMPORT"

        fun start(context: Context, importId: String) {
            val intent = Intent(context, SpotifyImportService::class.java).apply {
                putExtra(SpotifyImportManager.EXTRA_IMPORT_ID, importId)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
