package com.generic.audioplayes.nowplaying

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSongActionsBottomSheet(
    song: Song,
    visible: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    if (!visible) return
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
        QueueSongActionsSheetContent(
            song = song,
            onDismiss = onDismiss,
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveFromQueue = onRemoveFromQueue,
            onOpenAlbum = onOpenAlbum,
            onPlayerActionEditTags = onPlayerActionEditTags,
            onPlayerActionHideSong = onPlayerActionHideSong,
            onPlayerActionDeleteSong = onPlayerActionDeleteSong,
            onPlayerActionRingtone = onPlayerActionRingtone,
            onPlayerActionChangeCover = onPlayerActionChangeCover,
        )
    }
}

/**
 * Same track actions layout as the queue / folder reference (header, 2×3 grid, list rows) for Home → Songs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLibrarySongActionsBottomSheet(
    song: Song,
    visible: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
    /** When non-null (e.g. user playlist / editor), shows an extra list row after «Добавить в плейлист». */
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBg = Color(0xFF21212B)
    val sheetFg = Color(0xFFF3F4F6)
    val divider = Color(0xFF3D3D48)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = sheetFg,
        shape = RoundedCornerShape(
            topStart = UiTokens.sheetCornerTopLarge,
            topEnd = UiTokens.sheetCornerTopLarge,
        ),
    ) {
        SongTrackActionsSheetContent(
            song = song,
            onDismiss = onDismiss,
            onOpenAlbum = onOpenAlbum,
            onPlayerActionEditTags = onPlayerActionEditTags,
            onPlayerActionHideSong = onPlayerActionHideSong,
            onPlayerActionDeleteSong = onPlayerActionDeleteSong,
            onPlayerActionRingtone = onPlayerActionRingtone,
            onPlayerActionChangeCover = onPlayerActionChangeCover,
            dragHandleColor = sheetFg.copy(alpha = 0.45f),
            dividerColor = divider,
            gridCellBackground = Color(0xFF2A2D36),
        ) {
            Divider(color = divider)
            QueueSongSheetRow(
                icon = R.drawable.ic_baseline_playlist_play_40,
                label = stringResource(R.string.folder_action_play_next),
                onClick = {
                    onDismiss()
                    onPlayNext()
                },
            )
            Divider(color = divider)
            QueueSongSheetRow(
                icon = R.drawable.ic_baseline_queue_music_40,
                label = stringResource(R.string.folder_action_add_queue),
                onClick = {
                    onDismiss()
                    onAddToQueue()
                },
            )
            Divider(color = divider)
            QueueSongSheetRow(
                icon = R.drawable.ic_baseline_playlist_add_40,
                label = stringResource(R.string.folder_action_add_playlist),
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                },
            )
            if (onRemoveFromPlaylist != null) {
                Divider(color = divider)
                QueueSongSheetRow(
                    icon = R.drawable.ic_baseline_playlist_remove_40,
                    label = stringResource(R.string.track_action_remove_from_playlist),
                    onClick = {
                        onDismiss()
                        onRemoveFromPlaylist.invoke()
                    },
                )
            }
        }
    }
}

@Composable
private fun QueueSongActionsSheetContent(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    val dividerMuted = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    SongTrackActionsSheetContent(
        song = song,
        onDismiss = onDismiss,
        onOpenAlbum = onOpenAlbum,
        onPlayerActionEditTags = onPlayerActionEditTags,
        onPlayerActionHideSong = onPlayerActionHideSong,
        onPlayerActionDeleteSong = onPlayerActionDeleteSong,
        onPlayerActionRingtone = onPlayerActionRingtone,
        onPlayerActionChangeCover = onPlayerActionChangeCover,
        dragHandleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        dividerColor = dividerMuted,
        gridCellBackground = null,
    ) {
        Divider(color = dividerMuted)
        QueueSongSheetRow(
            icon = R.drawable.ic_baseline_playlist_play_40,
            label = stringResource(R.string.folder_action_play_next),
            onClick = {
                onDismiss()
                onPlayNext()
            },
        )
        Divider(color = dividerMuted)
        QueueSongSheetRow(
            icon = R.drawable.ic_baseline_playlist_add_40,
            label = stringResource(R.string.folder_action_add_playlist),
            onClick = {
                onDismiss()
                onAddToPlaylist()
            },
        )
        Divider(color = dividerMuted)
        QueueSongSheetRow(
            icon = R.drawable.ic_baseline_remove_circle_40,
            label = stringResource(R.string.queue_track_remove_from_queue),
            onClick = {
                onDismiss()
                onRemoveFromQueue()
            },
        )
    }
}

