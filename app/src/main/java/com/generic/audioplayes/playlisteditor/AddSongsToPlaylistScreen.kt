package com.generic.audioplayes.playlisteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.data.music.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistScreen(
    viewModel: AddSongsToPlaylistViewModel,
    onBack: () -> Unit,
    onAdded: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchInput by viewModel.searchInput.collectAsStateWithLifecycle()
    val selected by viewModel.selectedLocations.collectAsStateWithLifecycle()

    val addEnabled = selected.isNotEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_songs_to_playlist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Button(
                    onClick = { viewModel.addSelectedToPlaylist(onAdded) },
                    enabled = addEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.add_songs_to_playlist_add_selected,
                            selected.size,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = viewModel::updateSearchInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.add_songs_to_playlist_search_hint)) },
                singleLine = true,
            )

            when {
                uiState.songs.isEmpty() -> {
                    FullScreenSadMessage(stringResource(R.string.no_songs_found))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(
                            items = uiState.songs,
                            key = { it.location },
                        ) { song ->
                            LibrarySongPickRow(
                                song = song,
                                alreadyInPlaylist = song.location in uiState.playlistSongLocations,
                                selected = song.location in selected,
                                onToggle = { viewModel.toggleSelection(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySongPickRow(
    song: Song,
    alreadyInPlaylist: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !alreadyInPlaylist, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = alreadyInPlaylist || selected,
            onCheckedChange = { if (!alreadyInPlaylist) onToggle() },
            enabled = !alreadyInPlaylist,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (alreadyInPlaylist) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (alreadyInPlaylist) {
            Text(
                text = stringResource(R.string.add_songs_to_playlist_already_added),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
