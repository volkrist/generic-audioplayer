package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.ALBUM_ARTIST_TABLE)
data class AlbumArtist(
    @PrimaryKey val name: String
)
