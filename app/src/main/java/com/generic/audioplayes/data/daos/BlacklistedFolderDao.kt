package com.generic.audioplayes.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.generic.audioplayes.Constants
import com.generic.audioplayes.data.music.BlacklistedFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistedFolderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: BlacklistedFolder)

    @Query("SELECT * FROM ${Constants.Tables.BLACKLISTED_FOLDER_TABLE}")
    fun getAllFolders(): Flow<List<BlacklistedFolder>>

    @Delete
    suspend fun deleteFolder(folder: BlacklistedFolder)

}