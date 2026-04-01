package com.generic.audioplayes.search

import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.Artist
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.Song

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
