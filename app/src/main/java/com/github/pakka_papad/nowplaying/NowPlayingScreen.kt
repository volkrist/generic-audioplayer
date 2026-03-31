package com.github.pakka_papad.nowplaying

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.pakka_papad.R
import com.github.pakka_papad.data.UserPreferences.PlaybackParams
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.round
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import com.github.pakka_papad.nowplaying.RepeatMode as RepeatModeEnum
import com.github.pakka_papad.ui.theme.UiTokens

private val playbackSpeedPresets: List<Pair<String, Int>> = listOf(
    "0.75├Ч" to 75,
    "1├Ч" to 100,
    "1.25├Ч" to 125,
    "1.5├Ч" to 150,
    "2├Ч" to 200,
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NowPlayingScreen(
    paddingValues: PaddingValues,
    song: Song?,
    currentSongPlaying: Boolean?,
    onPausePlayPressed: () -> Unit,
    onPreviousPressed: () -> Unit,
    onNextPressed: () -> Unit,
    songPlaying: Boolean?,
    playerHelper: PlayerHelper,
    onFavouriteClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    repeatMode: RepeatModeEnum,
    toggleRepeatMode: () -> Unit,
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
    isTimerRunning: Boolean,
    timeLeft: Int,
    onTimerBegin: (Int) -> Unit,
    onTimerCancel: () -> Unit,
    onSaveQueueClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onEqualizerClicked: () -> Unit,
    onVolumeBoosterClicked: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenFolder: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    volumeBoosterPercent: Int,
    onVolumeBoosterPercentChange: (Int) -> Unit,
    onSettingsClicked: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit = {},
    onPlayerActionHideSong: (Song) -> Unit = {},
    onPlayerActionDeleteSong: (Song) -> Unit = {},
    onPlayerActionRingtone: (Song) -> Unit = {},
    onPlayerActionChangeCover: (Song) -> Unit = {},
) {
    if (song == null || songPlaying == null) return
    val configuration = LocalConfiguration.current
    val screenHeight = max(configuration.screenHeightDp - 20, 0)
    val screenWidth = configuration.screenWidthDp
    val artworkPalette = rememberNowPlayingArtworkPalette(song)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(paddingValues),
    ) {
        NowPlayingArtworkBackgroundLayer(palette = artworkPalette)
        if (configuration.orientation == ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
            val albumArtMaxWidth = ((0.48f) * screenWidth).toInt()
            if (albumArtMaxWidth >= 50 && screenHeight >= 50) {
                val imageSize = min(albumArtMaxWidth, screenHeight)
                AnimatedContent(
                    targetState = song.location,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.94f,
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                )
                            ) with (
                            fadeOut(animationSpec = tween(240)) +
                                scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = tween(240),
                                )
                            )
                    },
                    label = "npArtLand",
                ) {
                    AlbumArt(
                        song = song,
                        modifier = Modifier.size((imageSize * 0.92f).dp),
                    )
                }
            }
            InfoAndControls(
                song = song,
                onPausePlayPressed = onPausePlayPressed,
                onPreviousPressed = onPreviousPressed,
                onNextPressed = onNextPressed,
                showPlayButton = !songPlaying,
                playerHelper = playerHelper,
                currentSongPlaying = currentSongPlaying,
                onFavouriteClicked = onFavouriteClicked,
                onQueueClicked = onQueueClicked,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                repeatMode = repeatMode,
                toggleRepeatMode = toggleRepeatMode,
                playbackParams = playbackParams,
                updatePlaybackParams = updatePlaybackParams,
                isTimerRunning = isTimerRunning,
                timeLeft = timeLeft,
                onTimerBegin = onTimerBegin,
                onTimerCancel = onTimerCancel,
                onSaveQueueClicked = onSaveQueueClicked,
                onShuffleClicked = onShuffleClicked,
                onEqualizerClicked = onEqualizerClicked,
                onVolumeBoosterClicked = onVolumeBoosterClicked,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenFolder = onOpenFolder,
                onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                volumeBoosterPercent = volumeBoosterPercent,
                onVolumeBoosterPercentChange = onVolumeBoosterPercentChange,
                onSettingsClicked = onSettingsClicked,
                onPlayerActionEditTags = onPlayerActionEditTags,
                onPlayerActionHideSong = onPlayerActionHideSong,
                onPlayerActionDeleteSong = onPlayerActionDeleteSong,
                onPlayerActionRingtone = onPlayerActionRingtone,
                onPlayerActionChangeCover = onPlayerActionChangeCover,
            )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = song.location,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.94f,
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                )
                            ) with (
                            fadeOut(animationSpec = tween(240)) +
                                scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = tween(240),
                                )
                            )
                    },
                    label = "npArtPort",
                ) {
                    AlbumArt(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f),
                    )
                }
            }
            InfoAndControls(
                song = song,
                onPausePlayPressed = onPausePlayPressed,
                onPreviousPressed = onPreviousPressed,
                onNextPressed = onNextPressed,
                showPlayButton = !songPlaying,
                playerHelper = playerHelper,
                currentSongPlaying = currentSongPlaying,
                onFavouriteClicked = onFavouriteClicked,
                onQueueClicked = onQueueClicked,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                repeatMode = repeatMode,
                toggleRepeatMode = toggleRepeatMode,
                playbackParams = playbackParams,
                updatePlaybackParams = updatePlaybackParams,
                isTimerRunning = isTimerRunning,
                timeLeft = timeLeft,
                onTimerBegin = onTimerBegin,
                onTimerCancel = onTimerCancel,
                onSaveQueueClicked = onSaveQueueClicked,
                onShuffleClicked = onShuffleClicked,
                onEqualizerClicked = onEqualizerClicked,
                onVolumeBoosterClicked = onVolumeBoosterClicked,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenFolder = onOpenFolder,
                onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                volumeBoosterPercent = volumeBoosterPercent,
                onVolumeBoosterPercentChange = onVolumeBoosterPercentChange,
                onSettingsClicked = onSettingsClicked,
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

@Composable
private fun AlbumArt(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(UiTokens.cornerExtraLarge)
    val base = modifier
        .shadow(
            elevation = UiTokens.elevationNowPlayingArt,
            shape = shape,
            clip = false,
        )
        .clip(shape)
    if (song.artUri.isNullOrBlank()) {
        Box(
            modifier = base
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_outline_music_note_40),
                contentDescription = null,
                modifier = Modifier.size(UiTokens.artworkNowPlayingPlaceholder),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InfoAndControls(
    song: Song,
    currentSongPlaying: Boolean?,
    onPausePlayPressed: () -> Unit,
    onPreviousPressed: () -> Unit,
    onNextPressed: () -> Unit,
    showPlayButton: Boolean,
    playerHelper: PlayerHelper,
    onFavouriteClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    modifier: Modifier = Modifier,
    repeatMode: RepeatModeEnum,
    toggleRepeatMode: () -> Unit,
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
    isTimerRunning: Boolean,
    timeLeft: Int,
    onTimerBegin: (Int) -> Unit,
    onTimerCancel: () -> Unit,
    onSaveQueueClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onEqualizerClicked: () -> Unit,
    onVolumeBoosterClicked: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenFolder: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    volumeBoosterPercent: Int,
    onVolumeBoosterPercentChange: (Int) -> Unit,
    onSettingsClicked: () -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    var showPlayerActionsSheet by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiTokens.metaSpacingSmall),
        modifier = modifier.padding(vertical = UiTokens.rowSpacingComfort, horizontal = UiTokens.paddingSection),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showPlayerActionsSheet = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.player_actions_more),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        QuickActionsRow(
            onEqualizerClicked = onEqualizerClicked,
            onVolumeBoosterClicked = onVolumeBoosterClicked,
            onSaveQueueClicked = onSaveQueueClicked,
            onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
            sleepTimer = {
                SleepTimerButton(
                    isRunning = isTimerRunning,
                    timeLeft = timeLeft,
                    beginTimer = onTimerBegin,
                    cancelTimer = onTimerCancel,
                )
            },
        )
        PlaybackSpeedPresetsRow(
            playbackParams = playbackParams,
            updatePlaybackParams = updatePlaybackParams,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlaybackSpeedAndPitchController(
                playbackParams = playbackParams,
                updatePlaybackParams = updatePlaybackParams,
            )
        }
        AnimatedContent(
            targetState = song.location,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        slideInVertically { h -> h / 10 }
                    ) with (
                    fadeOut(animationSpec = tween(200)) +
                        slideOutVertically { h -> -h / 10 }
                    )
            },
            label = "npMeta",
        ) {
            SongInfo(
                song = song,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenFolder = onOpenFolder,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MusicSlider(
            modifier = Modifier.padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.smartSectionTitlePaddingBottom),
            playerHelper = playerHelper,
            currentSongPlaying = currentSongPlaying,
            duration = song.durationMillis,
            song = song,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiTokens.gridSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LikeButton(
                song = song,
                onFavouriteClicked = onFavouriteClicked,
                modifier = Modifier.weight(1f),
            )
            PreviousButton(
                onPreviousPressed = onPreviousPressed,
            )
            PausePlayButton(
                showPlayButton = showPlayButton,
                onPausePlayPressed = onPausePlayPressed,
            )
            NextButton(
                onNextPressed = onNextPressed,
            )
            QueueButton(
                onQueueClicked = onQueueClicked,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = UiTokens.smartSectionTitlePaddingBottom, bottom = UiTokens.paddingItem),
        ) {
            ShuffleButton(onClick = onShuffleClicked)
            RepeatModeController(
                currentRepeatMode = repeatMode,
                toggleRepeatMode = toggleRepeatMode,
            )
        }
    }
    if (showPlayerActionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlayerActionsSheet = false },
            sheetState = sheetState,
        ) {
            PlayerActionsSheetContent(
                onDismiss = { showPlayerActionsSheet = false },
                playbackParams = playbackParams,
                updatePlaybackParams = updatePlaybackParams,
                onQueueClicked = onQueueClicked,
                volumeBoosterPercent = volumeBoosterPercent,
                onVolumeBoosterPercentChange = onVolumeBoosterPercentChange,
                onVolumeBoosterOpenFull = onVolumeBoosterClicked,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                onSettingsClicked = onSettingsClicked,
                onOpenAlbum = onOpenAlbum,
                onPlayerActionEditTags = onPlayerActionEditTags,
                onPlayerActionHideSong = onPlayerActionHideSong,
                onPlayerActionDeleteSong = onPlayerActionDeleteSong,
                onPlayerActionRingtone = onPlayerActionRingtone,
                onPlayerActionChangeCover = onPlayerActionChangeCover,
                currentSong = song,
            )
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
    onQueueClicked: () -> Unit,
    volumeBoosterPercent: Int,
    onVolumeBoosterPercentChange: (Int) -> Unit,
    onVolumeBoosterOpenFull: () -> Unit,
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
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = UiTokens.actionSheetMaxHeight)
            .verticalScroll(scroll)
            .padding(bottom = UiTokens.paddingSheetBottom),
    ) {
        Text(
            text = stringResource(R.string.player_actions_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.queue_button)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_queue_music_40),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeSmall),
                )
            },
            modifier = Modifier.clickable {
                onQueueClicked()
                onDismiss()
            },
        )
        Divider()
        Text(
            text = stringResource(R.string.speed_and_pitch_controller),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = UiTokens.paddingScreen),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        ) {
            playbackSpeedPresets.forEach { (label, speedInt) ->
                val selected = playbackParams.playbackSpeed == speedInt
                FilterChip(
                    selected = selected,
                    onClick = {
                        updatePlaybackParams(speedInt, playbackParams.playbackPitch)
                    },
                    label = { Text(label) },
                )
            }
        }
        Divider()
        Text(
            text = stringResource(R.string.now_playing_volume_booster),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = UiTokens.paddingSheetHorizontal, vertical = UiTokens.paddingItem),
        )
        Text(
            text = stringResource(R.string.volume_booster_current, volumeBoosterPercent),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = UiTokens.paddingSheetHorizontal),
        )
        Slider(
            value = volumeBoosterPercent.toFloat(),
            onValueChange = { onVolumeBoosterPercentChange(it.toInt().coerceIn(100, 200)) },
            valueRange = 100f..200f,
            steps = 19,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingScreen),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_open_volume_booster_screen)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.baseline_speed_24),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeSmall),
                )
            },
            modifier = Modifier.clickable {
                onVolumeBoosterOpenFull()
                onDismiss()
            },
        )
        Divider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_keep_screen_on)) },
            supportingContent = {
                Text(stringResource(R.string.settings_keep_screen_on_desc))
            },
            trailingContent = {
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.iconSizeSmall),
                )
            },
            modifier = Modifier.clickable {
                onSettingsClicked()
                onDismiss()
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.now_playing_go_to_album)) },
            modifier = Modifier.clickable {
                onOpenAlbum()
                onDismiss()
            },
        )
        Divider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_edit_tags)) },
            modifier = Modifier.clickable {
                onPlayerActionEditTags(currentSong)
                onDismiss()
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_change_cover)) },
            modifier = Modifier.clickable {
                onPlayerActionChangeCover(currentSong)
                onDismiss()
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_hide_song)) },
            modifier = Modifier.clickable {
                onPlayerActionHideSong(currentSong)
                onDismiss()
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_delete_song)) },
            modifier = Modifier.clickable {
                onPlayerActionDeleteSong(currentSong)
                onDismiss()
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.player_action_ringtone)) },
            modifier = Modifier.clickable {
                onPlayerActionRingtone(currentSong)
                onDismiss()
            },
        )
    }
}

