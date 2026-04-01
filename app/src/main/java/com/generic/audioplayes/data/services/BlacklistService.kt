package com.generic.audioplayes.data.services

import com.generic.audioplayes.data.daos.AlbumArtistDao
import com.generic.audioplayes.data.daos.AlbumDao
import com.generic.audioplayes.data.daos.ArtistDao
import com.generic.audioplayes.data.daos.BlacklistDao
import com.generic.audioplayes.data.daos.BlacklistedFolderDao
import com.generic.audioplayes.data.daos.ComposerDao
import com.generic.audioplayes.data.daos.GenreDao
import com.generic.audioplayes.data.daos.LyricistDao
import com.generic.audioplayes.data.daos.SongDao
import com.generic.audioplayes.data.music.BlacklistedFolder
import com.generic.audioplayes.data.music.BlacklistedSong
import com.generic.audioplayes.data.music.Song
import kotlinx.coroutines.flow.Flow

interface BlacklistService {
    val blacklistedSongs: Flow<List<BlacklistedSong>>
    val blacklistedFolders: Flow<List<BlacklistedFolder>>

    suspend fun blacklistSongs(songs: List<Song>)
    suspend fun whitelistSongs(blacklistedSongs: List<BlacklistedSong>)

    suspend fun blacklistFolders(folderPaths: List<String>)
    suspend fun whitelistFolders(folders: List<BlacklistedFolder>)
}

class BlacklistServiceImpl(
    private val blacklistDao: BlacklistDao,
    private val blacklistedFolderDao: BlacklistedFolderDao,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val albumArtistDao: AlbumArtistDao,
    private val composerDao: ComposerDao,
    private val lyricistDao: LyricistDao,
    private val genreDao: GenreDao,
): BlacklistService {

    override val blacklistedSongs: Flow<List<BlacklistedSong>>
         = blacklistDao.getBlacklistedSongsFlow()

    override val blacklistedFolders: Flow<List<BlacklistedFolder>>
         = blacklistedFolderDao.getAllFolders()

    override suspend fun blacklistSongs(songs: List<Song>) {
        songs.forEach { song ->
            songDao.deleteSong(song)
            blacklistDao.addSong(
                BlacklistedSong(
                    location = song.location,
                    title = song.title,
                    artist = song.artist,
                )
            )
        }
    }

    override suspend fun whitelistSongs(blacklistedSongs: List<BlacklistedSong>) {
        blacklistedSongs.forEach { blacklistedSong ->
            blacklistDao.deleteBlacklistedSong(blacklistedSong)
        }
    }

    override suspend fun blacklistFolders(folderPaths: List<String>) {
        folderPaths.forEach { folderPath ->
            songDao.deleteSongsWithPathPrefix(folderPath)
            blacklistedFolderDao.insertFolder(BlacklistedFolder(folderPath))
        }
        cleanData()
    }

    private suspend fun cleanData(){
        albumDao.cleanAlbumTable()
        artistDao.cleanArtistTable()
        albumArtistDao.cleanAlbumArtistTable()
        composerDao.cleanComposerTable()
        lyricistDao.cleanLyricistTable()
        genreDao.cleanGenreTable()
    }

    override suspend fun whitelistFolders(folders: List<BlacklistedFolder>) {
        folders.forEach { folder ->
            blacklistedFolderDao.deleteFolder(folder)
        }
    }
}