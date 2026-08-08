package com.generic.audioplayes.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.PlaylistUserPlaylistRow
import com.generic.audioplayes.components.more_options.PlaylistOptions
import com.generic.audioplayes.data.music.PlaylistWithSongCount
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens
import kotlinx.coroutines.launch

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
    listState: LazyListState,
    onPlaylistCreate: (String) -> Unit,
    onDeletePlaylistClicked: (PlaylistWithSongCount) -> Unit,
    onSmartPlaylist: (HomeSmartPlaylistAction) -> Unit,
    onOpenSettingsForPlaylistTools: () -> Unit = {},
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.home_library_smart_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = UiTokens.smartSectionTitlePaddingBottom),
            )
        }
        item {
            SmartPlaylistListRow(
                title = stringResource(R.string.playlist_smart_favourites),
                countText = stringResource(R.string.playlist_songs_count_fmt, favouritesCount),
                iconVector = Icons.Outlined.FavoriteBorder,
                gradient = listOf(scheme.primary, scheme.primaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.Favourites) },
            )
        }
        item {
            SmartPlaylistListRow(
                title = stringResource(R.string.smart_playlist_recently_added),
                countText = stringResource(R.string.playlist_songs_count_fmt, recentlyAddedCount),
                iconVector = Icons.Outlined.Add,
                gradient = listOf(scheme.tertiary, scheme.tertiaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.RecentlyAdded) },
            )
        }
        item {
            SmartPlaylistListRow(
                title = stringResource(R.string.smart_playlist_recently_played),
                countText = stringResource(R.string.playlist_songs_count_fmt, recentlyPlayedCount),
                iconVector = Icons.Outlined.PlayArrow,
                gradient = listOf(scheme.secondary, scheme.secondaryContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.RecentlyPlayed) },
            )
        }
        item {
            SmartPlaylistListRow(
                title = stringResource(R.string.smart_playlist_top_tracks),
                countText = stringResource(R.string.playlist_songs_count_fmt, topTracksCount),
                iconVector = Icons.Outlined.Star,
                gradient = listOf(scheme.error, scheme.errorContainer),
                onClick = { onSmartPlaylist(HomeSmartPlaylistAction.TopTracks) },
            )
        }
        item {
            Text(
                text = stringResource(
                    R.string.home_library_my_playlists_fmt,
                    playlistsWithSongCount.size,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(
                    top = UiTokens.userPlaylistSectionTitlePaddingTop,
                    bottom = UiTokens.smartSectionTitlePaddingBottom,
                ),
            )
        }
        item {
            CreatePlaylistListRow(onPlaylistCreate = onPlaylistCreate)
        }
        items(
            items = playlistsWithSongCount,
            key = { it.playlistId },
        ) { playlist ->
            PlaylistUserPlaylistRow(
                playlistWithSongCount = playlist,
                onPlaylistClicked = onPlaylistClicked,
                options = listOf(
                    PlaylistOptions.DeletePlaylist {
                        onDeletePlaylistClicked(playlist)
                    },
                ),
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            PlaylistToolsActions(
                listState = listState,
                onRestoreClick = onOpenSettingsForPlaylistTools,
                onImportClick = onOpenSettingsForPlaylistTools,
            )
        }
    }
}

@Composable
private fun SmartPlaylistListRow(
    title: String,
    countText: String,
    iconVector: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true, radius = UiTokens.rippleLarge),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(UiTokens.playlistListRowArt)
                .clip(RoundedCornerShape(UiTokens.cornerMedium))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(UiTokens.smartTileIcon),
                tint = Color.White.copy(alpha = 0.95f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.textLineGapTight),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            modifier = Modifier
                .size(UiTokens.iconSizeTouch)
                .padding(10.dp)
                .clickable(
                    onClick = onClick,
                    indication = ripple(bounded = false, radius = UiTokens.rippleSmall),
                    interactionSource = remember { MutableInteractionSource() },
                ),
            tint = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun CreatePlaylistListRow(
    onPlaylistCreate: (String) -> Unit,
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .clickable(
                onClick = { isDialogVisible = true },
                indication = ripple(bounded = true, radius = UiTokens.rippleLarge),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(UiTokens.playlistListRowArt)
                .clip(RoundedCornerShape(UiTokens.cornerMedium))
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
                modifier = Modifier.size(UiTokens.smartTileIcon),
                tint = Color.White.copy(alpha = 0.95f),
            )
        }
        Text(
            text = stringResource(R.string.create_playlist),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.weight(1f),
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

@Composable
private fun PlaylistToolsActions(
    listState: LazyListState,
    onRestoreClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val accent = Color(0xFFFF9800)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = stringResource(R.string.library_playlist_tools_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = onRestoreClick,
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = accent,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.library_playlist_action_restore),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Surface(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_send_40),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = accent,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.library_playlist_action_import),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        indication = ripple(bounded = false, radius = 22.dp),
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Color(0xFFFFC107),
                )
            }
        }
    }
}
