package com.generic.audioplayes.data.music

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.generic.audioplayes.Constants

@Entity(tableName = Constants.Tables.PLAYLIST_TABLE)
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Long,
    val playlistName: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "NULL") val artUri: String? = null,
)

data class PlaylistExceptId(
    val playlistName: String,
    val createdAt: Long,
    val artUri: String? = null,
)
