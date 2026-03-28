package com.github.pakka_papad.playlisteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.services.PlaylistService
import com.github.pakka_papad.data.services.SongService
import com.github.pakka_papad.util.NaturalOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSongsToPlaylistUiState(
    val songs: List<Song> = emptyList(),
    val playlistSongLocations: Set<String> = emptySet(),
    val searchQuery: String = "",
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddSongsToPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songService: SongService,
    private val playlistService: PlaylistService,
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<Long>("playlistId")
        ?: error("playlistId missing")

    private val _searchInput = MutableStateFlow("")
    val searchInput: StateFlow<String> = _searchInput.asStateFlow()

    private val debouncedQuery = _searchInput
        .map { it.trim() }
        .debounce(300)
        .distinctUntilChanged()

    private val playlistLocations = playlistService.getPlaylistWithSongsById(playlistId)
        .map { pws ->
            pws?.songs?.map { it.location }?.toSet().orEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState = combine(
        songService.songs,
        debouncedQuery,
        playlistLocations,
    ) { allSongs, query, inPlaylist ->
        val filtered = filterSongs(allSongs, query)
        AddSongsToPlaylistUiState(
            songs = filtered,
            playlistSongLocations = inPlaylist,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddSongsToPlaylistUiState(),
    )

    private val _selectedLocations = MutableStateFlow<Set<String>>(emptySet())
    val selectedLocations: StateFlow<Set<String>> = _selectedLocations.asStateFlow()

    fun updateSearchInput(text: String) {
        _searchInput.value = text
    }

    fun toggleSelection(song: Song) {
        if (song.location in playlistLocations.value) return
        _selectedLocations.update { cur ->
            if (song.location in cur) cur - song.location else cur + song.location
        }
    }

    fun addSelectedToPlaylist(onDone: () -> Unit) {
        viewModelScope.launch {
            val inPlaylist = playlistService.getPlaylistWithSongsById(playlistId)
                .first()
                ?.songs
                ?.map { it.location }
                ?.toSet()
                .orEmpty()
            val toAdd = _selectedLocations.value.filter { it !in inPlaylist }
            if (toAdd.isEmpty()) return@launch
            playlistService.addSongsToPlaylist(toAdd, playlistId)
            _selectedLocations.update { emptySet() }
            onDone()
        }
    }

    companion object {
        fun filterSongs(allSongs: List<Song>, query: String): List<Song> {
            val q = query.trim()
            val base = if (q.isEmpty()) {
                allSongs
            } else {
                allSongs.filter { song ->
                    song.title.contains(q, ignoreCase = true) ||
                        song.artist.contains(q, ignoreCase = true) ||
                        song.album.contains(q, ignoreCase = true)
                }
            }
            return base.sortedWith(compareBy(NaturalOrder.stringComparator) { it.title })
        }
    }
}
