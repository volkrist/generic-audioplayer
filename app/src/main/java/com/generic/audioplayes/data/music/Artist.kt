package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.ARTIST_TABLE)
data class Artist(
    @PrimaryKey val name: String,
)
