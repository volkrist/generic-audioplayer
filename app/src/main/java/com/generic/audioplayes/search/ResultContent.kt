package com.generic.audioplayes.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.SongCardV3
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.Artist
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.home.AlbumCard

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun TextCard(
    text: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ResultContent(
    contentPadding: PaddingValues,
    searchResult: SearchResult,
    onSongClicked: (Song) -> Unit,
    onAlbumClicked: (Album) -> Unit,
    onArtistClicked: (Artist) -> Unit,
    onFolderClicked: (FolderSearchResult) -> Unit,
    onPlaylistClicked: (Playlist) -> Unit,
    onDictaphoneRecordingClicked: (Song) -> Unit,
) {
    val empty = searchResult.songs.isEmpty() &&
        searchResult.albums.isEmpty() &&
        searchResult.artists.isEmpty() &&
        searchResult.folders.isEmpty() &&
        searchResult.playlists.isEmpty() &&
        searchResult.dictaphoneRecordings.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (searchResult.songs.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_songs)) }
            items(
                items = searchResult.songs,
                key = { it.location },
            ) { song ->
                SongCardV3(
                    song = song,
                    onSongClicked = onSongClicked,
                )
            }
        }
        if (searchResult.albums.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_albums)) }
            items(
                items = searchResult.albums,
                key = { it.name },
            ) { album ->
                AlbumCard(
                    album = album,
                    onAlbumClicked = onAlbumClicked,
                )
            }
        }
        if (searchResult.artists.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_artists)) }
            items(
                items = searchResult.artists,
                key = { it.name },
            ) { artist ->
                TextCard(
                    text = artist.name,
                    onClick = { onArtistClicked(artist) },
                )
            }
        }
        if (searchResult.folders.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_folders)) }
            items(
                items = searchResult.folders,
                key = { it.absolutePath },
            ) { folder ->
                TextCard(
                    text = folder.name,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_folder_40),
                            contentDescription = null,
                        )
                    },
                    onClick = { onFolderClicked(folder) },
                )
            }
        }
        if (searchResult.playlists.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_playlists)) }
            items(
                items = searchResult.playlists,
                key = { it.playlistId },
            ) { playlist ->
                TextCard(
                    text = playlist.playlistName,
                    onClick = { onPlaylistClicked(playlist) },
                )
            }
        }
        if (searchResult.dictaphoneRecordings.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_section_dictaphone)) }
            items(
                items = searchResult.dictaphoneRecordings,
                key = { it.location },
            ) { song ->
                SongCardV3(
                    song = song,
                    onSongClicked = onDictaphoneRecordingClicked,
                )
            }
        }
        if (empty) {
            item {
                Text(
                    text = stringResource(R.string.search_no_results),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
