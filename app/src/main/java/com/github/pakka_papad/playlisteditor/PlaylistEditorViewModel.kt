package com.github.pakka_papad.playlisteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.pakka_papad.R
import com.github.pakka_papad.data.music.Playlist
import com.github.pakka_papad.data.services.PlayerService
import com.github.pakka_papad.data.services.PlaylistService
import com.github.pakka_papad.data.services.QueueService
import com.github.pakka_papad.data.services.SongService
import com.github.pakka_papad.util.MessageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistService: PlaylistService,
    private val songService: SongService,
    private val queueService: QueueService,
    private val playerService: PlayerService,
    private val messageStore: MessageStore,
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<Long>("playlistId")
        ?: error("playlistId missing")

    val playlistWithSongs = playlistService.getPlaylistWithSongsById(playlistId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val albums = songService.albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val artists = songService.artists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    fun consumeMessage() {
        _message.update { "" }
    }

    private fun postDone() {
        _message.update { messageStore.getString(R.string.done) }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            playlistService.reorderPlaylistSongs(playlistId, fromIndex, toIndex)
        }
    }

    fun removeSong(location: String) {
        viewModelScope.launch {
            playlistService.removeSongsFromPlaylist(listOf(location), playlistId)
        }
    }

    fun renamePlaylist(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val p: Playlist = playlistWithSongs.value?.playlist ?: return@launch
            playlistService.updatePlaylist(
                p.copy(playlistName = trimmed),
            )
            postDone()
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            playlistService.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    fun playAll() {
        viewModelScope.launch {
            val songs = playlistWithSongs.value?.songs ?: return@launch
            if (songs.isEmpty()) return@launch
            playerService.startServiceIfNotRunning(songs, 0)
        }
    }

    fun playFromIndex(index: Int) {
        viewModelScope.launch {
            val songs = playlistWithSongs.value?.songs ?: return@launch
            if (index !in songs.indices) return@launch
            playerService.startServiceIfNotRunning(songs, index)
        }
    }

    fun addQueueToPlaylist() {
        viewModelScope.launch {
            val locs = queueService.queue.map { it.location }
            if (locs.isEmpty()) {
                _message.update { messageStore.getString(R.string.playlist_editor_queue_empty) }
                return@launch
            }
            playlistService.addSongsToPlaylist(locs, playlistId)
            postDone()
        }
    }

    fun addSongsFromAlbum(albumName: String) {
        viewModelScope.launch {
            val songs = songService.getSongsByAlbumName(albumName)
            if (songs.isEmpty()) return@launch
            playlistService.addSongsToPlaylist(songs.map { it.location }, playlistId)
            postDone()
        }
    }

    fun addSongsFromArtist(artistName: String) {
        viewModelScope.launch {
            val songs = songService.getSongsByArtistName(artistName)
            if (songs.isEmpty()) return@launch
            playlistService.addSongsToPlaylist(songs.map { it.location }, playlistId)
            postDone()
        }
    }

    fun addSongsFromFolder(folderPath: String) {
        val trimmed = folderPath.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val songs = songService.getSongsUnderFolderPath(trimmed)
            if (songs.isEmpty()) {
                _message.update { messageStore.getString(R.string.playlist_editor_folder_empty) }
                return@launch
            }
            playlistService.addSongsToPlaylist(songs.map { it.location }, playlistId)
            postDone()
        }
    }

    suspend fun getExportM3uText(): String? =
        playlistService.exportPlaylistM3u(playlistId)

    fun importM3uContent(content: String) {
        viewModelScope.launch {
            val added = playlistService.importM3uContent(playlistId, content)
            _message.update {
                messageStore.getString(R.string.playlist_import_done, added)
            }
        }
    }
}
