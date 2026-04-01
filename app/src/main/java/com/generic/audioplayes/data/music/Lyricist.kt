package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.LYRICIST_TABLE)
data class Lyricist(
    @PrimaryKey val name: String,
)
