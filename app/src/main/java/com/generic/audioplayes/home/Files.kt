package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.components.MiniSongCard
import com.generic.audioplayes.components.more_options.SongOptions
import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.storage_explorer.Directory
import com.generic.audioplayes.storage_explorer.DirectoryContents
import com.generic.audioplayes.ui.theme.UiTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Files(
    contents: DirectoryContents,
    onDirectoryClicked: (Directory) -> Unit,
    onSongClicked: (index: Int) -> Unit,
    currentSong: Song?,
    onAddToPlaylistClicked: (MiniSong) -> Unit,
    onAddToQueueClicked: (MiniSong) -> Unit,
    onFolderPlay: (Directory) -> Unit,
    onFolderPlayNext: (Directory) -> Unit,
    onFolderAddToQueue: (Directory) -> Unit,
    onFolderAddToPlaylist: (Directory) -> Unit,
    onFolderHide: (Directory) -> Unit,
    onFolderDelete: (Directory) -> Unit,
    /** Same track count as when opening a folder: [com.generic.audioplayes.data.music.SongExtractor.extractMini]. */
    folderTrackCount: (String) -> Int,
    /** When set, ⋮ opens the library track sheet (resolved to [Song] in [HomeFragment]). */
    onTrackOverflowClick: ((MiniSong) -> Unit)? = null,
) {
    if (contents.directories.isEmpty() && contents.songs.isEmpty()) {
        FullScreenSadMessage(stringResource(R.string.nothing_here))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = UiTokens.paddingScreen),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        items(
            items = contents.directories,
            key = { it.absolutePath },
        ) {
            Folder(
                folder = it,
                onDirectoryClicked = onDirectoryClicked,
                folderTrackCount = folderTrackCount,
                onPlay = { onFolderPlay(it) },
                onPlayNext = { onFolderPlayNext(it) },
                onAddToQueue = { onFolderAddToQueue(it) },
                onAddToPlaylist = { onFolderAddToPlaylist(it) },
                onHideFolder = { onFolderHide(it) },
                onDeleteFolderFromDevice = { onFolderDelete(it) },
            )
        }
        itemsIndexed(
            items = contents.songs,
            key = { index, song -> song.location },
        ) { index, song ->
            MiniSongCard(
                song = song,
                onSongClicked = { onSongClicked(index) },
                songOptions = listOf(
                    SongOptions.AddToPlaylist { onAddToPlaylistClicked(song) },
                    SongOptions.AddToQueue { onAddToQueueClicked(song) },
                ),
                onOverflowClick = onTrackOverflowClick?.let { cb -> { cb(song) } },
                currentlyPlaying = (song.location == currentSong?.location),
                shellListStyle = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderActionsBottomSheet(
    folderName: String,
    songCountLabel: String?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHideFolder: () -> Unit,
    onDeleteMenuItem: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(
            topStart = UiTokens.sheetCornerTopLarge,
            topEnd = UiTokens.sheetCornerTopLarge,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UiTokens.paddingSheetBottom),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_folder_40),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.artworkMedium),
                    tint = Color(0xFFFFC107),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    songCountLabel?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Divider()
            FolderSheetRow(
                icon = R.drawable.ic_baseline_play_arrow_40,
                label = stringResource(R.string.folder_action_play),
                onClick = {
                    onDismiss()
                    onPlay()
                },
            )
            FolderSheetRow(
                icon = R.drawable.ic_baseline_playlist_play_40,
                label = stringResource(R.string.folder_action_play_next),
                onClick = {
                    onDismiss()
                    onPlayNext()
                },
            )
            FolderSheetRow(
                icon = R.drawable.ic_baseline_queue_music_40,
                label = stringResource(R.string.folder_action_add_queue),
                onClick = {
                    onDismiss()
                    onAddToQueue()
                },
            )
            FolderSheetRow(
                icon = R.drawable.ic_baseline_playlist_add_40,
                label = stringResource(R.string.folder_action_add_playlist),
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                },
            )
            Divider()
            FolderSheetRow(
                icon = R.drawable.ic_baseline_remove_circle_40,
                label = stringResource(R.string.folder_action_hide),
                onClick = {
                    onDismiss()
                    onHideFolder()
                },
            )
            FolderSheetRow(
                icon = R.drawable.ic_baseline_playlist_remove_40,
                label = stringResource(R.string.folder_action_delete),
                onClick = {
                    onDismiss()
                    onDeleteMenuItem()
                },
            )
            Spacer(modifier = Modifier.height(UiTokens.paddingItem))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.paddingScreen),
                shape = RoundedCornerShape(UiTokens.cornerPill),
            ) {
                Text(stringResource(R.string.folder_sheet_close))
            }
        }
    }
}

@Composable
private fun FolderSheetRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.cornerSmall))
            .clickable(
                onClick = onClick,
                indication = rememberRipple(radius = 160.dp),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(
                horizontal = UiTokens.paddingSheetHorizontal,
                vertical = UiTokens.paddingItem,
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(UiTokens.iconSizeMedium),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folder(
    folder: Directory,
    onDirectoryClicked: (Directory) -> Unit,
    folderTrackCount: (String) -> Int,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHideFolder: () -> Unit,
    onDeleteFolderFromDevice: () -> Unit,
) {
    val resource = painterResource(R.drawable.ic_baseline_folder_40)
    var showClickIndicator by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var songCount by remember(folder.absolutePath) { mutableStateOf<Int?>(null) }
    LaunchedEffect(folder.absolutePath) {
        songCount = withContext(Dispatchers.IO) {
            folderTrackCount(folder.absolutePath)
        }
    }
    val songCountLabel = songCount?.let { formatTrackCountRu(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onDirectoryClicked(folder)
                showClickIndicator = true
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = resource,
            contentDescription = stringResource(R.string.folder_icon),
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFFC107),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            songCount?.let { n ->
                Text(
                    text = formatTrackCountRu(n),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                )
            }
        }
        if (showClickIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = Modifier
                    .size(26.dp)
                    .clickable(
                        onClick = {
                            showFolderMenu = true
                        },
                        indication = rememberRipple(
                            bounded = false,
                            radius = 20.dp,
                        ),
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
    if (showFolderMenu) {
        FolderActionsBottomSheet(
            folderName = folder.name,
            songCountLabel = songCountLabel,
            onDismiss = { showFolderMenu = false },
            onPlay = onPlay,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onHideFolder = onHideFolder,
            onDeleteMenuItem = { showDeleteConfirm = true },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.folder_delete_confirm_title)) },
            text = { Text(stringResource(R.string.folder_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteFolderFromDevice()
                    },
                ) {
                    Text(stringResource(R.string.folder_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.songs_sort_cancel))
                }
            },
        )
    }
}

private fun formatTrackCountRu(n: Int): String = when {
    n % 100 in 11..14 -> "$n песен"
    n % 10 == 1 -> "$n песня"
    n % 10 in 2..4 -> "$n песни"
    else -> "$n песен"
}