@Composable
private fun SongTrackActionsSheetContent(
    song: Song,
    onDismiss: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
    dragHandleColor: Color,
    dividerColor: Color,
    gridCellBackground: Color?,
    bottomRows: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var showSongInfoDialog by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    if (showSongInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSongInfoDialog = false },
            title = { Text(song.title) },
            text = {
                Text(
                    buildString {
                        appendLine(song.artist)
                        appendLine(song.album)
                        appendLine(song.location)
                        appendLine(
                            "${song.durationFormatted} · ${formatQueueSheetBitrateKbps(song.bitrate)} · ${song.size}",
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showSongInfoDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = UiTokens.actionSheetMaxHeight)
            .verticalScroll(scroll)
            .padding(bottom = UiTokens.paddingSheetBottom),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = UiTokens.paddingItemTight, bottom = UiTokens.paddingItem),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(UiTokens.queueSheetDragHandleWidth)
                    .height(UiTokens.queueSheetDragHandleHeight)
                    .clip(RoundedCornerShape(UiTokens.queueSheetDragHandleCorner))
                    .background(dragHandleColor),
            )
        }
        QueueSongSheetHeader(
            song = song,
            onInfoClick = { showSongInfoDialog = true },
            onShareClick = {
                val path = song.location
                if (path.isNotBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        val uri = Uri.fromFile(file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = song.mimeType?.takeIf { it.startsWith("audio/") } ?: "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, null))
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.player_sheet_share_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_sheet_share_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
        Divider(color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItem),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        ) {
            QueueSongActionGridCell(
                label = stringResource(R.string.player_action_ringtone),
                painter = painterResource(R.drawable.ic_baseline_library_music_40),
                onClick = {
                    onPlayerActionRingtone(song)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
            QueueSongActionGridCell(
                label = stringResource(R.string.player_action_change_cover),
                painter = painterResource(R.drawable.baseline_palette_40),
                onClick = {
                    onPlayerActionChangeCover(song)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
            QueueSongActionGridCell(
                label = stringResource(R.string.player_action_edit_tags),
                painter = painterResource(R.drawable.baseline_bug_report_40),
                onClick = {
                    onPlayerActionEditTags(song)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItemTight),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        ) {
            QueueSongActionGridCell(
                label = stringResource(R.string.now_playing_go_to_album),
                painter = painterResource(R.drawable.ic_outline_album_40),
                onClick = {
                    onOpenAlbum()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
            QueueSongActionGridCell(
                label = stringResource(R.string.player_action_hide_song),
                painter = painterResource(R.drawable.ic_baseline_remove_circle_40),
                onClick = {
                    onPlayerActionHideSong(song)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
            QueueSongActionGridCell(
                label = stringResource(R.string.player_action_delete_song),
                painter = painterResource(R.drawable.ic_baseline_playlist_remove_40),
                onClick = {
                    onPlayerActionDeleteSong(song)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                cellBackground = gridCellBackground,
            )
        }
        bottomRows()
    }
}

@Composable
private fun QueueSongSheetHeader(
    song: Song,
    onInfoClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val contentColor = LocalContentColor.current
    val metaLine = remember(song.artist, song.durationFormatted, song.bitrate) {
        buildString {
            append(song.artist)
            append(" | ")
            append(song.durationFormatted)
            append(" | ")
            append(formatQueueSheetBitrateKbps(song.bitrate))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueSongSheetThumb(song = song)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.65f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.player_action_song_info),
                tint = contentColor,
            )
        }
        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.player_action_share),
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun QueueSongSheetThumb(song: Song) {
    val shape = RoundedCornerShape(UiTokens.cornerSmall)
    val thumbBg = LocalContentColor.current.copy(alpha = 0.14f)
    val thumbIcon = LocalContentColor.current.copy(alpha = 0.45f)
    val base = Modifier
        .size(UiTokens.artworkMedium)
        .clip(shape)
    if (song.artUri.isNullOrBlank()) {
        Box(
            modifier = base.background(thumbBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_outline_music_note_40),
                contentDescription = null,
                modifier = Modifier.size(UiTokens.iconSizeMedium),
                colorFilter = ColorFilter.tint(thumbIcon),
            )
        }
    } else {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = base,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun QueueSongActionGridCell(
    label: String,
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cellBackground: Color? = null,
) {
    val contentColor = LocalContentColor.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.cornerMedium))
            .then(
                if (cellBackground != null) Modifier.background(cellBackground) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = UiTokens.paddingItem, horizontal = UiTokens.paddingItemTight),
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(UiTokens.iconSizeMedium),
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QueueSongSheetRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val contentColor = LocalContentColor.current
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
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )
    }
}

private fun formatQueueSheetBitrateKbps(bitrate: Float): String {
    if (bitrate <= 0f) return "—"
    val kbps = if (bitrate >= 1000f) bitrate / 1000f else bitrate
    return "${kbps.roundToInt()}kbps"
}
