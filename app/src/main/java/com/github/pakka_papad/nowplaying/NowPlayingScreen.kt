package com.github.pakka_papad.nowplaying

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

private val playbackSpeedPresets: List<Pair<String, Int>> = listOf(
    "0.75×" to 75,
    "1×" to 100,
    "1.25×" to 125,
    "1.5×" to 150,
    "2×" to 200,
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
) {
    if (song == null || songPlaying == null) return
    val configuration = LocalConfiguration.current
    val screenHeight = max(configuration.screenHeightDp - 20, 0)
    val screenWidth = configuration.screenWidthDp
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surface,
        ),
    )
    if (configuration.orientation == ORIENTATION_LANDSCAPE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(paddingValues),
        ) {
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
            )
        }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(paddingValues),
        ) {
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
    val shape = RoundedCornerShape(20.dp)
    val base = modifier
        .shadow(
            elevation = 20.dp,
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
                modifier = Modifier.size(96.dp),
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
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        SongLyricsTabRow()
        AnimatedContent(
            targetState = song.location,
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
        ReferenceQuickActionsRow(
            song = song,
            onFavouriteClicked = onFavouriteClicked,
            onAddCurrentSongToPlaylist = onAddCurrentSongToPlaylist,
            onEqualizerClicked = onEqualizerClicked,
            onVolumeBoosterClicked = onVolumeBoosterClicked,
            onSaveQueueClicked = onSaveQueueClicked,
            onQueueClicked = onQueueClicked,
            sleepTimer = {
                SleepTimerButton(
                    isRunning = isTimerRunning,
                    timeLeft = timeLeft,
                    beginTimer = onTimerBegin,
                    cancelTimer = onTimerCancel,
                )
            },
        )
        MusicSlider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            playerHelper = playerHelper,
            currentSongPlaying = currentSongPlaying,
            duration = song.durationMillis,
            song = song,
        )
        SeekSkipRow(playerHelper = playerHelper)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            ShuffleButton(onClick = onShuffleClicked)
            PreviousButton(onPreviousPressed = onPreviousPressed)
            PausePlayButton(
                showPlayButton = showPlayButton,
                onPausePlayPressed = onPausePlayPressed,
            )
            NextButton(onNextPressed = onNextPressed)
            RepeatModeController(
                currentRepeatMode = repeatMode,
                toggleRepeatMode = toggleRepeatMode,
            )
        }
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
    }
}

@Composable
private fun SongLyricsTabRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.now_playing_tab_song),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Text(
            text = " | ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.now_playing_tab_lyrics),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .alpha(0.45f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.now_playing_lyrics_placeholder),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ReferenceQuickActionsRow(
    song: Song,
    onFavouriteClicked: () -> Unit,
    onAddCurrentSongToPlaylist: () -> Unit,
    onEqualizerClicked: () -> Unit,
    onVolumeBoosterClicked: () -> Unit,
    onSaveQueueClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    sleepTimer: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactFavoriteButton(song = song, onFavouriteClicked = onFavouriteClicked)
        Icon(
            painter = painterResource(R.drawable.ic_baseline_playlist_play_40),
            contentDescription = stringResource(R.string.now_playing_add_to_playlist),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onAddCurrentSongToPlaylist,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 20.dp),
                )
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_piano_40),
            contentDescription = stringResource(R.string.now_playing_equalizer),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onEqualizerClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 20.dp),
                )
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        sleepTimer()
        Icon(
            painter = painterResource(R.drawable.ic_baseline_queue_music_40),
            contentDescription = stringResource(R.string.queue_button),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onQueueClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 20.dp),
                )
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            painter = painterResource(R.drawable.baseline_speed_24),
            contentDescription = stringResource(R.string.now_playing_volume_booster),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onVolumeBoosterClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 20.dp),
                )
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_playlist_add_40),
            contentDescription = stringResource(R.string.save_queue_to_playlist),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onSaveQueueClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 20.dp),
                )
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompactFavoriteButton(
    song: Song,
    onFavouriteClicked: () -> Unit,
) {
    Image(
        imageVector = if (song.favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = stringResource(R.string.favourite_button),
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = onFavouriteClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 20.dp),
            )
            .padding(4.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    )
}

@Composable
private fun SeekSkipRow(playerHelper: PlayerHelper) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.seek_back_10),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = { playerHelper.seekRelative(-10_000L) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.seek_forward_10),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = { playerHelper.seekRelative(10_000L) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 22.dp),
            )
            .padding(6.dp),
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
            .size(50.dp)
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
                    bounded = false, radius = 25.dp
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(10.dp),
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
        .size(70.dp)
        .clip(RoundedCornerShape(35.dp))
        .clickable(
            onClick = onPreviousPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = 35.dp
            )
        )
        .padding(10.dp),
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
        .size(70.dp)
        .clip(CircleShape)
        .clickable(
            onClick = onPausePlayPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = 35.dp
            )
        )
        .background(
            color = MaterialTheme.colorScheme.primary, shape = CircleShape
        )
        .padding(10.dp),
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
        .size(70.dp)
        .clip(RoundedCornerShape(35.dp))
        .clickable(
            onClick = onNextPressed,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = true, radius = 35.dp
            )
        )
        .padding(10.dp),
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
        .size(50.dp)
        .clickable(
            onClick = onQueueClicked,
            indication = rememberRipple(
                bounded = false, radius = 25.dp
            ),
            interactionSource = remember { MutableInteractionSource() }
        )
        .padding(10.dp),
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
    val spacerModifier = Modifier.height(6.dp)
    val albumOpenable = song.album.isNotBlank() && song.album != "Unknown"
    val artistOpenable = song.artist.isNotBlank() && song.artist != "Unknown"
    val parentFolder = remember(song.location) { File(song.location).parentFile }
    val folderOpenable = parentFolder != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 12.dp),
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
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = onOpenFolder,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_folder_40),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
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
            .size(28.dp)
            .clickable(
                onClick = { showDialog = true },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 20.dp)
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
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                onClick = toggleRepeatMode,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 22.dp),
            )
            .padding(6.dp),
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
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = { showTimerDialog = true },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 20.dp),
            )
            .padding(4.dp),
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
                        .padding(vertical = 12.dp),
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
                                    .width(80.dp)
                            )
                            Text(
                                text = ":",
                                modifier = Modifier
                                    .width(12.dp),
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
                                    .width(80.dp)
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
