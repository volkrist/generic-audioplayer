package com.generic.audioplayes.nowplaying

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.util.Locale
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.UserPreferences.PlaybackParams
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.PLAYBACK_MULTIPLIER_MAX
import com.generic.audioplayes.PLAYBACK_MULTIPLIER_MIN
import com.generic.audioplayes.percentToPlaybackMultiplier
import com.generic.audioplayes.snapPlaybackParamPercent
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.generic.audioplayes.nowplaying.RepeatMode as RepeatModeEnum
import com.generic.audioplayes.ui.theme.UiTokens

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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
    /** Opens existing sleep timer screen (same as drawer / nav). */
    onSleepTimerClicked: () -> Unit,
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
    onCollapseFullPlayer: () -> Unit = {},
    /** Parent shows [PlayerActionsSheetModal] when overflow is tapped (same layer as [QueueBottomSheetModal]). */
    onPlayerOverflowMenuClick: () -> Unit = {},
    onThemeClicked: () -> Unit = {},
) {
    if (song == null || songPlaying == null) return
    val configuration = LocalConfiguration.current
    val screenHeight = max(configuration.screenHeightDp - 20, 0)
    val screenWidth = configuration.screenWidthDp
    val fullPlayerContentColor = Color.White
    val fullPlayerMuted = Color.White.copy(alpha = 0.88f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .union(WindowInsets.navigationBars),
            )
            .padding(paddingValues),
    ) {
        NowPlayingFullPlayerBackground()
        if (configuration.orientation == ORIENTATION_LANDSCAPE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = UiTokens.nowPlayingScreenHorizontalPadding),
            ) {
                NowPlayingFullTopBar(
                    onCollapse = onCollapseFullPlayer,
                    onOpenPlayerActionsMenu = onPlayerOverflowMenuClick,
                    onThemeClicked = onThemeClicked,
                    contentColor = fullPlayerContentColor,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingScreen),
                ) {
                    val albumArtMaxWidth = ((0.40f) * screenWidth).toInt()
                    Box(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
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
                                    modifier = Modifier.size((imageSize * 0.88f).dp),
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NowPlayingTrackTitleBlock(
                            song = song,
                            onOpenArtist = onOpenArtist,
                            titleColor = fullPlayerContentColor,
                            subtitleColor = fullPlayerMuted,
                        )
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
                            modifier = Modifier.fillMaxWidth(),
                            repeatMode = repeatMode,
                            toggleRepeatMode = toggleRepeatMode,
                            onSleepTimerClicked = onSleepTimerClicked,
                            onShuffleClicked = onShuffleClicked,
                            onEqualizerClicked = onEqualizerClicked,
                            onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
                            iconTint = fullPlayerContentColor,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = UiTokens.nowPlayingScreenHorizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                NowPlayingFullTopBar(
                    onCollapse = onCollapseFullPlayer,
                    onOpenPlayerActionsMenu = onPlayerOverflowMenuClick,
                    onThemeClicked = onThemeClicked,
                    contentColor = fullPlayerContentColor,
                )
                Box(
                    modifier = Modifier
                        .weight(1.05f)
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
                                .fillMaxWidth(0.82f)
                                .aspectRatio(1f),
                        )
                    }
                }
                NowPlayingTrackTitleBlock(
                    song = song,
                    onOpenArtist = onOpenArtist,
                    titleColor = fullPlayerContentColor,
                    subtitleColor = fullPlayerMuted,
                )
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
                    onSleepTimerClicked = onSleepTimerClicked,
                    onShuffleClicked = onShuffleClicked,
                    onEqualizerClicked = onEqualizerClicked,
                    onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
                    iconTint = fullPlayerContentColor,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingFullTopBar(
    onCollapse: () -> Unit,
    onOpenPlayerActionsMenu: () -> Unit,
    onThemeClicked: () -> Unit,
    contentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(32f)
            .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItemTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.now_playing_collapse_full_player),
                tint = contentColor,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.now_playing_tab_song),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = " | ",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.now_playing_tab_lyrics),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.copy(alpha = 0.5f),
            )
        }
        IconButton(onClick = onThemeClicked) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = stringResource(R.string.drawer_graphic_theme),
                tint = contentColor,
                modifier = Modifier.size(UiTokens.iconSizeMedium),
            )
        }
        IconButton(onClick = onOpenPlayerActionsMenu) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.player_actions_more),
                tint = contentColor,
            )
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

@Composable
private fun NowPlayingFullPlayerBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF472B6),
                            Color(0xFF9333EA),
                            Color(0xFF0F172A),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f)),
        )
    }
}

@Composable
private fun NowPlayingTrackTitleBlock(
    song: Song,
    onOpenArtist: () -> Unit,
    titleColor: Color,
    subtitleColor: Color,
) {
    val artistOpenable = song.artist.isNotBlank() && song.artist != "Unknown"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.paddingScreen, vertical = UiTokens.paddingItem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(UiTokens.paddingItemTight))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = subtitleColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (artistOpenable) TextDecoration.Underline else null,
            modifier = Modifier.clickable(
                enabled = artistOpenable,
                onClick = onOpenArtist,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
            ),
        )
    }
}

@Composable
private fun NowPlayingSecondaryActionsRow(
    song: Song,
    onFavouriteClicked: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    onEqualizerClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.paddingItem),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LikeButton(
            song = song,
            onFavouriteClicked = onFavouriteClicked,
            modifier = Modifier.size(UiTokens.iconSizeTouch),
            iconTint = iconTint,
        )
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
            tint = iconTint,
        )
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
            tint = iconTint,
        )
        Icon(
            painter = painterResource(R.drawable.outline_timer_24),
            contentDescription = stringResource(R.string.sleep_timer_button),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onSleepTimerClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = iconTint,
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_queue_music_40),
            contentDescription = stringResource(R.string.queue_button),
            modifier = Modifier
                .size(UiTokens.iconSizeMedium)
                .clip(RoundedCornerShape(UiTokens.cornerXs))
                .clickable(
                    onClick = onQueueClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleSmall),
                )
                .padding(UiTokens.paddingItemTight),
            tint = iconTint,
        )
    }
}

