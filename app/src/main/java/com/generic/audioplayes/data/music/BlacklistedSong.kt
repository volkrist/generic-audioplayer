package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.BLACKLIST_TABLE)
data class BlacklistedSong(
    @PrimaryKey val location: String,
    val title: String,
    val artist: String,
)
