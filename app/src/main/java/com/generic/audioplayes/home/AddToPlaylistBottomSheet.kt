package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.PlaylistWithSongCount
import com.generic.audioplayes.ui.theme.UiTokens

private val sheetBg = Color(0xFF1E222E)
private val sheetMuted = Color.White.copy(alpha = 0.62f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistFromPlayerSheet(
    playlists: List<PlaylistWithSongCount>,
    favouritesCount: Int,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onFavouritesClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = UiTokens.sheetCornerTopLarge,
            topEnd = UiTokens.sheetCornerTopLarge,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = UiTokens.paddingSheetBottom),
        ) {
            Text(
                text = stringResource(R.string.songs_select_add_playlist),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = UiTokens.paddingSheetHorizontal,
                    vertical = UiTokens.paddingItem,
                ),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = UiTokens.actionSheetMaxHeight),
            ) {
                item {
                    CreateNewPlaylistSheetRow(
                        onCreatePlaylist = onCreatePlaylist,
                    )
                }
                item {
                    FavouritesSheetRow(
                        countText = stringResource(R.string.playlist_songs_count_fmt, favouritesCount),
                        onClick = {
                            onFavouritesClick()
                            onDismiss()
                        },
                    )
                }
                items(
                    items = playlists,
                    key = { it.playlistId },
                ) { playlist ->
                    PlaylistPickRow(
                        playlist = playlist,
                        onClick = {
                            onAddToPlaylist(playlist.playlistId)
                            onDismiss()
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiTokens.paddingItem))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.paddingSheetHorizontal),
                shape = RoundedCornerShape(UiTokens.cornerPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(R.string.folder_sheet_close),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CreateNewPlaylistSheetRow(
    onCreatePlaylist: (String) -> Unit,
) {
    var dialogVisible by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .clickable(
                onClick = { dialogVisible = true },
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleLarge),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(UiTokens.playlistListRowArt)
                .clip(RoundedCornerShape(UiTokens.cornerMedium))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(UiTokens.smartTileIcon),
                tint = Color.White.copy(alpha = 0.95f),
            )
        }
        Text(
            text = stringResource(R.string.add_to_playlist_sheet_create_new),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
    if (dialogVisible) {
        AlertDialog(
            onDismissRequest = { dialogVisible = false },
            confirmButton = {
                Button(
                    onClick = {
                        val name = playlistName.trim()
                        if (name.isNotEmpty()) {
                            dialogVisible = false
                            onCreatePlaylist(name)
                            playlistName = ""
                        }
                    },
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        dialogVisible = false
                        playlistName = ""
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.create_playlist)) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                )
            },
        )
    }
}

@Composable
private fun FavouritesSheetRow(
    countText: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .clickable(
                onClick = onClick,
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleLarge),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(UiTokens.playlistListRowArt)
                .clip(RoundedCornerShape(UiTokens.cornerMedium))
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primary, scheme.primaryContainer),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
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
                text = stringResource(R.string.playlist_smart_favourites),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = sheetMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaylistPickRow(
    playlist: PlaylistWithSongCount,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .clickable(
                onClick = onClick,
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleLarge),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = playlist.artUri,
            contentDescription = stringResource(R.string.playlist_art),
            modifier = Modifier
                .size(UiTokens.playlistListRowArt)
                .clip(RoundedCornerShape(UiTokens.cornerMedium))
                .background(Color.White.copy(alpha = 0.12f)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.textLineGapTight),
        ) {
            Text(
                text = playlist.playlistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.playlist_songs_count_fmt, playlist.count),
                style = MaterialTheme.typography.bodySmall,
                color = sheetMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
