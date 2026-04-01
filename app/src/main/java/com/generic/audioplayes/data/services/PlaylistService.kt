package com.generic.audioplayes.data.services

import com.generic.audioplayes.data.daos.PlaylistDao
import com.generic.audioplayes.data.daos.SongDao
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.PlaylistExceptId
import com.generic.audioplayes.data.music.PlaylistSongCrossRef
import com.generic.audioplayes.data.music.PlaylistWithSongCount
import com.generic.audioplayes.data.music.PlaylistWithSongs
import com.generic.audioplayes.data.thumbnails.ThumbnailDao
import com.generic.audioplayes.util.PlaylistM3u
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

interface PlaylistService {
    val playlists: Flow<List<PlaylistWithSongCount>>

    fun getPlaylistWithSongsById(playlistId: Long): Flow<PlaylistWithSongs?>

    suspend fun createPlaylist(name: String): Boolean
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun updatePlaylist(updatedPlaylist: Playlist)

    suspend fun addSongsToPlaylist(songLocations: List<String>, playlistId: Long)
    suspend fun removeSongsFromPlaylist(songLocations: List<String>, playlistId: Long)

    suspend fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int)

    /** Parses M3U text and appends matching library tracks to the playlist (skipped if already present). */
    suspend fun importM3uContent(playlistId: Long, fileContent: String): Int

    suspend fun exportPlaylistM3u(playlistId: Long): String?
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistServiceImpl(
    private val playlistDao: PlaylistDao,
    private val thumbnailDao: ThumbnailDao,
    private val songDao: SongDao,
) : PlaylistService {
    override val playlists: Flow<List<PlaylistWithSongCount>>
        = playlistDao.getAllPlaylistWithSongCount()

    override fun getPlaylistWithSongsById(playlistId: Long): Flow<PlaylistWithSongs?> {
        return playlistDao.getPlaylistWithSongs(playlistId).flatMapLatest { pws ->
            flow {
                if (pws == null) {
                    emit(null)
                } else {
                    val order = playlistDao.getCrossRefsOrdered(playlistId).associate { it.location to it.position }
                    emit(
                        pws.copy(
                            songs = pws.songs.sortedBy { order[it.location] ?: 0 },
                        ),
                    )
                }
            }
        }
    }

    override suspend fun createPlaylist(name: String): Boolean {
        if (name.isBlank()) return false
        val playlist = PlaylistExceptId(
            playlistName = name.trim(),
            createdAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)
        return true
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        val playlist = playlistDao.getPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
        if (playlist?.artUri == null) return
        thumbnailDao.markDelete(playlist.artUri)
    }

    override suspend fun updatePlaylist(updatedPlaylist: Playlist) {
        playlistDao.updatePlaylist(updatedPlaylist)
    }

    override suspend fun addSongsToPlaylist(songLocations: List<String>, playlistId: Long) {
        if (songLocations.isEmpty()) return
        val maxPos = playlistDao.getMaxPosition(playlistId)
        var next = maxPos + 1
        if (next < 0) next = 0
        val refs = songLocations.mapIndexed { i, loc ->
            PlaylistSongCrossRef(playlistId, loc, next + i)
        }
        playlistDao.insertPlaylistSongCrossRef(refs)
    }

    override suspend fun removeSongsFromPlaylist(songLocations: List<String>, playlistId: Long) {
        songLocations.forEach {
            playlistDao.deletePlaylistSongCrossRef(
                PlaylistSongCrossRef(playlistId, it, 0)
            )
        }
    }

    override suspend fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val refs = playlistDao.getCrossRefsOrdered(playlistId).toMutableList()
        if (fromIndex !in refs.indices || toIndex !in refs.indices) return
        val item = refs.removeAt(fromIndex)
        refs.add(toIndex, item)
        playlistDao.setPlaylistSongOrder(playlistId, refs.map { it.location })
    }

    override suspend fun importM3uContent(playlistId: Long, fileContent: String): Int {
        val paths = PlaylistM3u.parsePaths(fileContent)
        if (paths.isEmpty()) return 0
        val resolved = paths.mapNotNull { path ->
            PlaylistM3u.resolveLocation(
                { songDao.getSongByLocation(it) },
                path,
            )
        }.distinctBy { it.location }
        if (resolved.isEmpty()) return 0
        addSongsToPlaylist(resolved.map { it.location }, playlistId)
        return resolved.size
    }

    override suspend fun exportPlaylistM3u(playlistId: Long): String? {
        val withSongs = playlistDao.getPlaylistWithSongs(playlistId).first() ?: return null
        return PlaylistM3u.buildM3u(withSongs.songs)
    }
}