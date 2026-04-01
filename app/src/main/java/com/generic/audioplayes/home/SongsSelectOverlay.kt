package com.generic.audioplayes.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens

private val overlayBg = Color(0xFF0A0A0C)
private val barBg = Color(0xFF151018)
private val accentOrange = Color(0xFFFF9800)

@Composable
fun SongsSelectOverlay(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onPlaySelected: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onDeleteSelected: (List<Song>) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var query by remember { mutableStateOf("") }
    var selectedLocations by remember { mutableStateOf(emptySet<String>()) }
    var deleteConfirm by remember { mutableStateOf(false) }

    val filtered by remember(songs, query) {
        derivedStateOf {
            val q = query.trim().lowercase()
            if (q.isEmpty()) songs
            else songs.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
    }

    val allFilteredSelected by remember(filtered, selectedLocations) {
        derivedStateOf {
            filtered.isNotEmpty() && filtered.all { selectedLocations.contains(it.location) }
        }
    }

    fun toggleAllFiltered() {
        val set = selectedLocations.toMutableSet()
        if (allFilteredSelected) {
            filtered.forEach { set.remove(it.location) }
        } else {
            filtered.forEach { set.add(it.location) }
        }
        selectedLocations = set
    }

    fun toggleOne(song: Song) {
        val set = selectedLocations.toMutableSet()
        if (set.contains(song.location)) set.remove(song.location) else set.add(song.location)
        selectedLocations = set
    }

    val selectedSongsOrdered by remember(selectedLocations, songs) {
        derivedStateOf {
            songs.filter { selectedLocations.contains(it.location) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = overlayBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeLibraryTokens.libraryShellGradient),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(R.string.songs_select_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { /* reserved */ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.player_actions_more),
                        tint = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.songs_select_search_placeholder),
                        color = Color.White.copy(alpha = 0.45f),
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = accentOrange,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = allFilteredSelected,
                    onCheckedChange = { toggleAllFiltered() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentOrange,
                        uncheckedColor = Color.White.copy(alpha = 0.45f),
                    ),
                )
                Text(
                    text = stringResource(R.string.songs_select_all),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { toggleAllFiltered() },
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = WindowInsets.systemBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered, key = { it.location }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedLocations.contains(song.location),
                            onCheckedChange = { toggleOne(song) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentOrange,
                                uncheckedColor = Color.White.copy(alpha = 0.45f),
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { toggleOne(song) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = song.artUri,
                                contentDescription = stringResource(R.string.song_image),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surface,
                                            ),
                                        ),
                                    ),
                                contentScale = ContentScale.Crop,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Text(
                                    text = song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "${song.artist} — ${song.album}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = barBg,
                tonalElevation = UiTokens.elevationTonalLow,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = selectedSongsOrdered.isNotEmpty()) {
                                onPlaySelected(selectedSongsOrdered)
                                onDismiss()
                            },
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_play_arrow_40),
                            contentDescription = null,
                            tint = if (selectedSongsOrdered.isNotEmpty()) accentOrange else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.songs_select_play),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedSongsOrdered.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = selectedSongsOrdered.isNotEmpty()) {
                                onAddToPlaylist(selectedSongsOrdered)
                                onDismiss()
                            },
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_playlist_add_40),
                            contentDescription = null,
                            tint = if (selectedSongsOrdered.isNotEmpty()) accentOrange else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.songs_select_add_playlist),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selectedSongsOrdered.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = selectedSongsOrdered.isNotEmpty()) {
                                deleteConfirm = true
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = if (selectedSongsOrdered.isNotEmpty()) accentOrange else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.songs_select_delete),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedSongsOrdered.isNotEmpty()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(stringResource(R.string.songs_select_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.songs_select_delete_confirm_body,
                        selectedSongsOrdered.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSelected(selectedSongsOrdered)
                        deleteConfirm = false
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.songs_select_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