@Composable
private fun QuickActionsRow(
    onEqualizerClicked: () -> Unit,
    onVolumeBoosterClicked: () -> Unit,
    onSaveQueueClicked: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    sleepTimer: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UiTokens.paddingItem),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_piano_40),
            contentDescription = stringResource(R.string.now_playing_equalizer),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onEqualizerClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            painter = painterResource(R.drawable.baseline_speed_24),
            contentDescription = stringResource(R.string.now_playing_volume_booster),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onVolumeBoosterClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        sleepTimer()
        Icon(
            painter = painterResource(R.drawable.ic_baseline_playlist_play_40),
            contentDescription = stringResource(R.string.now_playing_add_to_playlist),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onAddCurrentSongToPlaylist,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_playlist_add_40),
            contentDescription = stringResource(R.string.save_queue_to_playlist),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onSaveQueueClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSpeedPresetsRow(
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = UiTokens.paddingSection),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        playbackSpeedPresets.forEach { (label, speedInt) ->
            val selected = playbackParams.playbackSpeed == speedInt
            FilterChip(
                selected = selected,
                onClick = {
                    updatePlaybackParams(speedInt, playbackParams.playbackPitch)
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun ShuffleButton(
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(R.drawable.ic_baseline_shuffle_40),
        contentDescription = stringResource(R.string.shuffle_button),
        modifier = Modifier
            .size(UiTokens.iconSizeTouch)
            .clip(RoundedCornerShape(UiTokens.cornerPill))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleMedium),
            )
            .padding(UiTokens.metaSpacingSmall),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    )
}

