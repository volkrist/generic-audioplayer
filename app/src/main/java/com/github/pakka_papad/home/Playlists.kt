package com.github.pakka_papad.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.github.pakka_papad.R
import com.github.pakka_papad.components.PlaylistCardV2
import com.github.pakka_papad.components.more_options.PlaylistOptions
import com.github.pakka_papad.data.music.PlaylistWithSongCount
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

enum class HomeSmartPlaylistAction {
    Favourites,
    RecentlyAdded,
    RecentlyPlayed,
    TopTracks,
}

@Composable
fun Playlists(
    allSongs: List<Song>?,
    playlistsWithSongCount: List<PlaylistWithSongCount>?,
    onPlaylistClicked: (Long) -> Unit,
    listState: LazyGridState,
    onPlaylistCreate: (String) -> Unit,
    onDeletePlaylistClicked: (PlaylistWithSongCount) -> Unit,
    onSmartPlaylist: (HomeSmartPlaylistAction) -> Unit,
) {
    if (playlistsWithSongCount == null) return
    val scheme = MaterialTheme.colorScheme
    val songs = allSongs.orEmpty()
    val nowSec = System.currentTimeMillis() / 1000L
    val windowSec = 30L * 24L * 3600L
    val recentlyAddedCount = songs.count {
        it.dateModifiedSec > 0L && nowSec - it.dateModifiedSec <= windowSec
    }
    val recentlyPlayedCount = songs.count { it.lastPlayed != null && it.lastPlayed!! > 0L }
    val topTracksCount = songs.count { it.playCount > 0 }
    val favouritesCount = songs.count { it.favourite }

    val contentPadding = PaddingValues(
        start = HomeLibraryTokens.contentHorizontalPadding,
        end = HomeLibraryTokens.contentHorizontalPadding,
        top = UiTokens.gridContentPaddingTop,
        bottom = UiTokens.gridContentPaddingBottom,
    )

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        columns = GridCells.Adaptive(HomeLibraryTokens.gridMinSize),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(UiTokens.gridVerticalSpacing),
        contentPadding = contentPadding,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.home_library_smart_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.padding(bottom = UiTokens.smartSectionTitlePaddingBottom),
            )
        }
        item {
            CreatePlaylistCard(onPlaylistCreate = onPlaylistCreate)
        }
        item {
            SmartPlaylistSquareCard(
                title = stringResource(R.string.favourites),
                countText = pluralStringResource(
                    R.plurals.song_count,
                    favouritesCount,
                    favouritesCount,
                ),
                iconVector = Icons.Outlined.FavoriteBorder,
                gradient = listOf(scheme.primary, scheme.primaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.Favourites) },
            )
        }
        item {
            SmartPlaylistSquareCard(
                title = stringResource(R.string.smart_playlist_recently_added),
                countText = pluralStringResource(
                    R.plurals.song_count,
                    recentlyAddedCount,
                    recentlyAddedCount,
                ),
                iconVector = Icons.Outlined.Add,
                gradient = listOf(scheme.tertiary, scheme.tertiaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.RecentlyAdded) },
            )
        }
        item {
            SmartPlaylistSquareCard(
                title = stringResource(R.string.smart_playlist_recently_played),
                countText = pluralStringResource(
                    R.plurals.song_count,
                    recentlyPlayedCount,
                    recentlyPlayedCount,
                ),
                iconVector = Icons.Outlined.PlayArrow,
                gradient = listOf(scheme.secondary, scheme.secondaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.RecentlyPlayed) },
            )
        }
        item {
            SmartPlaylistSquareCard(
                title = stringResource(R.string.smart_playlist_top_tracks),
                countText = pluralStringResource(
                    R.plurals.song_count,
                    topTracksCount,
                    topTracksCount,
                ),
                iconVector = Icons.Outlined.Star,
                gradient = listOf(scheme.error, scheme.errorContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.TopTracks) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.home_library_your_playlists),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.padding(
                    top = UiTokens.userPlaylistSectionTitlePaddingTop,
                    bottom = UiTokens.smartSectionTitlePaddingBottom,
                ),
            )
        }
        items(
            items = playlistsWithSongCount,
            key = { it.playlistId },
        ) { playlist ->
            PlaylistCardV2(
                playlistWithSongCount = playlist,
                onPlaylistClicked = onPlaylistClicked,
                options = listOf(
                    PlaylistOptions.DeletePlaylist {
                        onDeletePlaylistClicked(playlist)
                    },
                ),
            )
        }
    }
}

@Composable
private fun SmartPlaylistSquareCard(
    title: String,
    countText: String,
    iconVector: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UiTokens.playlistTileVerticalPadding)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.cornerExtraLarge))
                .background(
                    Brush.linearGradient(gradient),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(UiTokens.iconSizeTouch),
                tint = Color.White.copy(alpha = 0.95f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
        Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UiTokens.playlistTileVerticalPadding)
            .clickable(onClick = { isDialogVisible = true }),
        verticalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.cornerExtraLarge))
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primary.copy(alpha = 0.9f), scheme.primaryContainer),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_playlist_add_40),
                contentDescription = stringResource(R.string.create_playlist_button),
                modifier = Modifier.size(UiTokens.iconSizeTouch),
                tint = Color.White.copy(alpha = 0.95f),
            )
        }
        Text(
            text = stringResource(R.string.new_playlist),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = scheme.onSurface,
        )
        Text(
            text = stringResource(R.string.create_playlist_button),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    AnimatedVisibility(isDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDialogVisible = false },
            confirmButton = {
                Button(
                    onClick = {
                        isDialogVisible = false
                        onPlaylistCreate(playlistName)
                        playlistName = ""
                    },
                ) {
                    Text(
                        text = stringResource(R.string.create),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isDialogVisible = false
                        playlistName = ""
                    },
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.create_playlist),
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
            },
        )
    }
}
