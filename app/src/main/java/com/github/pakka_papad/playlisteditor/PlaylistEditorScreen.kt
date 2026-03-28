package com.github.pakka_papad.playlisteditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R
import com.github.pakka_papad.components.FullScreenSadMessage
import com.github.pakka_papad.components.Snackbar
import com.github.pakka_papad.components.SongCardV2
import com.github.pakka_papad.components.more_options.SongOptions
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.nowplaying.DraggableItem
import com.github.pakka_papad.nowplaying.dragContainer
import com.github.pakka_papad.nowplaying.rememberDragDropState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistEditorScreen(
    viewModel: PlaylistEditorViewModel,
    onBack: () -> Unit,
    onAddFromLibrary: () -> Unit,
    onExportM3u: () -> Unit,
    onImportM3u: () -> Unit,
    onFavouriteClicked: (Song) -> Unit,
) {
    val playlistWithSongs by viewModel.playlistWithSongs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message.isEmpty()) return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showArtistPicker by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderText by remember { mutableStateOf("") }

    val songs = playlistWithSongs?.songs.orEmpty()
    val title = playlistWithSongs?.playlist?.playlistName.orEmpty()
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { from, to ->
        viewModel.reorder(from, to)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.playAll() },
                        enabled = songs.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.playlist_editor_play_all),
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.more_menu_button),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_rename)) },
                            onClick = {
                                menuExpanded = false
                                renameText = title
                                showRename = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_add_queue)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.addQueueToPlaylist()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_add_from_library)) },
                            onClick = {
                                menuExpanded = false
                                onAddFromLibrary()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_add_album)) },
                            onClick = {
                                menuExpanded = false
                                showAlbumPicker = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_add_artist)) },
                            onClick = {
                                menuExpanded = false
                                showArtistPicker = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_add_folder)) },
                            onClick = {
                                menuExpanded = false
                                folderText = ""
                                showFolderDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_export_m3u)) },
                            onClick = {
                                menuExpanded = false
                                onExportM3u()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_import_m3u)) },
                            onClick = {
                                menuExpanded = false
                                onImportM3u()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_editor_delete_playlist)) },
                            onClick = {
                                menuExpanded = false
                                showDelete = true
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { Snackbar(it) }
        },
    ) { padding ->
        when {
            playlistWithSongs == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    FullScreenSadMessage(stringResource(R.string.no_songs_found))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface)
                        .dragContainer(dragDropState),
                    state = listState,
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.location },
                    ) { index, song ->
                        DraggableItem(dragDropState, index) {
                            SongCardV2(
                                song = song,
                                onSongClicked = { viewModel.playFromIndex(index) },
                                onFavouriteClicked = onFavouriteClicked,
                                currentlyPlaying = false,
                                songOptions = listOf(
                                    SongOptions.RemoveFromPlaylist {
                                        viewModel.removeSong(song.location)
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.playlist_editor_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renamePlaylist(renameText)
                        showRename = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.playlist_editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.playlist_editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        viewModel.deletePlaylist(onBack)
                    },
                ) { Text(stringResource(R.string.playlist_editor_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showAlbumPicker) {
        AlertDialog(
            onDismissRequest = { showAlbumPicker = false },
            title = { Text(stringResource(R.string.playlist_editor_pick_album)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    items(albums, key = { it.name }) { album ->
                        TextButton(
                            onClick = {
                                viewModel.addSongsFromAlbum(album.name)
                                showAlbumPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbumPicker = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showArtistPicker) {
        AlertDialog(
            onDismissRequest = { showArtistPicker = false },
            title = { Text(stringResource(R.string.playlist_editor_pick_artist)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    items(artists, key = { it.name }) { artist ->
                        TextButton(
                            onClick = {
                                viewModel.addSongsFromArtist(artist.name)
                                showArtistPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArtistPicker = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(stringResource(R.string.playlist_editor_folder_path_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.playlist_editor_folder_path_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = folderText,
                        onValueChange = { folderText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addSongsFromFolder(folderText)
                        showFolderDialog = false
                    },
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