/**
 * All control buttons composable
 */
@Composable
private fun LikeButton(
    song: Song,
    onFavouriteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val favouriteButtonScale = remember { Animatable(1f) }
    Image(
        imageVector = if (song.favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = stringResource(R.string.favourite_button),
        modifier = modifier
            .size(UiTokens.iconSizeLarge)
            .scale(favouriteButtonScale.value)
            .clickable(
                onClick = {
                    onFavouriteClicked()
                    scope.launch {
                        favouriteButtonScale.animateTo(
                            targetValue = 1.2f, animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutLinearInEasing,
                            )
                        )
                        favouriteButtonScale.animateTo(
                            targetValue = 0.8f, animationSpec = tween(
                                durationMillis = 200,
                                easing = LinearEasing,
                            )
                        )
                        favouriteButtonScale.animateTo(
                            targetValue = 1f, animationSpec = tween(
                                durationMillis = 100,
                                easing = FastOutLinearInEasing,
                            )
                        )
                    }
                },
                indication = rememberRipple(
                    bounded = false, radius = UiTokens.rippleLarge
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(UiTokens.gridSpacing),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
private fun PreviousButton(
    onPreviousPressed: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(R.drawable.ic_baseline_skip_previous_40),
    contentDescription = stringResource(R.string.previous_button),
    modifier = modifier
        .size(UiTokens.playControlSize)
        .clip(CircleShape)
        .clickable(
            onClick = onPreviousPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = UiTokens.rippleHuge
            )
        )
        .padding(UiTokens.gridSpacing),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
)

@Composable
private fun PausePlayButton(
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(
        if (showPlayButton) R.drawable.ic_baseline_play_arrow_40 else R.drawable.ic_baseline_pause_40
    ),
    contentDescription = stringResource(
        if (showPlayButton) R.string.play_button else R.string.pause_button
    ),
    modifier = modifier
        .size(UiTokens.playControlSize)
        .clip(CircleShape)
        .clickable(
            onClick = onPausePlayPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = UiTokens.rippleHuge
            )
        )
        .background(
            color = MaterialTheme.colorScheme.primary, shape = CircleShape
        )
        .padding(UiTokens.gridSpacing),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
)

@Composable
private fun NextButton(
    onNextPressed: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(R.drawable.ic_baseline_skip_next_40),
    contentDescription = stringResource(R.string.next_button),
    modifier = modifier
        .size(UiTokens.playControlSize)
        .clip(CircleShape)
        .clickable(
            onClick = onNextPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = UiTokens.rippleHuge
            )
        )
        .padding(UiTokens.gridSpacing),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
)

@Composable
private fun QueueButton(
    onQueueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(R.drawable.ic_baseline_queue_music_40),
    contentDescription = stringResource(R.string.queue_button),
    modifier = modifier
        .size(UiTokens.iconSizeLarge)
        .clickable(
            onClick = onQueueClicked,
            indication = rememberRipple(
                bounded = false, radius = UiTokens.rippleLarge
            ),
            interactionSource = remember { MutableInteractionSource() }
        )
        .padding(UiTokens.gridSpacing),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
)

@Composable
private fun SongInfo(
    song: Song,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacerModifier = Modifier.height(UiTokens.metaSpacingSmall)
    val albumOpenable = song.album.isNotBlank() && song.album != "Unknown"
    val artistOpenable = song.artist.isNotBlank() && song.artist != "Unknown"
    val parentFolder = remember(song.location) { File(song.location).parentFile }
    val folderOpenable = parentFolder != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = UiTokens.paddingSection),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(spacerModifier)
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = if (artistOpenable) TextDecoration.Underline else null,
            modifier = Modifier
                .then(
                    if (artistOpenable) {
                        Modifier.clickable(
                            onClick = onOpenArtist,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true),
                        )
                    } else {
                        Modifier
                    }
                ),
        )
        Spacer(spacerModifier)
        Text(
            text = song.album,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (albumOpenable) TextDecoration.Underline else null,
            modifier = Modifier
                .then(
                    if (albumOpenable) {
                        Modifier.clickable(
                            onClick = onOpenAlbum,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true),
                        )
                    } else {
                        Modifier
                    }
                ),
        )
        if (folderOpenable && parentFolder != null) {
            Spacer(Modifier.height(UiTokens.paddingItem))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(UiTokens.cornerXs))
                    .clickable(
                        onClick = onOpenFolder,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true),
                    )
                    .padding(horizontal = UiTokens.paddingItem, vertical = UiTokens.smartSectionTitlePaddingBottom),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_folder_40),
                    contentDescription = null,
                    modifier = Modifier.size(UiTokens.artworkThumbSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(UiTokens.metaSpacingSmall))
                Text(
                    text = parentFolder.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PlaybackSpeedAndPitchController(
    playbackParams: PlaybackParams,
    updatePlaybackParams: (speed: Int, pitch: Int) -> Unit,
) {
    val speed = playbackParams.playbackSpeed
    val pitch = playbackParams.playbackPitch
    var showDialog by remember { mutableStateOf(false) }
    Icon(
        painter = painterResource(R.drawable.ic_outline_music_note_40),
        contentDescription = stringResource(R.string.speed_and_pitch_controller),
        modifier = Modifier
            .size(UiTokens.iconSizeProminent)
            .clickable(
                onClick = { showDialog = true },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = UiTokens.rippleSmall)
            ),
        tint = MaterialTheme.colorScheme.onSurface
    )
    if (showDialog) {
        var newSpeed by remember { mutableStateOf((speed.toFloat() / 100).round(2)) }
        var newPitch by remember { mutableStateOf((pitch.toFloat() / 100).round(2)) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        updatePlaybackParams(
                            newSpeed.times(100).toInt(),
                            newPitch.times(100).toInt()
                        )
                    },
                    content = {
                        Text(text = stringResource(R.string.save))
                    }
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    content = {
                        Text(text = stringResource(R.string.cancel))
                    }
                )
            },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.speed_x, newSpeed),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                        TextButton(
                            onClick = { newSpeed = 1f },
                            content = {
                                Text(text = stringResource(R.string.reset))
                            },
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                    Slider(
                        value = newSpeed,
                        onValueChange = { newSpeed = it.round(2) },
                        valueRange = 0.01f..2.0f,
                        steps = 20,
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.pitch_x, newPitch),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                        TextButton(
                            onClick = { newPitch = 1f },
                            content = {
                                Text(text = stringResource(R.string.reset))
                            },
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                    Slider(
                        value = newPitch,
                        onValueChange = { newPitch = it.round(2) },
                        valueRange = 0.01f..2.0f,
                        steps = 20,
                    )
                }
            }
        )
    }
}

