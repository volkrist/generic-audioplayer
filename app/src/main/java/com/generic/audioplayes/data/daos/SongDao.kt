package com.generic.audioplayes.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.generic.audioplayes.Constants
import com.generic.audioplayes.data.music.AlbumArtistWithSongCount
import com.generic.audioplayes.data.music.ArtistWithSongCount
import com.generic.audioplayes.data.music.ComposerWithSongCount
import com.generic.audioplayes.data.music.GenreWithSongCount
import com.generic.audioplayes.data.music.LyricistWithSongCount
import com.generic.audioplayes.data.music.Song
import kotlinx.coroutines.flow.Flow

data class SongFingerprint(
    val location: String,
    val dateModifiedSec: Long,
)

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllSongs(data: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceSongs(data: List<Song>)

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE}")
    suspend fun getSongs(): List<Song>

    @Query("SELECT COUNT(*) FROM ${Constants.Tables.SONG_TABLE}")
    suspend fun getSongCount(): Int

    @Query("SELECT location, dateModifiedSec FROM ${Constants.Tables.SONG_TABLE}")
    suspend fun getSongFingerprints(): List<SongFingerprint>

    @Query("DELETE FROM ${Constants.Tables.SONG_TABLE} WHERE location IN (:locations)")
    suspend fun deleteSongsByLocationsChunk(locations: List<String>)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("DELETE FROM ${Constants.Tables.SONG_TABLE}")
    suspend fun deleteAllSongs()

    @Transaction
    @Query("DELETE FROM ${Constants.Tables.SONG_TABLE} WHERE location LIKE :prefix || '%'")
    suspend fun deleteSongsWithPathPrefix(prefix: String)

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE title LIKE '%' || :query || '%' OR " +
            "artist LIKE '%' || :query || '%' OR " +
            "albumArtist LIKE '%' || :query || '%' OR " +
            "composer LIKE '%' || :query || '%' OR " +
            "genre LIKE '%' || :query || '%' OR " +
            "lyricist LIKE '%' || :query || '%'")
    suspend fun searchSongs(query: String): List<Song>

    @Query("SELECT artist as name, COUNT(*) as count FROM ${Constants.Tables.SONG_TABLE} GROUP BY " +
            "${Constants.Tables.SONG_TABLE}.artist")
    fun getAllArtistsWithSongCount(): Flow<List<ArtistWithSongCount>>

    @Query("SELECT albumArtist as name, COUNT(*) as count FROM ${Constants.Tables.SONG_TABLE} GROUP BY " +
            "${Constants.Tables.SONG_TABLE}.albumArtist")
    fun getAllAlbumArtistsWithSongCount(): Flow<List<AlbumArtistWithSongCount>>


    @Query("SELECT composer as name, COUNT(*) as count FROM ${Constants.Tables.SONG_TABLE} GROUP BY " +
            "${Constants.Tables.SONG_TABLE}.composer")
    fun getAllComposersWithSongCount(): Flow<List<ComposerWithSongCount>>

    @Query("SELECT lyricist as name, COUNT(*) as count FROM ${Constants.Tables.SONG_TABLE} GROUP BY " +
            "${Constants.Tables.SONG_TABLE}.lyricist")
    fun getAllLyricistsWithSongCount(): Flow<List<LyricistWithSongCount>>

    @Query("SELECT genre AS genreName, COUNT(*) AS count FROM ${Constants.Tables.SONG_TABLE} GROUP BY " +
            "${Constants.Tables.SONG_TABLE}.genre")
    fun getAllGenresWithSongCount(): Flow<List<GenreWithSongCount>>

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE favourite = 1")
    fun getAllFavourites(): Flow<List<Song>>

    /** All library tracks ordered by date added (newest first). */
    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} ORDER BY dateAddedSec DESC, title COLLATE NOCASE ASC",
    )
    fun getRecentlyAddedSongs(): Flow<List<Song>>

    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE lastPlayed IS NOT NULL " +
            "ORDER BY lastPlayed DESC",
    )
    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    /** Tracks with at least one completed play session; ordered by play count then recency. */
    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE playCount > 0 ORDER BY playCount DESC, " +
            "CASE WHEN lastPlayed IS NULL THEN 1 ELSE 0 END, lastPlayed DESC, title COLLATE NOCASE ASC",
    )
    fun getTopTracks(): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM ${Constants.Tables.SONG_TABLE} WHERE favourite = 1")
    fun observeFavouriteSongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ${Constants.Tables.SONG_TABLE}")
    fun observeLibrarySongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ${Constants.Tables.SONG_TABLE} WHERE lastPlayed IS NOT NULL")
    fun observeRecentlyPlayedSongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ${Constants.Tables.SONG_TABLE} WHERE playCount > 0")
    fun observeSongsWithPlayCount(): Flow<Int>

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE location IN (:locations)")
    suspend fun getSongsFromLocations(locations: List<String>): List<Song>

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE location = :location LIMIT 1")
    suspend fun getSongByLocation(location: String): Song?

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE album = :albumName")
    suspend fun getSongsByAlbumName(albumName: String): List<Song>

    @Query("SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE artist = :artistName")
    suspend fun getSongsByArtistName(artistName: String): List<Song>

    /**
     * All songs whose file path equals the folder or is under it (prefix + '/').
     */
    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE location = :folderPath OR " +
            "location LIKE :folderPath || '/%'"
    )
    suspend fun getSongsUnderFolderPath(folderPath: String): List<Song>

    /** Title-only search for library; excludes dictaphone recordings folder. */
    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE title LIKE '%' || :query || '%' " +
            "AND location NOT LIKE '%AudioPlayer/Recordings%' LIMIT 80",
    )
    suspend fun searchSongsByTitleExcludingDictaphone(query: String): List<Song>

    /** Paths that may contain matching folder segments (bounded for performance). */
    @Query(
        "SELECT location FROM ${Constants.Tables.SONG_TABLE} WHERE location LIKE '%' || :query || '%' LIMIT 600",
    )
    suspend fun searchLocationsContainingForFolders(query: String): List<String>

    @Query(
        "SELECT * FROM ${Constants.Tables.SONG_TABLE} WHERE title LIKE '%' || :query || '%' " +
            "AND location LIKE '%AudioPlayer/Recordings%' LIMIT 50",
    )
    suspend fun searchDictaphoneRecordingsByTitle(query: String): List<Song>
}

/** SQLite variable limit (~999); chunk deletes to stay safe. */
suspend fun SongDao.deleteSongsByLocations(locations: Collection<String>) {
    if (locations.isEmpty()) return
    locations.chunked(450).forEach { chunk ->
        deleteSongsByLocationsChunk(chunk)
    }
}