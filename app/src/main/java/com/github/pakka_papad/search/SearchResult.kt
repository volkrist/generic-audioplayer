package com.github.pakka_papad.search

import com.github.pakka_papad.data.music.Album
import com.github.pakka_papad.data.music.Artist
import com.github.pakka_papad.data.music.Playlist
import com.github.pakka_papad.data.music.Song

data class FolderSearchResult(
    val name: String,
    val absolutePath: String,
)

data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<FolderSearchResult> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val dictaphoneRecordings: List<Song> = emptyList(),
    val errorMsg: String? = null,
)