@Composable
fun RepeatModeController(
    currentRepeatMode: RepeatModeEnum,
    toggleRepeatMode: () -> Unit,
) {
    Icon(
        painter = painterResource(currentRepeatMode.iconResource),
        contentDescription = stringResource(R.string.repeat_mode_button),
        modifier = Modifier
            .size(UiTokens.iconSizeTouch)
            .clip(RoundedCornerShape(UiTokens.cornerPill))
            .clickable(
                onClick = toggleRepeatMode,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleMedium),
            )
            .padding(UiTokens.metaSpacingSmall),
        tint = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SleepTimerButton(
    isRunning: Boolean,
    timeLeft: Int,
    beginTimer: (Int) -> Unit,
    cancelTimer: () -> Unit,
) {
    var showTimerDialog by remember { mutableStateOf(false) }
    Icon(
        painter = painterResource(R.drawable.outline_timer_24),
        contentDescription = stringResource(R.string.sleep_timer_button),
        modifier = Modifier
            .size(UiTokens.iconSizeMedium)
            .clip(RoundedCornerShape(UiTokens.cornerXs))
            .clickable(
                onClick = { showTimerDialog = true },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
            )
            .padding(UiTokens.paddingItemTight),
        tint = MaterialTheme.colorScheme.onSurface
    )
    if (showTimerDialog) {
        var minutes by remember { mutableStateOf<Int?>(null) }
        var seconds by remember { mutableStateOf<Int?>(null) }
        val time by remember(timeLeft) {
            derivedStateOf {
                val mins = timeLeft / 60
                val secs = timeLeft % 60
                val sMinutes = if (mins < 10) "0$mins" else mins.toString()
                val sSeconds = if (secs < 10) "0$secs" else secs.toString()
                "$sMinutes:$sSeconds"
            }
        }
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UiTokens.paddingSection),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRunning) {
                        Text(
                            text = stringResource(R.string.stopping_in, time),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = minutes?.toString() ?: "",
                                onValueChange = {
                                    if (it.length > 2) return@OutlinedTextField
                                    minutes = try {
                                        if (it.isEmpty()) null else it.toInt()
                                    } catch (_: Exception) {
                                        minutes
                                    }
                                },
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = {
                                    Text(text = "mm")
                                },
                                textStyle = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .width(UiTokens.sleepTimerFieldWidth)
                            )
                            Text(
                                text = ":",
                                modifier = Modifier
                                    .width(UiTokens.sleepTimerSeparatorWidth),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            OutlinedTextField(
                                value = seconds?.toString() ?: "",
                                onValueChange = {
                                    if (it.length > 2) return@OutlinedTextField
                                    if (it.isEmpty()) {
                                        seconds = null
                                        return@OutlinedTextField
                                    }
                                    val num = try {
                                        it.toInt()
                                    } catch (_: Exception) {
                                        seconds
                                    }
                                    if (num != null && num > 59) return@OutlinedTextField
                                    seconds = num
                                },
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = {
                                    Text(text = "ss")
                                },
                                textStyle = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .width(UiTokens.sleepTimerFieldWidth)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRunning) {
                            cancelTimer()
                        } else {
                            beginTimer((minutes ?: 0) * 60 + (seconds ?: 0))
                        }
                        showTimerDialog = false
                    },
                    content = {
                        Text(text = stringResource(if (isRunning) R.string.stop_timer else R.string.start_timer))
                    }
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTimerDialog = false },
                    content = {
                        Text(text = stringResource(R.string.close))
                    }
                )
            },
        )
    }
}
