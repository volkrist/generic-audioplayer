package com.github.pakka_papad.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds [com.github.pakka_papad.data.music.Song.dateAddedSec] for "Recently added" smart playlist ordering.
 */
val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE song_table ADD COLUMN dateAddedSec INTEGER NOT NULL DEFAULT 0"
        )
    }
}
