package com.generic.audioplayes.nowplaying

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.PLAYBACK_MULTIPLIER_MAX
import com.generic.audioplayes.PLAYBACK_MULTIPLIER_MIN
import com.generic.audioplayes.PLAYBACK_PARAM_MAX_PERCENT
import com.generic.audioplayes.PLAYBACK_PARAM_MIN_PERCENT
import com.generic.audioplayes.data.UserPreferences.PlaybackParams
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.snapPlaybackParamPercent
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.util.Stage4DebugLog
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.roundToInt

private val volumeBoosterPresets: List<Int> = listOf(125, 150, 175, 200)

private val playerSheetVolumeThumbColor = Color(0xFFFF9800)

private val playerActionsSheetBg = Color(0xFF21212B)
private val playerActionsSheetText = Color(0xFFF3F4F6)
private val playerActionsSheetMuted = Color(0xFF9CA3AF)
private val playerActionsSheetDivider = Color(0xFF3D3D48)
private val playerActionsSheetCell = Color(0xFF2A2D36)

/**
 * Track actions menu (ringtone, tags, speed, volume, …). Uses [Dialog], not [androidx.compose.material3.ModalBottomSheet],
 * so it never shares the same window path as the queue sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionsSheetModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    song: Song,
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
    volumeBoosterPercent: Int,
    onVolumeBoosterPercentChange: (Int) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    if (!visible) return
    LaunchedEffect(song.location) {
        Stage4DebugLog.i("PlayerActionsSheetModal opened path=${song.location}")
    }
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(
                    topStart = UiTokens.sheetCornerTopLarge,
                    topEnd = UiTokens.sheetCornerTopLarge,
                ),
                color = scheme.surfaceVariant,
                contentColor = scheme.onSurface,
            ) {
                PlayerActionsSheetContent(
                    currentSong = song,
                    onDismiss = onDismiss,
                    playbackParams = playbackParams,
                    updatePlaybackParams = updatePlaybackParams,
                    volumeBoosterPercent = volumeBoosterPercent,
                    onVolumeBoosterPercentChange = onVolumeBoosterPercentChange,
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnChange = onKeepScreenOnChange,
                    onSettingsClicked = onSettingsClicked,
                    onOpenAlbum = onOpenAlbum,
                    onPlayerActionEditTags = onPlayerActionEditTags,
                    onPlayerActionHideSong = onPlayerActionHideSong,
                    onPlayerActionDeleteSong = onPlayerActionDeleteSong,
                    onPlayerActionRingtone = onPlayerActionRingtone,
                    onPlayerActionChangeCover = onPlayerActionChangeCover,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerActionsSheetContent(
    currentSong: Song,
    onDismiss: () -> Unit,
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
    volumeBoosterPercent: Int,
    onVolumeBoosterPercentChange: (Int) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
    onOpenAlbum: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    val context = LocalContext.current
    var showSongInfoDialog by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val speedDisplay = remember(playbackParams.playbackSpeed) {
        val v = playbackParams.playbackSpeed / 100f
        String.format(Locale.getDefault(), "%.2f", v).replace('.', ',') + "x"
    }
    if (showSongInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSongInfoDialog = false },
            title = { Text(currentSong.title) },
            text = {
                Text(
                    buildString {
                        appendLine(currentSong.artist)
                        appendLine(currentSong.album)
                        appendLine(currentSong.location)
                        appendLine(
                            "${currentSong.durationFormatted} · ${formatBitrateKbps(currentSong.bitrate)} · ${currentSong.size}",
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
    val listColors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
        headlineColor = playerActionsSheetText,
        supportingColor = playerActionsSheetMuted,
        leadingIconColor = playerActionsSheetText,
        trailingIconColor = playerActionsSheetText,
    )
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = playerActionsSheetText,
        selectedContainerColor = playerSheetVolumeThumbColor.copy(alpha = 0.22f),
        selectedLabelColor = playerSheetVolumeThumbColor,
        iconColor = playerActionsSheetMuted,
        disabledContainerColor = Color.Transparent,
        disabledLabelColor = playerActionsSheetMuted,
        disabledLeadingIconColor = playerActionsSheetMuted,
    )
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
                    .background(playerActionsSheetMuted.copy(alpha = 0.45f)),
            )
        }
        PlayerActionsSheetTrackHeader(
            song = currentSong,
            onInfoClick = { showSongInfoDialog = true },
            onShareClick = {
                val path = currentSong.location
                if (path.isNotBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        val uri = Uri.fromFile(file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = currentSong.mimeType?.takeIf { it.startsWith("audio/") } ?: "audio/*"
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
        Divider(color = playerActionsSheetDivider, modifier = Modifier.padding(vertical = UiTokens.paddingItemTight))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItem),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        ) {
            PlayerActionSheetGridButton(
                label = stringResource(R.string.player_action_ringtone),
                icon = Icons.Outlined.Notifications,
                onClick = {
                    onPlayerActionRingtone(currentSong)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            PlayerActionSheetGridButton(
                label = stringResource(R.string.player_action_change_cover),
                icon = Icons.Outlined.Image,
                onClick = {
                    onPlayerActionChangeCover(currentSong)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            PlayerActionSheetGridButton(
                label = stringResource(R.string.player_action_edit_tags),
                icon = Icons.Outlined.EditNote,
                onClick = {
                    Stage4DebugLog.i("Edit tags clicked trackPath=${currentSong.location} (PlayerActionsSheetModal)")
                    onPlayerActionEditTags(currentSong)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItemTight),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        ) {
            PlayerActionSheetGridButton(
                label = stringResource(R.string.now_playing_go_to_album),
                icon = Icons.Outlined.Album,
                onClick = {
                    onOpenAlbum()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            PlayerActionSheetGridButton(
                label = stringResource(R.string.player_action_hide_song),
                icon = Icons.Outlined.VisibilityOff,
                onClick = {
                    onPlayerActionHideSong(currentSong)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            PlayerActionSheetGridButton(
                label = stringResource(R.string.player_action_delete_song),
                icon = Icons.Outlined.Delete,
                onClick = {
                    onPlayerActionDeleteSong(currentSong)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
        }
        Divider(color = playerActionsSheetDivider, modifier = Modifier.padding(vertical = UiTokens.paddingItemTight))
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.player_sheet_speed),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.baseline_speed_24),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeMedium),
                    tint = playerActionsSheetText,
                )
            },
            trailingContent = {
                Text(
                    text = speedDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    color = playerSheetVolumeThumbColor,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            colors = listColors,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItemTight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        ) {
            Slider(
                value = playbackParams.playbackSpeed
                    .coerceIn(PLAYBACK_PARAM_MIN_PERCENT, PLAYBACK_PARAM_MAX_PERCENT) / 100f,
                onValueChange = { raw ->
                    val snapped = (kotlin.math.round(raw * 100.0) / 100.0).toFloat()
                        .coerceIn(PLAYBACK_MULTIPLIER_MIN, PLAYBACK_MULTIPLIER_MAX)
                    updatePlaybackParams(
                        snapPlaybackParamPercent(snapped),
                        playbackParams.playbackPitch,
                    )
                },
                valueRange = PLAYBACK_MULTIPLIER_MIN..PLAYBACK_MULTIPLIER_MAX,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = playerSheetVolumeThumbColor,
                    activeTrackColor = playerSheetVolumeThumbColor,
                    inactiveTrackColor = playerActionsSheetDivider,
                ),
            )
        }
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.player_sheet_playback_settings),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = {
                Text(
                    stringResource(R.string.player_sheet_playback_settings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = playerActionsSheetMuted,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeMedium),
                    tint = playerActionsSheetText,
                )
            },
            colors = listColors,
            modifier = Modifier.clickable {
                onSettingsClicked()
                onDismiss()
            },
        )
        Divider(color = playerActionsSheetDivider, modifier = Modifier.padding(vertical = UiTokens.paddingItemTight))
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.settings_keep_screen_on),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.baseline_light_mode_40),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeMedium),
                    tint = playerActionsSheetText,
                )
            },
            trailingContent = {
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = playerSheetVolumeThumbColor,
                        checkedTrackColor = playerSheetVolumeThumbColor.copy(alpha = 0.45f),
                        uncheckedThumbColor = playerActionsSheetMuted,
                        uncheckedTrackColor = playerActionsSheetCell,
                    ),
                )
            },
            colors = listColors,
        )
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeMedium),
                    tint = playerActionsSheetText,
                )
            },
            colors = listColors,
            modifier = Modifier.clickable {
                onSettingsClicked()
                onDismiss()
            },
        )
        Divider(color = playerActionsSheetDivider, modifier = Modifier.padding(vertical = UiTokens.paddingItemTight))
        Text(
            text = stringResource(R.string.now_playing_volume_booster),
            style = MaterialTheme.typography.titleMedium,
            color = playerActionsSheetText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up_24),
                contentDescription = null,
                modifier = Modifier.size(UiTokens.iconSizeMedium),
                tint = playerActionsSheetText,
            )
            Slider(
                value = volumeBoosterPercent.toFloat(),
                onValueChange = { onVolumeBoosterPercentChange(it.toInt().coerceIn(100, 200)) },
                valueRange = 100f..200f,
                steps = 19,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = playerSheetVolumeThumbColor,
                    activeTrackColor = playerSheetVolumeThumbColor,
                    inactiveTrackColor = playerActionsSheetDivider,
                ),
            )
            Text(
                text = "${volumeBoosterPercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = playerActionsSheetText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 44.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItem),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        ) {
            volumeBoosterPresets.forEach { percent ->
                val selected = volumeBoosterPercent == percent
                FilterChip(
                    selected = selected,
                    onClick = { onVolumeBoosterPercentChange(percent) },
                    label = { Text("${percent}%", maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    colors = chipColors,
                )
            }
        }
    }
}

private fun formatBitrateKbps(bitrate: Float): String {
    if (bitrate <= 0f) return "—"
    val kbps = if (bitrate >= 1000f) bitrate / 1000f else bitrate
    return "${kbps.roundToInt()} kbps"
}

@Composable
private fun PlayerActionsSheetTrackHeader(
    song: Song,
    onInfoClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val metaLine = remember(song.artist, song.durationFormatted, song.bitrate) {
        buildString {
            append(song.artist)
            append(" | ")
            append(song.durationFormatted)
            append(" | ")
            append(formatBitrateKbps(song.bitrate))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerActionsSheetThumb(song = song)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = playerActionsSheetText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = playerActionsSheetMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.player_action_song_info),
                tint = playerActionsSheetText,
            )
        }
        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.player_action_share),
                tint = playerActionsSheetText,
            )
        }
    }
}

@Composable
private fun PlayerActionsSheetThumb(
    song: Song,
) {
    val shape = RoundedCornerShape(UiTokens.cornerSmall)
    val base = Modifier
        .size(UiTokens.artworkMedium)
        .clip(shape)
    if (song.artUri.isNullOrBlank()) {
        Box(
            modifier = base.background(playerActionsSheetCell),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_outline_music_note_40),
                contentDescription = null,
                modifier = Modifier.size(UiTokens.iconSizeMedium),
                colorFilter = ColorFilter.tint(playerActionsSheetMuted),
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
private fun PlayerActionSheetGridButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.cornerMedium))
            .background(playerActionsSheetCell)
            .clickable(onClick = onClick)
            .padding(vertical = UiTokens.paddingItem, horizontal = UiTokens.paddingItemTight),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(UiTokens.iconSizeMedium),
            tint = playerActionsSheetText,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = playerActionsSheetText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
