package com.shuckler.app.shortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.download.DownloadStatus
import com.shuckler.app.player.MusicPlayerService
import com.shuckler.app.player.QueueItem
import java.io.File

/**
 * Handles app shortcut actions launched from the launcher icon long-press menu.
 */
object AppShortcutHandler {

    const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
    const val ACTION_RESUME = "resume"
    const val ACTION_SHUFFLE = "shuffle"
    const val ACTION_RECENTLY_PLAYED = "recently_played"

    /**
     * Process a shortcut action from the given intent. Call from MainActivity onCreate/onNewIntent.
     * @return true if a shortcut action was handled
     */
    fun handleShortcutIntent(context: Context, intent: Intent?): Boolean {
        val action = intent?.getStringExtra(EXTRA_SHORTCUT_ACTION) ?: return false
        val app = context.applicationContext as? ShucklerApplication ?: return false
        val downloadManager = app.downloadManager

        val completedTracks = downloadManager.downloads.value
            .filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotBlank() }

        when (action) {
            ACTION_RESUME -> {
                if (completedTracks.isNotEmpty()) {
                    val recentlyPlayed = completedTracks
                        .filter { it.lastPlayedMs > 0 }
                        .sortedByDescending { it.lastPlayedMs }
                    val track = recentlyPlayed.firstOrNull() ?: completedTracks.random()
                    val items = listOf(trackToQueueItem(track))
                    context.startForegroundService(
                        Intent(context, MusicPlayerService::class.java).apply {
                            setAction(MusicPlayerService.ACTION_PLAY_WITH_QUEUE)
                            putExtra(MusicPlayerService.EXTRA_QUEUE_JSON, QueueItem.listToJson(items))
                            putExtra(MusicPlayerService.EXTRA_START_INDEX, 0)
                        }
                    )
                }
            }
            ACTION_SHUFFLE -> {
                val shuffleable = downloadManager.filterForShuffle(completedTracks)
                if (shuffleable.isNotEmpty()) {
                    val shuffled = shuffleable.shuffled()
                    val items = shuffled.map { trackToQueueItem(it) }
                    context.startForegroundService(
                        Intent(context, MusicPlayerService::class.java).apply {
                            setAction(MusicPlayerService.ACTION_PLAY_WITH_QUEUE)
                            putExtra(MusicPlayerService.EXTRA_QUEUE_JSON, QueueItem.listToJson(items))
                            putExtra(MusicPlayerService.EXTRA_START_INDEX, 0)
                        }
                    )
                }
            }
            ACTION_RECENTLY_PLAYED -> {
                val recentlyPlayed = completedTracks
                    .filter { it.lastPlayedMs > 0 }
                    .sortedByDescending { it.lastPlayedMs }
                if (recentlyPlayed.isNotEmpty()) {
                    val items = recentlyPlayed.map { trackToQueueItem(it) }
                    context.startForegroundService(
                        Intent(context, MusicPlayerService::class.java).apply {
                            setAction(MusicPlayerService.ACTION_PLAY_WITH_QUEUE)
                            putExtra(MusicPlayerService.EXTRA_QUEUE_JSON, QueueItem.listToJson(items))
                            putExtra(MusicPlayerService.EXTRA_START_INDEX, 0)
                        }
                    )
                } else {
                    // Fallback to shuffle
                    val shuffleable = downloadManager.filterForShuffle(completedTracks)
                    if (shuffleable.isNotEmpty()) {
                        val items = shuffleable.shuffled().map { trackToQueueItem(it) }
                        context.startForegroundService(
                            Intent(context, MusicPlayerService::class.java).apply {
                                setAction(MusicPlayerService.ACTION_PLAY_WITH_QUEUE)
                                putExtra(MusicPlayerService.EXTRA_QUEUE_JSON, QueueItem.listToJson(items))
                                putExtra(MusicPlayerService.EXTRA_START_INDEX, 0)
                            }
                        )
                    }
                }
            }
            else -> return false
        }
        return true
    }

    private fun trackToQueueItem(track: com.shuckler.app.download.DownloadedTrack): QueueItem =
        QueueItem(
            uri = Uri.fromFile(File(track.filePath)).toString(),
            title = track.title,
            artist = track.artist,
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            startMs = track.startMs,
            endMs = track.endMs
        )
}
