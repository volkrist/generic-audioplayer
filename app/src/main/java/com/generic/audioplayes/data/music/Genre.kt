package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.GENRE_TABLE)
data class Genre(
    @PrimaryKey val genre: String,
)
