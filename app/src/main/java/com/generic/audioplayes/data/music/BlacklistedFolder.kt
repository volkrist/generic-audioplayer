package com.generic.audioplayes.data.music

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.BLACKLISTED_FOLDER_TABLE)
data class BlacklistedFolder(
    @PrimaryKey val path: String,
)