@Composable
private fun MusicSliderWithSeekRow(
    playerHelper: PlayerHelper,
    currentSongPlaying: Boolean?,
    song: Song,
    durationMillis: Long,
    timeLabelColor: Color,
    seekLabelColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.paddingItem, vertical = UiTokens.smartSectionTitlePaddingBottom),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItemTight),
    ) {
        Text(
            text = stringResource(R.string.seek_back_10),
            style = MaterialTheme.typography.labelLarge,
            color = seekLabelColor,
            modifier = Modifier
                .clip(RoundedCornerShape(UiTokens.cornerSmall))
                .clickable(
                    onClick = { playerHelper.seekRelative(-10_000L) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                )
                .padding(horizontal = UiTokens.paddingItemTight, vertical = UiTokens.paddingItemTight),
        )
        MusicSlider(
            modifier = Modifier.weight(1f),
            playerHelper = playerHelper,
            currentSongPlaying = currentSongPlaying,
            duration = durationMillis,
            song = song,
            timeLabelColor = timeLabelColor,
        )
        Text(
            text = stringResource(R.string.seek_forward_10),
            style = MaterialTheme.typography.labelLarge,
            color = seekLabelColor,
            modifier = Modifier
                .clip(RoundedCornerShape(UiTokens.cornerSmall))
                .clickable(
                    onClick = { playerHelper.seekRelative(10_000L) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                )
                .padding(horizontal = UiTokens.paddingItemTight, vertical = UiTokens.paddingItemTight),
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
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
    onShuffleClicked: () -> Unit,
    onEqualizerClicked: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    iconTint: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        modifier = modifier.padding(vertical = UiTokens.paddingItem),
    ) {
        NowPlayingSecondaryActionsRow(
            song = song,
            onFavouriteClicked = onFavouriteClicked,
            onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
            onEqualizerClicked = onEqualizerClicked,
            onQueueClicked = onQueueClicked,
            onSleepTimerClicked = onSleepTimerClicked,
            iconTint = iconTint,
        )
        MusicSliderWithSeekRow(
            playerHelper = playerHelper,
            currentSongPlaying = currentSongPlaying,
            song = song,
            durationMillis = song.durationMillis,
            timeLabelColor = iconTint,
            seekLabelColor = iconTint,
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.paddingItem, vertical = UiTokens.paddingItemTight),
        ) {
            ShuffleButton(onClick = onShuffleClicked, iconTint = iconTint)
            PreviousButton(
                onPreviousPressed = onPreviousPressed,
                iconTint = iconTint,
            )
            PausePlayButton(
                showPlayButton = showPlayButton,
                onPausePlayPressed = onPausePlayPressed,
            )
            NextButton(
                onNextPressed = onNextPressed,
                iconTint = iconTint,
            )
            RepeatModeController(
                currentRepeatMode = repeatMode,
                toggleRepeatMode = toggleRepeatMode,
                iconTint = iconTint,
            )
        }
    }
}

@Composable
private fun ShuffleButton(
    onClick: () -> Unit,
    iconTint: Color? = null,
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
        colorFilter = ColorFilter.tint(iconTint ?: MaterialTheme.colorScheme.onSurface),
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
    iconTint: Color? = null,
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
        colorFilter = ColorFilter.tint(iconTint ?: MaterialTheme.colorScheme.onSurface),
    )
}

@Composable
private fun PreviousButton(
    onPreviousPressed: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
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
    colorFilter = ColorFilter.tint(iconTint ?: MaterialTheme.colorScheme.onSurface),
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
    iconTint: Color? = null,
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
    colorFilter = ColorFilter.tint(iconTint ?: MaterialTheme.colorScheme.onSurface),
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
        var newSpeed by remember {
            mutableStateOf(percentToPlaybackMultiplier(speed))
        }
        var newPitch by remember {
            mutableStateOf(percentToPlaybackMultiplier(pitch))
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        updatePlaybackParams(
                            snapPlaybackParamPercent(newSpeed),
                            snapPlaybackParamPercent(newPitch),
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
                            text = stringResource(
                                R.string.speed_x,
                                String.format(Locale.US, "%.2f", newSpeed),
                            ),
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
                        onValueChange = { raw ->
                            newSpeed = (kotlin.math.round(raw * 100.0) / 100.0).toFloat()
                                .coerceIn(PLAYBACK_MULTIPLIER_MIN, PLAYBACK_MULTIPLIER_MAX)
                        },
                        valueRange = PLAYBACK_MULTIPLIER_MIN..PLAYBACK_MULTIPLIER_MAX,
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pitch_x,
                                String.format(Locale.US, "%.2f", newPitch),
                            ),
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
                        onValueChange = { raw ->
                            newPitch = (kotlin.math.round(raw * 100.0) / 100.0).toFloat()
                                .coerceIn(PLAYBACK_MULTIPLIER_MIN, PLAYBACK_MULTIPLIER_MAX)
                        },
                        valueRange = PLAYBACK_MULTIPLIER_MIN..PLAYBACK_MULTIPLIER_MAX,
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
    iconTint: Color? = null,
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
        tint = iconTint ?: MaterialTheme.colorScheme.onSurface,
    )
}
