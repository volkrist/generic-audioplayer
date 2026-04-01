package com.generic.audioplayes.data.music

import androidx.room.Embedded
import androidx.room.Relation

data class AlbumArtistWithSongs(
    @Embedded
    val albumArtist: AlbumArtist,
    @Relation(
        parentColumn = "name",
        entityColumn = "albumArtist"
    )
    val songs: List<Song>,
)
