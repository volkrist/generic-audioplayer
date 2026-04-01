package com.generic.audioplayes.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds [com.generic.audioplayes.data.music.PlaylistSongCrossRef.position] and backfills stable order.
 */
val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE playlist_song_cross_ref_table ADD COLUMN position INTEGER NOT NULL DEFAULT 0"
        )
        database.query(
            "SELECT playlistId, location FROM playlist_song_cross_ref_table ORDER BY playlistId, location"
        ).use { cursor ->
            var lastPid = -1L
            var pos = 0
            while (cursor.moveToNext()) {
                val pid = cursor.getLong(0)
                val loc = cursor.getString(1)
                if (pid != lastPid) {
                    lastPid = pid
                    pos = 0
                }
                database.execSQL(
                    "UPDATE playlist_song_cross_ref_table SET position = ? WHERE playlistId = ? AND location = ?",
                    arrayOf<Any>(pos, pid, loc)
                )
                pos++
            }
        }
    }
}
