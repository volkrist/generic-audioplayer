package com.generic.audioplayes.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version alignment: some installs already had the DB at 7; schema matches v6 entities.
 */
val Migration6To7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema change — bump only.
    }
}
