package com.generic.audioplayes.data.analytics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants
import com.generic.audioplayes.data.music.Song

@Entity(
    tableName = Constants.Tables.PLAY_HISTORY_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["location"],
            childColumns = ["songLocation"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songLocation: String,
    val timestamp: Long,
    val playDuration: Long,
)
