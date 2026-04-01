package com.generic.audioplayes.data.services

import com.generic.audioplayes.data.daos.AlbumArtistDao
import com.generic.audioplayes.data.daos.AlbumDao
import com.generic.audioplayes.data.daos.ArtistDao
import com.generic.audioplayes.data.daos.ComposerDao
import com.generic.audioplayes.data.daos.GenreDao
import com.generic.audioplayes.data.daos.LyricistDao
import com.generic.audioplayes.data.daos.PlaylistDao
import com.generic.audioplayes.data.daos.SongDao
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.AlbumArtist
import com.generic.audioplayes.data.music.Artist
import com.generic.audioplayes.data.music.Composer
import com.generic.audioplayes.data.music.Genre
import com.generic.audioplayes.data.music.Lyricist
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.Song

interface SearchService {
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun searchAlbumArtists(query: String): List<AlbumArtist>
    suspend fun searchComposers(query: String): List<Composer>
    suspend fun searchLyricists(query: String): List<Lyricist>
    suspend fun searchPlaylists(query: String): List<Playlist>
    suspend fun searchGenres(query: String): List<Genre>
}

class SearchServiceImpl(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val albumArtistDao: AlbumArtistDao,
    private val composerDao: ComposerDao,
    private val lyricistDao: LyricistDao,
    private val genreDao: GenreDao,
    private val playlistDao: PlaylistDao,
): SearchService {
    override suspend fun searchSongs(query: String): List<Song> {
        return songDao.searchSongs(query)
    }

    override suspend fun searchAlbums(query: String): List<Album> {
        return albumDao.searchAlbums(query)
    }

    override suspend fun searchArtists(query: String): List<Artist> {
        return artistDao.searchArtists(query)
    }

    override suspend fun searchAlbumArtists(query: String): List<AlbumArtist> {
        return albumArtistDao.searchAlbumArtists(query)
    }

    override suspend fun searchComposers(query: String): List<Composer> {
        return composerDao.searchComposers(query)
    }

    override suspend fun searchLyricists(query: String): List<Lyricist> {
        return lyricistDao.searchLyricists(query)
    }

    override suspend fun searchPlaylists(query: String): List<Playlist> {
        return playlistDao.searchPlaylists(query)
    }

    override suspend fun searchGenres(query: String): List<Genre> {
        return genreDao.searchGenres(query)
    }
}