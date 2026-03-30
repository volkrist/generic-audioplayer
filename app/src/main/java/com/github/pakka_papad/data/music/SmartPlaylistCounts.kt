package com.github.pakka_papad.data.music

/**
 * Live counts for system smart playlists (favourites, recently added, etc.).
 */
data class SmartPlaylistCounts(
    val favourites: Int,
    val recentlyAdded: Int,
    val recentlyPlayed: Int,
    val topTracks: Int,
)
