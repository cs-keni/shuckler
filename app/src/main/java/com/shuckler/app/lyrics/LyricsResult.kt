package com.shuckler.app.lyrics

/**
 * Result of a lyrics fetch: success with synced or plain lyrics, or failure/missing.
 */
sealed class LyricsResult {
    /** Synced lyrics (LRC format): list of (timestampMs, line). */
    data class Synced(val lines: List<LyricLine>) : LyricsResult()

    /** Plain lyrics (no timestamps). */
    data class Plain(val text: String) : LyricsResult()

    /** No lyrics found or API error. */
    data object NotFound : LyricsResult()

    /** Fetch in progress (e.g. loading state). */
    data object Loading : LyricsResult()
}

/**
 * A single lyric line with optional timestamp for synchronized display.
 */
data class LyricLine(
    val timestampMs: Long,
    val text: String
)
