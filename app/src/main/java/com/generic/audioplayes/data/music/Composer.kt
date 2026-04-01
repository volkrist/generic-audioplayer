package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.COMPOSER_TABLE)
data class Composer(
    @PrimaryKey val name: String,
)
