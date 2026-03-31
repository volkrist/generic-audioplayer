package com.github.pakka_papad.data.services

import com.github.pakka_papad.data.daos.AlbumArtistDao
import com.github.pakka_papad.data.daos.AlbumDao
import com.github.pakka_papad.data.daos.ArtistDao
import com.github.pakka_papad.data.daos.ComposerDao
import com.github.pakka_papad.data.daos.GenreDao
import com.github.pakka_papad.data.daos.LyricistDao
import com.github.pakka_papad.data.daos.SongDao
import com.github.pakka_papad.data.music.Album
import com.github.pakka_papad.data.music.AlbumArtistWithSongCount
import com.github.pakka_papad.data.music.AlbumArtistWithSongs
import com.github.pakka_papad.data.music.AlbumWithSongs
import com.github.pakka_papad.data.music.ArtistWithSongCount
import com.github.pakka_papad.data.music.ArtistWithSongs
import com.github.pakka_papad.data.music.ComposerWithSongCount
import com.github.pakka_papad.data.music.ComposerWithSongs
import com.github.pakka_papad.data.music.GenreWithSongCount
import com.github.pakka_papad.data.music.GenreWithSongs
import com.github.pakka_papad.data.music.LyricistWithSongCount
import com.github.pakka_papad.data.music.LyricistWithSongs
import com.github.pakka_papad.data.music.SmartPlaylistCounts
import com.github.pakka_papad.data.music.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface SongService {
    val songs: Flow<List<Song>>
    val albums: Flow<List<Album>>
    val artists: Flow<List<ArtistWithSongCount>>
    val albumArtists: Flow<List<AlbumArtistWithSongCount>>
    val composers: Flow<List<ComposerWithSongCount>>
    val lyricists: Flow<List<LyricistWithSongCount>>
    val genres: Flow<List<GenreWithSongCount>>

    /** Aggregate counts for smart playlist cards (updates with library / playback). */
    val smartPlaylistCounts: Flow<SmartPlaylistCounts>

    fun getAlbumWithSongsByName(albumName: String): Flow<AlbumWithSongs?>
    fun getArtistWithSongsByName(artistName: String): Flow<ArtistWithSongs?>
    fun getAlbumArtistWithSongsByName(albumArtistName: String): Flow<AlbumArtistWithSongs?>
    fun getComposerWithSongsByName(composerName: String): Flow<ComposerWithSongs?>
    fun getLyricistWithSongsByName(lyricistName: String): Flow<LyricistWithSongs?>
    fun getGenreWithSongsByName(genre: String): Flow<GenreWithSongs?>

    fun getFavouriteSongs(): Flow<List<Song>>

    fun getRecentlyAddedSongs(): Flow<List<Song>>

    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    fun getTopTracks(): Flow<List<Song>>

    suspend fun updateSong(song: Song)
    suspend fun getSongsFromLocations(locations: List<String>): List<Song>

    suspend fun getSongsByAlbumName(albumName: String): List<Song>

    suspend fun getSongsByArtistName(artistName: String): List<Song>

    suspend fun getSongsUnderFolderPath(folderPath: String): List<Song>

    /** Removes the song row and rebuilds aggregate tables (same cleanup as blacklist). */
    suspend fun removeSongFromLibraryMetadata(song: Song)
}

class SongServiceImpl(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val albumArtistDao: AlbumArtistDao,
    private val composerDao: ComposerDao,
    private val lyricistDao: LyricistDao,
    private val genreDao: GenreDao,
) :SongService {
    override val smartPlaylistCounts: Flow<SmartPlaylistCounts> = combine(
        songDao.observeFavouriteSongCount(),
        songDao.observeLibrarySongCount(),
        songDao.observeRecentlyPlayedSongCount(),
        songDao.observeSongsWithPlayCount(),
    ) { fav, lib, recent, top ->
        SmartPlaylistCounts(
            favourites = fav,
            recentlyAdded = lib,
            recentlyPlayed = recent,
            topTracks = top,
        )
    }

    override val songs: Flow<List<Song>>
        = songDao.getAllSongs()

    override val albums: Flow<List<Album>>
        = albumDao.getAllAlbums()

    override val artists: Flow<List<ArtistWithSongCount>>
        = songDao.getAllArtistsWithSongCount()

    override val albumArtists: Flow<List<AlbumArtistWithSongCount>>
        = songDao.getAllAlbumArtistsWithSongCount()

    override val composers: Flow<List<ComposerWithSongCount>>
        = songDao.getAllComposersWithSongCount()

    override val lyricists: Flow<List<LyricistWithSongCount>>
        = songDao.getAllLyricistsWithSongCount()

    override val genres: Flow<List<GenreWithSongCount>>
        = songDao.getAllGenresWithSongCount()

    override fun getAlbumWithSongsByName(albumName: String): Flow<AlbumWithSongs?> {
        return albumDao.getAlbumWithSongsByName(albumName)
    }

    override fun getArtistWithSongsByName(artistName: String): Flow<ArtistWithSongs?> {
        return artistDao.getArtistWithSongsByName(artistName)
    }

    override fun getAlbumArtistWithSongsByName(albumArtistName: String): Flow<AlbumArtistWithSongs?> {
        return albumArtistDao.getAlbumArtistWithSongs(albumArtistName)
    }

    override fun getComposerWithSongsByName(composerName: String): Flow<ComposerWithSongs?> {
        return composerDao.getComposerWithSongs(composerName)
    }

    override fun getLyricistWithSongsByName(lyricistName: String): Flow<LyricistWithSongs?> {
        return lyricistDao.getLyricistWithSongs(lyricistName)
    }

    override fun getGenreWithSongsByName(genre: String): Flow<GenreWithSongs?> {
        return genreDao.getGenreWithSongs(genre)
    }

    override fun getFavouriteSongs(): Flow<List<Song>> {
        return songDao.getAllFavourites()
    }

    override fun getRecentlyAddedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyAddedSongs()
    }

    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs()
    }

    override fun getTopTracks(): Flow<List<Song>> {
        return songDao.getTopTracks()
    }

    override suspend fun updateSong(song: Song) {
        songDao.updateSong(song)
    }

    override suspend fun getSongsFromLocations(locations: List<String>): List<Song> {
        return songDao.getSongsFromLocations(locations)
    }

    override suspend fun getSongsByAlbumName(albumName: String): List<Song> {
        return songDao.getSongsByAlbumName(albumName)
    }

    override suspend fun getSongsByArtistName(artistName: String): List<Song> {
        return songDao.getSongsByArtistName(artistName)
    }

    override suspend fun getSongsUnderFolderPath(folderPath: String): List<Song> {
        return songDao.getSongsUnderFolderPath(folderPath)
    }

    override suspend fun removeSongFromLibraryMetadata(song: Song) {
        songDao.deleteSong(song)
        albumDao.cleanAlbumTable()
        artistDao.cleanArtistTable()
        albumArtistDao.cleanAlbumArtistTable()
        composerDao.cleanComposerTable()
        lyricistDao.cleanLyricistTable()
        genreDao.cleanGenreTable()
    }
}