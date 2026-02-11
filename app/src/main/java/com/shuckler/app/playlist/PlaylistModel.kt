package com.shuckler.app.playlist

/**
 * A user-created playlist.
 */
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverImagePath: String? = null
)

/**
 * Entry linking a track to a playlist with position for ordering.
 */
data class PlaylistEntry(
    val playlistId: String,
    val trackId: String,
    val position: Int
)
