package com.shuckler.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.shuckler.app.MainActivity
import com.shuckler.app.R
import com.shuckler.app.player.MusicPlayerService

class NowPlayingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, null, null, false, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.app_name)
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                @Suppress("DEPRECATION")
                val bitmap = intent.getParcelableExtra<Bitmap>(EXTRA_ARTWORK)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NowPlayingWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (widgetId in widgetIds) {
                    updateWidget(context, appWidgetManager, widgetId, title, artist, isPlaying, bitmap)
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        artwork: Bitmap?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_now_playing)
        views.setTextViewText(R.id.widget_title, title ?: context.getString(R.string.app_name))
        views.setTextViewText(R.id.widget_artist, artist ?: "")
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.mipmap.ic_launcher)
        }

        // Open app when tapping artwork or text area
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPending)

        // Play/pause
        val toggleIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_TOGGLE
        }
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            PendingIntent.getService(
                context, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Previous
        val prevIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PREVIOUS
        }
        views.setOnClickPendingIntent(
            R.id.widget_previous,
            PendingIntent.getService(
                context, 2, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Next
        val nextIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_NEXT
        }
        views.setOnClickPendingIntent(
            R.id.widget_next,
            PendingIntent.getService(
                context, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_UPDATE = "com.shuckler.app.WIDGET_UPDATE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_ARTWORK = "artwork"

        fun updateAllWidgets(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            artwork: Bitmap?
        ) {
            val intent = Intent(context, NowPlayingWidgetProvider::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                artwork?.let { putExtra(EXTRA_ARTWORK, it) }
            }
            context.sendBroadcast(intent)
        }
    }
}
