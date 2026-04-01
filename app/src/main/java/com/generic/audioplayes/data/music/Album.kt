package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.ALBUM_TABLE)
data class Album(
    @PrimaryKey val name: String,
    val albumArtUri: String? = null,
)
