package com.generic.audioplayes.data.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.daos.AlbumArtistDao
import com.generic.audioplayes.data.daos.AlbumDao
import com.generic.audioplayes.data.daos.ArtistDao
import com.generic.audioplayes.data.daos.BlacklistDao
import com.generic.audioplayes.data.daos.BlacklistedFolderDao
import com.generic.audioplayes.data.daos.ComposerDao
import com.generic.audioplayes.data.daos.GenreDao
import com.generic.audioplayes.data.daos.LyricistDao
import com.generic.audioplayes.data.daos.SongDao
import com.generic.audioplayes.formatToDate
import com.generic.audioplayes.toMBfromB
import com.generic.audioplayes.toMS
import com.generic.audioplayes.data.daos.deleteSongsByLocations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.LinkedHashMap
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SongExtractor(
    private val scope: CoroutineScope,
    private val context: Context,
    private val crashReporter: AudioPlayerCrashReporter,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val albumArtistDao: AlbumArtistDao,
    private val composerDao: ComposerDao,
    private val lyricistDao: LyricistDao,
    private val genreDao: GenreDao,
    private val blacklistDao: BlacklistDao,
    private val blacklistedFolderDao: BlacklistedFolderDao,
) {

    init {
        scope.launch {
            delay(1500)
            cleanData()
        }
    }

    fun cleanData() {
        scope.launch {
            val jobs = mutableListOf<Job>()
            songDao.getSongs().forEach {
                try {
                    if(!File(it.location).exists()){
                        jobs += launch { songDao.deleteSong(it) }
                    }
                } catch (_: Exception){

                }
            }
            jobs.joinAll()
            jobs.clear()
            cleanupOrphanMetadataTables()
        }
    }

    private suspend fun cleanupOrphanMetadataTables() {
        albumDao.cleanAlbumTable()
        artistDao.cleanArtistTable()
        albumArtistDao.cleanAlbumArtistTable()
        composerDao.cleanComposerTable()
        lyricistDao.cleanLyricistTable()
        genreDao.cleanGenreTable()
    }

    private fun checkReadStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
    )

    fun resolveSong(location: String): Song? {
        if (!checkReadStoragePermission()) return null
        val selection = MediaStore.Audio.Media.DATA + " LIKE ?"
        val selectionArgs = arrayOf(location)
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DATE_ADDED,
            null
        ) ?: return null
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val dateAddedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val songIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        var resSong: Song? = null
        cursor.moveToFirst()
        try {
            val songPath = cursor.getString(dataIndex)
            val songFile = File(songPath)
            if (!songFile.exists()) throw FileNotFoundException()
            val size = cursor.getString(sizeIndex)
            val dateAddedSec = cursor.getLong(dateAddedIndex).coerceAtLeast(0L)
            val modifiedDate = cursor.getString(dateModifiedIndex)
            val dateModifiedSec = cursor.getLong(dateModifiedIndex).coerceAtLeast(0L)
            val songId = cursor.getLong(songIdIndex)
            val title = cursor.getString(titleIndex).trim()
            val album = cursor.getString(albumIndex).trim()
            resSong = getSong(
                path = songPath,
                size = size,
                dateAddedSec = dateAddedSec,
                modifiedDate = modifiedDate,
                dateModifiedSec = dateModifiedSec,
                songId = songId,
                title = title,
                album = album,
            )
        } catch (_: Exception){

        }
        cursor.close()
        return resSong
    }

    suspend fun extract(folderPath: String? = null): List<Song> {
        if (!checkReadStoragePermission()) return emptyList()
        val selection = MediaStore.Audio.Media.DATA + " LIKE ?"
        val selectionArgs = folderPath?.let {
            arrayOf("$it%")
        }
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DATE_ADDED,
            null
        ) ?: return emptyList()
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val dateAddedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val songIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val dSongs = ArrayList<Deferred<Song?>>()
        val folderRoot = folderPath?.let { fp ->
            try {
                File(fp).canonicalFile.absolutePath.trimEnd('/')
            } catch (_: Exception) {
                fp.trimEnd('/')
            }
        }
        cursor.moveToFirst()
        do {
            try {
                val songPath = cursor.getString(dataIndex)
                val songFile = File(songPath)
                if (!songFile.exists()) throw FileNotFoundException()
                if (folderRoot != null) {
                    val parentCanon = songFile.parentFile?.canonicalFile?.absolutePath?.trimEnd('/')
                    if (parentCanon != folderRoot) throw Exception()
                }
                val size = cursor.getString(sizeIndex)
                val dateAddedSec = cursor.getLong(dateAddedIndex).coerceAtLeast(0L)
                val modifiedDate = cursor.getString(dateModifiedIndex)
                val dateModifiedSec = cursor.getLong(dateModifiedIndex).coerceAtLeast(0L)
                val songId = cursor.getLong(songIdIndex)
                val title = cursor.getString(titleIndex).trim()
                val album = cursor.getString(albumIndex).trim()
                dSongs.add(scope.async {
                    getSong(
                        path = songPath,
                        size = size,
                        dateAddedSec = dateAddedSec,
                        modifiedDate = modifiedDate,
                        dateModifiedSec = dateModifiedSec,
                        songId = songId,
                        title = title,
                        album = album,
                    )
                })
            } catch (_: Exception){

            }
        } while (cursor.moveToNext())
        val songs = dSongs.awaitAll().filterNotNull()
        cursor.close()
        return songs
    }

    /**
     * All indexed audio files under [folderPath] (recursive). Path rules match [countAudioTracksUnderFolderPath].
     */
    suspend fun extractAllSongsUnderFolderRecursive(folderPath: String): List<Song> {
        if (!checkReadStoragePermission()) return emptyList()
        val root = try {
            File(folderPath).canonicalFile.absolutePath.trimEnd('/')
        } catch (_: Exception) {
            folderPath.trimEnd('/')
        }
        val rootWithSlash = "$root/"
        val selection = MediaStore.Audio.Media.DATA + " LIKE ?"
        val selectionArgs = arrayOf("$root%")
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DATE_ADDED,
            null,
        ) ?: return emptyList()
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val dateAddedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val songIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val dSongs = ArrayList<Deferred<Song?>>()
        cursor.moveToFirst()
        do {
            try {
                val songPath = cursor.getString(dataIndex)
                val songFile = File(songPath)
                if (!songFile.exists()) throw FileNotFoundException()
                val canon = songFile.canonicalPath
                if (!canon.startsWith(rootWithSlash) && canon != root) throw Exception()
                val size = cursor.getString(sizeIndex)
                val dateAddedSec = cursor.getLong(dateAddedIndex).coerceAtLeast(0L)
                val modifiedDate = cursor.getString(dateModifiedIndex)
                val dateModifiedSec = cursor.getLong(dateModifiedIndex).coerceAtLeast(0L)
                val songId = cursor.getLong(songIdIndex)
                val title = cursor.getString(titleIndex).trim()
                val album = cursor.getString(albumIndex).trim()
                dSongs.add(scope.async {
                    getSong(
                        path = songPath,
                        size = size,
                        dateAddedSec = dateAddedSec,
                        modifiedDate = modifiedDate,
                        dateModifiedSec = dateModifiedSec,
                        songId = songId,
                        title = title,
                        album = album,
                    )
                })
            } catch (_: Exception) {
            }
        } while (cursor.moveToNext())
        val songs = dSongs.awaitAll().filterNotNull()
        cursor.close()
        return songs
    }

    fun extractMini(folderPath: String? = null): List<MiniSong> {
        if (!checkReadStoragePermission()) return emptyList()
        val projectionForMini = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
        )
        val selection = MediaStore.Audio.Media.DATA + " LIKE ?"
        val selectionArgs = folderPath?.let {
            arrayOf("$it%")
        }
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projectionForMini,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.DATE_ADDED,
            null
        ) ?: return emptyList()
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val songIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val songs = ArrayList<MiniSong>()
        val folderRoot = folderPath?.let { fp ->
            try {
                File(fp).canonicalFile.absolutePath.trimEnd('/')
            } catch (_: Exception) {
                fp.trimEnd('/')
            }
        }
        cursor.moveToFirst()
        do {
            try {
                val songPath = cursor.getString(dataIndex)
                val songFile = File(songPath)
                if (!songFile.exists()) throw FileNotFoundException()
                if (folderRoot != null) {
                    val parentCanon = songFile.parentFile?.canonicalFile?.absolutePath?.trimEnd('/')
                    if (parentCanon != folderRoot) throw Exception()
                }
                songs.add(
                    MiniSong(
                        location = songPath,
                        title = cursor.getString(titleIndex).trim(),
                        artUri = "content://media/external/audio/media/${cursor.getLong(songIdIndex)}/albumart",
                        artist = cursor.getString(artistIndex)
                    )
                )
            } catch (_: Exception){

            }
        } while (cursor.moveToNext())
        cursor.close()
        return songs
    }

    /**
     * Total audio files under [folderPath] (recursive), using canonical paths so counts match
     * real storage even when [File.absolutePath] and MediaStore [DATA] differ (e.g. symlinks).
     * Used for folder list subtitles; explorer content still uses [extractMini] (direct children).
     */
    fun countAudioTracksUnderFolderPath(folderPath: String): Int {
        if (!checkReadStoragePermission()) return 0
        val root = try {
            File(folderPath).canonicalFile.absolutePath.trimEnd('/')
        } catch (_: Exception) {
            folderPath.trimEnd('/')
        }
        val rootWithSlash = "$root/"
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = MediaStore.Audio.Media.DATA + " LIKE ?"
        val selectionArgs = arrayOf("$root%")
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        ) ?: return 0
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        if (dataIndex < 0) {
            cursor.close()
            return 0
        }
        var count = 0
        try {
            while (cursor.moveToNext()) {
                val songPath = cursor.getString(dataIndex) ?: continue
                try {
                    val songFile = File(songPath)
                    if (!songFile.exists()) continue
                    val canon = songFile.canonicalPath
                    if (canon.startsWith(rootWithSlash)) {
                        count++
                    }
                } catch (_: Exception) {
                }
            }
        } finally {
            cursor.close()
        }
        return count
    }

    private val _scanStatus = Channel<ScanStatus>(onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val scanStatus = _scanStatus.receiveAsFlow()

    private val scanInProgress = AtomicBoolean(false)

    private suspend fun persistExtractedLibrary(songs: List<Song>, albums: List<Album>) {
        if (songs.isEmpty()) {
            cleanupOrphanMetadataTables()
            return
        }
        val insertJobs = listOf(
            scope.launch {
                val artists = songs.map { it.artist }.toSet().map { Artist(it) }
                artistDao.insertAllArtists(artists)
            },
            scope.launch {
                val albumArtists = songs.map { it.albumArtist }.toSet().map { AlbumArtist(it) }
                albumArtistDao.insertAllAlbumArtists(albumArtists)
            },
            scope.launch {
                val lyricists = songs.map { it.lyricist }.toSet().map { Lyricist(it) }
                lyricistDao.insertAllLyricists(lyricists)
            },
            scope.launch {
                val composers = songs.map { it.composer }.toSet().map { Composer(it) }
                composerDao.insertAllComposers(composers)
            },
            scope.launch {
                val genres = songs.map { it.genre }.toSet().map { Genre(it) }
                genreDao.insertAllGenres(genres)
            }
        )
        albumDao.insertAllAlbums(albums)
        insertJobs.joinAll()
        songDao.insertOrReplaceSongs(mergePersistedUserFields(songs))
        cleanupOrphanMetadataTables()
    }

    /**
     * [insertOrReplaceSongs] replaces full rows; MediaStore scans always set [Song.favourite] to false.
     * Preserve user-edited fields from the DB so favourites and play stats survive library sync.
     */
    private suspend fun mergePersistedUserFields(songs: List<Song>): List<Song> {
        if (songs.isEmpty()) return songs
        val existingByLocation = LinkedHashMap<String, Song>(songs.size)
        songs.map { it.location }.chunked(450).forEach { chunk ->
            songDao.getSongsFromLocations(chunk).forEach { existing ->
                existingByLocation[existing.location] = existing
            }
        }
        return songs.map { fresh ->
            val old = existingByLocation[fresh.location] ?: return@map fresh
            fresh.copy(
                favourite = old.favourite,
                playCount = old.playCount,
                lastPlayed = old.lastPlayed,
            )
        }
    }

    fun scanForMusic() {
        if (!checkReadStoragePermission()) return
        scope.launch {
            if (!scanInProgress.compareAndSet(false, true)) return@launch
            try {
                _scanStatus.send(ScanStatus.ScanStarted)
                val blacklistedSongLocations = blacklistDao
                    .getBlacklistedSongs()
                    .map { it.location }
                    .toHashSet()
                val blacklistedFolderPaths = blacklistedFolderDao
                    .getAllFolders()
                    .first()
                    .map { it.path }
                    .toHashSet()
                val (songs, albums) = extract(
                    blacklistedSongLocations,
                    blacklistedFolderPaths,
                    statusListener = { parsed, total ->
                        _scanStatus.trySend(ScanStatus.ScanProgress(parsed, total))
                    }
                )
                persistExtractedLibrary(songs, albums)
                _scanStatus.send(ScanStatus.ScanComplete)
            } catch (e: Exception) {
                crashReporter.logException(e)
                _scanStatus.trySend(ScanStatus.ScanNotRunning)
            } finally {
                scanInProgress.set(false)
            }
        }
    }

    private suspend fun extract(
        blacklistedSongLocations: HashSet<String>,
        blacklistedFolderPaths: HashSet<String>,
        statusListener: ((parsed: Int, total: Int) -> Unit)? = null,
        pathsFilter: Set<String>? = null
    ): Pair<List<Song>,List<Album>>  {
        if (pathsFilter != null && pathsFilter.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }
        val selection = StringBuilder()
        val selectionArgs = arrayListOf<String>()
        selection.append(MediaStore.Audio.Media.IS_MUSIC + " != 0 ")
        blacklistedFolderPaths.forEach { path ->
            selection.append(" AND NOT ")
                .append(MediaStore.Audio.Media.DATA)
                .append(" LIKE ?")
            selectionArgs.add("$path%")
        }
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection.toString(),
            selectionArgs.toTypedArray(),
            MediaStore.Audio.Media.DATE_ADDED,
            null
        ) ?: return Pair(emptyList(), emptyList())
        val songCover = Uri.parse("content://media/external/audio/albumart")
        val albumArtMap = TreeMap<String,Long>()
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val dateAddedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val songIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val dSongs = ArrayList<Deferred<Song?>>()
        cursor.moveToFirst()
        do {
            try {
                val songPath = cursor.getString(dataIndex)
                val songFile = File(songPath)
                if (!songFile.exists()) throw FileNotFoundException()
                if (blacklistedSongLocations.contains(songFile.path)) continue
                if (pathsFilter != null && !pathsFilter.contains(songPath)) continue
                val size = cursor.getString(sizeIndex)
                val dateAddedSec = cursor.getLong(dateAddedIndex).coerceAtLeast(0L)
                val modifiedDate = cursor.getString(dateModifiedIndex)
                val dateModifiedSec = cursor.getLong(dateModifiedIndex).coerceAtLeast(0L)
                val songId = cursor.getLong(songIdIndex)
                val title = cursor.getString(titleIndex).trim()
                val album = cursor.getString(albumIndex).trim()
                albumArtMap[album] = cursor.getLong(albumIdIndex)
                dSongs.add(
                    scope.async {
                        getSong(
                            path = songPath,
                            size = size,
                            dateAddedSec = dateAddedSec,
                            modifiedDate = modifiedDate,
                            dateModifiedSec = dateModifiedSec,
                            songId = songId,
                            title = title,
                            album = album,
                        )
                    }
                )
            } catch (_: Exception){

            }
        } while (cursor.moveToNext())
        val totalForProgress = dSongs.size
        val parsed = AtomicInteger(0)
        dSongs.forEach { deferred ->
            deferred.invokeOnCompletion {
                statusListener?.invoke(parsed.incrementAndGet(), totalForProgress)
            }
        }
        val songs = dSongs.awaitAll().filterNotNull()
        cursor.close()
        val albums = albumArtMap.map { (t, u) -> Album(t, ContentUris.withAppendedId(songCover, u).toString()) }
        return Pair(songs,albums)
    }

    private suspend fun queryMediaStoreFingerprints(
        blacklistedSongLocations: HashSet<String>,
        blacklistedFolderPaths: HashSet<String>,
    ): Map<String, Long> {
        val selection = StringBuilder()
        val selectionArgs = arrayListOf<String>()
        selection.append(MediaStore.Audio.Media.IS_MUSIC + " != 0 ")
        blacklistedFolderPaths.forEach { path ->
            selection.append(" AND NOT ")
                .append(MediaStore.Audio.Media.DATA)
                .append(" LIKE ?")
            selectionArgs.add("$path%")
        }
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DATE_MODIFIED),
            selection.toString(),
            selectionArgs.toTypedArray(),
            MediaStore.Audio.Media.DATE_ADDED,
            null
        ) ?: return emptyMap()
        val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val result = LinkedHashMap<String, Long>()
        cursor.moveToFirst()
        do {
            try {
                val songPath = cursor.getString(dataIndex)
                val songFile = File(songPath)
                if (!songFile.exists()) throw FileNotFoundException()
                if (blacklistedSongLocations.contains(songFile.path)) continue
                result[songPath] = cursor.getLong(dateModifiedIndex).coerceAtLeast(0L)
            } catch (_: Exception) { }
        } while (cursor.moveToNext())
        cursor.close()
        return result
    }

    suspend fun hasCachedLibrary(): Boolean =
        songDao.getSongCount() > 0

    suspend fun syncLibraryIncremental() {
        if (!checkReadStoragePermission()) return
        val blacklistedSongLocations = blacklistDao.getBlacklistedSongs().map { it.location }.toHashSet()
        val blacklistedFolderPaths = blacklistedFolderDao.getAllFolders().first().map { it.path }.toHashSet()

        val dbMap = songDao.getSongFingerprints().associate { it.location to it.dateModifiedSec }

        if (dbMap.isEmpty()) {
            val (songs, albums) = extract(
                blacklistedSongLocations,
                blacklistedFolderPaths,
                statusListener = null,
                pathsFilter = null,
            )
            persistExtractedLibrary(songs, albums)
            return
        }

        val mediaFingerprints = queryMediaStoreFingerprints(blacklistedSongLocations, blacklistedFolderPaths)

        val toDelete = dbMap.keys - mediaFingerprints.keys
        songDao.deleteSongsByLocations(toDelete)

        val toProcess = mediaFingerprints.filter { (path, sec) ->
            dbMap[path] != sec
        }
        if (toProcess.isEmpty()) {
            cleanupOrphanMetadataTables()
            return
        }

        val (songs, albums) = extract(
            blacklistedSongLocations,
            blacklistedFolderPaths,
            statusListener = null,
            pathsFilter = toProcess.keys.toHashSet(),
        )
        persistExtractedLibrary(songs, albums)
    }

    companion object {
        private const val UNKNOWN = "Unknown"
    }

    private fun getSong(
        path: String,
        size: String,
        dateAddedSec: Long,
        modifiedDate: String,
        dateModifiedSec: Long,
        songId: Long,
        title: String,
        album: String,
    ): Song? {
        val extractor = MediaMetadataRetriever()
        var result: Song? = null
        try {
            extractor.setDataSource(path)
            val durationMillis = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val sampleRate = if (Build.VERSION.SDK_INT >= 31){
                extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toFloatOrNull() ?: 0f
            } else 0f
            val bitsPerSample = if (Build.VERSION.SDK_INT >= 31){
                extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull() ?: 0
            } else 0
            val song = Song(
                location = path,
                title = title,
                album = album,
                size = size.toFloat().toMBfromB(),
                addedDate = (dateAddedSec * 1000L).formatToDate(),
                modifiedDate = modifiedDate.toLong().formatToDate(),
                dateModifiedSec = dateModifiedSec,
                dateAddedSec = dateAddedSec,
                artist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim() ?: UNKNOWN,
                albumArtist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim() ?: UNKNOWN,
                composer = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.trim() ?: UNKNOWN,
                genre = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim() ?: UNKNOWN,
                lyricist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)?.trim() ?: UNKNOWN,
                year = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0,
                comment = null,
                durationMillis = durationMillis,
                durationFormatted = durationMillis.toMS(),
                bitrate = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toFloatOrNull() ?: 0f,
                sampleRate = sampleRate,
                bitsPerSample = bitsPerSample,
                mimeType = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                favourite = false,
                artUri = "content://media/external/audio/media/$songId/albumart"
            )
            result = song
        } catch (e: Exception) {
            crashReporter.logException(e)
            result = null
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {  }
        }
        return result
    }


}