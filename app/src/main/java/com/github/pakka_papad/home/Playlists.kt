package com.github.pakka_papad.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.pakka_papad.R
import com.github.pakka_papad.components.PlaylistCardV2
import com.github.pakka_papad.components.more_options.PlaylistOptions
import com.github.pakka_papad.data.UserPreferences
import com.github.pakka_papad.data.music.PlaylistWithSongCount
import com.github.pakka_papad.data.music.SmartPlaylistCounts
import com.github.pakka_papad.ui.theme.LocalThemePreference
import com.github.pakka_papad.ui.theme.harmonize
import scheme.Scheme

@Composable
fun Playlists(
    playlistsWithSongCount: List<PlaylistWithSongCount>?,
    smartPlaylistCounts: SmartPlaylistCounts,
    onPlaylistClicked: (Long) -> Unit,
    listState: LazyGridState,
    onPlaylistCreate: (String) -> Unit,
    onFavouritesClicked: () -> Unit,
    onRecentlyAddedClicked: () -> Unit,
    onRecentlyPlayedClicked: () -> Unit,
    onTopTracksClicked: () -> Unit,
    onDeletePlaylistClicked: (PlaylistWithSongCount) -> Unit,
) {
    if (playlistsWithSongCount == null) return
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
        columns = GridCells.Adaptive(150.dp),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        item {
            SmartSystemPlaylistCard(
                title = stringResource(R.string.favourites),
                songCount = smartPlaylistCounts.favourites,
                seedColor = Color(0xFFE90064),
                iconPainter = painterResource(R.drawable.ic_baseline_favorite_border_24),
                contentDescription = stringResource(R.string.favourite_button),
                onClick = onFavouritesClicked,
            )
        }
        item {
            SmartSystemPlaylistCard(
                title = stringResource(R.string.smart_playlist_recently_added),
                songCount = smartPlaylistCounts.recentlyAdded,
                seedColor = Color(0xFF2E7D32),
                iconPainter = painterResource(R.drawable.ic_baseline_music_note_40),
                contentDescription = stringResource(R.string.smart_playlist_recently_added),
                onClick = onRecentlyAddedClicked,
            )
        }
        item {
            SmartSystemPlaylistCard(
                title = stringResource(R.string.smart_playlist_recently_played),
                songCount = smartPlaylistCounts.recentlyPlayed,
                seedColor = Color(0xFF7B1FA2),
                iconPainter = painterResource(R.drawable.ic_baseline_queue_music_40),
                contentDescription = stringResource(R.string.smart_playlist_recently_played),
                onClick = onRecentlyPlayedClicked,
            )
        }
        item {
            SmartSystemPlaylistCard(
                title = stringResource(R.string.smart_playlist_top_tracks),
                songCount = smartPlaylistCounts.topTracks,
                seedColor = Color(0xFFF57C00),
                iconPainter = painterResource(R.drawable.ic_baseline_album_40),
                contentDescription = stringResource(R.string.smart_playlist_top_tracks),
                onClick = onTopTracksClicked,
            )
        }
        item {
            CreatePlaylistCard(
                onPlaylistCreate = onPlaylistCreate,
            )
        }
        items(
            items = playlistsWithSongCount,
            key = { it.playlistId }
        ) { playlist ->
            PlaylistCardV2(
                playlistWithSongCount = playlist,
                onPlaylistClicked = onPlaylistClicked,
                options = listOf(
                    PlaylistOptions.DeletePlaylist {
                        onDeletePlaylistClicked(playlist)
                    }
                )
            )
        }
    }
}

@Composable
private fun SmartSystemPlaylistCard(
    title: String,
    songCount: Int,
    seedColor: Color,
    iconPainter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val themePreference = LocalThemePreference.current
    val isSystemDark = isSystemInDarkTheme()
    val scheme by remember(themePreference, seedColor) {
        derivedStateOf {
            val isDark = when (themePreference.theme) {
                UserPreferences.Theme.LIGHT_MODE, UserPreferences.Theme.UNRECOGNIZED -> false
                UserPreferences.Theme.DARK_MODE, UserPreferences.Theme.AMOLED_MODE -> true
                UserPreferences.Theme.USE_SYSTEM_MODE -> isSystemDark
            }
            if (isDark) Scheme.dark(seedColor.toArgb())
            else Scheme.light(seedColor.toArgb())
        }
    }
    Column(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier
                .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = false)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            harmonize(Color(scheme.primary)),
                            harmonize(Color(scheme.primaryContainer))
                        )
                    )
                )
                .padding(45.dp),
            painter = iconPainter,
            contentDescription = contentDescription,
            tint = Color(scheme.onPrimary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pluralStringResource(R.plurals.song_count, songCount, songCount.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CreatePlaylistCard(
    onPlaylistCreate: (String) -> Unit,
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .clickable(onClick = { isDialogVisible = true })
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_playlist_add_40),
            modifier = Modifier
                .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = false)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(45.dp),
            contentDescription = stringResource(R.string.create_playlist_button),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = stringResource(R.string.new_playlist),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis
        )
    }
    AnimatedVisibility (isDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDialogVisible = false },
            confirmButton = {
                Button(
                    onClick = {
                        isDialogVisible = false
                        onPlaylistCreate(playlistName)
                        playlistName = ""
                    }
                ) {
                    Text(
                        text = stringResource(R.string.create),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isDialogVisible = false
                        playlistName = ""
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.create_playlist)
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                    },
                    label = {
                        Text(text = stringResource(R.string.playlist_name))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                )
            }
        )
    }
}
