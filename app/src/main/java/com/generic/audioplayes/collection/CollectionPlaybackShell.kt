package com.generic.audioplayes.collection

import android.app.PendingIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.rememberSwipeableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.generic.audioplayes.R
import com.generic.audioplayes.components.BottomSheet
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.home.HomeNavHelper
import com.generic.audioplayes.home.HomeViewModel
import com.generic.audioplayes.home.MiniPlayer
import com.generic.audioplayes.nowplaying.NowPlayingScreen
import com.generic.audioplayes.nowplaying.PlayerActionsSheetModal
import com.generic.audioplayes.nowplaying.PlayerHelper
import com.generic.audioplayes.nowplaying.QueueBottomSheetModal
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.util.AudioFileActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CollectionPlaybackOverlay {
    None,
    Queue,
    Actions,
}

/**
 * Same bottom playback chrome as [com.generic.audioplayes.home.HomeFragment] (mini player, swipe-up full
 * player, queue & overflow actions) so collection / artist / smart playlist screens keep the mini player.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CollectionPlaybackShell(
    homeViewModel: HomeViewModel,
    preferenceProvider: AudioPlayerPreferenceProvider,
    playerHelper: PlayerHelper,
    navHelper: HomeNavHelper,
    pendingPreviousIntent: PendingIntent,
    pendingNextIntent: PendingIntent,
    content: @Composable (scaffoldBottomPadding: Dp) -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val swipeableState = rememberSwipeableState(initialValue = 0)
    var fullPlayerOverlay by remember { mutableStateOf(CollectionPlaybackOverlay.None) }

    val currentSong by homeViewModel.currentSong.collectAsStateWithLifecycle()
    val songPlaying by homeViewModel.currentSongPlaying.collectAsStateWithLifecycle()
    val queue = homeViewModel.queue
    val repeatMode by homeViewModel.repeatMode.collectAsStateWithLifecycle()
    val playbackParams by preferenceProvider.playbackParams.collectAsStateWithLifecycle()
    val keepScreenOn by preferenceProvider.keepScreenOn.collectAsStateWithLifecycle()
    val volumeBoosterPercent by preferenceProvider.volumeBoosterPercent.collectAsStateWithLifecycle()
    val widgetStyle by preferenceProvider.widgetStyle.collectAsStateWithLifecycle()

    val miniPlayerPeekProgress by remember {
        derivedStateOf {
            if (swipeableState.progress.from == 0) {
                if (swipeableState.progress.to == 0) 1f
                else if (swipeableState.progress.fraction < 0.25f) 1f - swipeableState.progress.fraction * 4
                else 0f
            } else {
                if (swipeableState.progress.to == 1) 0f
                else if (swipeableState.progress.fraction > 0.75f) {
                    1f - (1f - swipeableState.progress.fraction) * 4
                } else {
                    0f
                }
            }
        }
    }
    val libraryBehindPlayerDimAlpha by remember {
        derivedStateOf {
            if (currentSong == null) 0f
            else (1f - miniPlayerPeekProgress) * UiTokens.libraryBehindPlayerDimAlpha
        }
    }
    val homeScreenBottomPadding by remember(currentSong) {
        derivedStateOf {
            if (currentSong == null) {
                12.dp
            } else {
                12.dp + HomeLibraryTokens.miniPlayerPeekHeight
            }
        }
    }
    val windowInsets = WindowInsets.systemBars.asPaddingValues()

    val miniPlayerPlayPauseClicked = remember {
        {
            if (swipeableState.currentValue == 0) {
                homeViewModel.onMiniPlayerPlayPause()
            }
        }
    }
    val expandQueueBottomSheet: () -> Unit = remember {
        { fullPlayerOverlay = CollectionPlaybackOverlay.Queue }
    }
    val expandFullPlayer: () -> Unit = remember(scope, swipeableState) {
        {
            scope.launch { swipeableState.animateTo(1) }
        }
    }
    val collapseFullPlayer: () -> Unit = remember(scope, swipeableState) {
        {
            scope.launch { swipeableState.animateTo(0) }
        }
    }

    BackHandler(
        enabled = swipeableState.currentValue == 1 ||
            fullPlayerOverlay != CollectionPlaybackOverlay.None,
    ) {
        when {
            fullPlayerOverlay != CollectionPlaybackOverlay.None ->
                fullPlayerOverlay = CollectionPlaybackOverlay.None
            swipeableState.currentValue == 1 ->
                scope.launch { swipeableState.animateTo(0) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content(homeScreenBottomPadding)

        if (libraryBehindPlayerDimAlpha > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = libraryBehindPlayerDimAlpha)),
            )
        }
        BottomSheet(
            peekHeight = homeScreenBottomPadding + windowInsets.calculateBottomPadding(),
            peekContent = {
                val scheme = MaterialTheme.colorScheme
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = windowInsets.calculateStartPadding(layoutDirection),
                            end = windowInsets.calculateEndPadding(layoutDirection),
                        ),
                ) {
                    if (currentSong != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val s = UiTokens.miniPlayerPeekScaleCollapsed +
                                        (UiTokens.miniPlayerPeekScaleExpanded - UiTokens.miniPlayerPeekScaleCollapsed) *
                                        miniPlayerPeekProgress
                                    scaleX = s
                                    scaleY = s
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                },
                            shape = RoundedCornerShape(
                                topStart = UiTokens.sheetCornerTopSmall,
                                topEnd = UiTokens.sheetCornerTopSmall,
                            ),
                            shadowElevation = UiTokens.elevationSurface * miniPlayerPeekProgress,
                            tonalElevation = UiTokens.elevationTonalLow,
                            color = Color.Transparent,
                        ) {
                            Column {
                                MiniPlayer(
                                    onPausePlayPressed = miniPlayerPlayPauseClicked,
                                    onQueueClick = expandQueueBottomSheet,
                                    song = currentSong,
                                    playerHelper = playerHelper,
                                    showPlayButton = songPlaying == false,
                                    widgetStyle = widgetStyle,
                                    modifier = Modifier.fillMaxWidth(),
                                    onExpandPlayer = expandFullPlayer,
                                )
                                var progress by remember { mutableStateOf(0f) }
                                DisposableEffect(currentSong) {
                                    progress = if (playerHelper.duration > 0) {
                                        playerHelper.currentPosition / playerHelper.duration
                                    } else {
                                        0f
                                    }
                                    val listener = object : Player.Listener {
                                        override fun onMediaItemTransition(
                                            mediaItem: MediaItem?,
                                            reason: Int,
                                        ) {
                                            super.onMediaItemTransition(mediaItem, reason)
                                            progress = 0f
                                        }
                                    }
                                    playerHelper.addListener(listener)
                                    onDispose {
                                        playerHelper.removeListener(listener)
                                    }
                                }
                                if (songPlaying == true && swipeableState.currentValue == 0) {
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            progress = if (playerHelper.duration > 0) {
                                                (playerHelper.currentPosition / playerHelper.duration)
                                                    .coerceIn(0f, 1f)
                                            } else {
                                                0f
                                            }
                                            delay(40)
                                        }
                                    }
                                }
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(UiTokens.progressBarThin),
                                    progress = progress.coerceIn(0f, 1f),
                                    color = scheme.primary,
                                    trackColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            },
            content = {
                currentSong?.let { playingSong ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NowPlayingScreen(
                            paddingValues = PaddingValues(0.dp),
                            song = playingSong,
                            onPlayerOverflowMenuClick = {
                                fullPlayerOverlay = CollectionPlaybackOverlay.Actions
                            },
                            onThemeClicked = navHelper::navigateToTheme,
                            onCollapseFullPlayer = collapseFullPlayer,
                            onPausePlayPressed = homeViewModel::onMiniPlayerPlayPause,
                            onPreviousPressed = pendingPreviousIntent::send,
                            onNextPressed = pendingNextIntent::send,
                            songPlaying = songPlaying,
                            playerHelper = playerHelper,
                            currentSongPlaying = songPlaying,
                            onFavouriteClicked = homeViewModel::changeFavouriteValue,
                            onQueueClicked = expandQueueBottomSheet,
                            repeatMode = repeatMode,
                            toggleRepeatMode = homeViewModel::toggleRepeatMode,
                            playbackParams = playbackParams,
                            updatePlaybackParams = preferenceProvider::updatePlaybackParams,
                            onSleepTimerClicked = navHelper::navigateToSleepTimer,
                            onSaveQueueClicked = { navHelper.navigateToChoosePlaylist(queue) },
                            onShuffleClicked = { homeViewModel.shufflePlay(queue.toList()) },
                            onEqualizerClicked = navHelper::navigateToEqualizer,
                            onVolumeBoosterClicked = navHelper::navigateToVolumeBooster,
                            onOpenAlbum = { navHelper.navigateToAlbumByName(playingSong.album) },
                            onOpenArtist = { navHelper.navigateToArtistByName(playingSong.artist) },
                            onOpenFolder = {
                                java.io.File(playingSong.location).parentFile?.let { dir ->
                                    homeViewModel.navigateToFolderInExplorer(dir.absolutePath)
                                }
                            },
                            onAddCurrentSongToPlaylist = {
                                navHelper.navigateToChoosePlaylist(playingSong)
                            },
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnChange = preferenceProvider::updateKeepScreenOn,
                            volumeBoosterPercent = volumeBoosterPercent,
                            onVolumeBoosterPercentChange = preferenceProvider::updateVolumeBoosterPercent,
                            onSettingsClicked = navHelper::navigateToSettings,
                            onPlayerActionEditTags = navHelper::navigateToTagEditor,
                            onPlayerActionHideSong = homeViewModel::onSongBlacklist,
                            onPlayerActionDeleteSong = homeViewModel::deleteSongFromDevice,
                            onPlayerActionRingtone = { song ->
                                if (AudioFileActions.setAsRingtone(context, song.location)) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.player_ringtone_ok),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.player_ringtone_failed),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            onPlayerActionChangeCover = { song ->
                                if (!AudioFileActions.tryOpenAudioForCoverChange(
                                        context,
                                        song.location,
                                    )
                                ) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.player_action_no_editor_app),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                        )
                    }
                }
            },
            swipeableState = swipeableState,
        )

        currentSong?.let { playingSong ->
            QueueBottomSheetModal(
                visible = fullPlayerOverlay == CollectionPlaybackOverlay.Queue,
                onDismiss = { fullPlayerOverlay = CollectionPlaybackOverlay.None },
                queue = queue,
                onFavouriteClicked = homeViewModel::changeFavouriteValue,
                currentSong = playingSong,
                playerHelper = playerHelper,
                onDrag = homeViewModel::onSongDrag,
                onQueueSongPlayNext = homeViewModel::moveQueueSongToPlayNext,
                onQueueSongAddToPlaylist = navHelper::navigateToChoosePlaylist,
                onQueueSongRemoveFromQueue = homeViewModel::removeSongFromQueue,
                onQueueSongOpenAlbum = { s -> navHelper.navigateToAlbumByName(s.album) },
                onQueueSongEditTags = navHelper::navigateToTagEditor,
                onQueueSongHide = homeViewModel::onSongBlacklist,
                onQueueSongDelete = homeViewModel::deleteSongFromDevice,
                onQueueSongRingtone = { song ->
                    if (AudioFileActions.setAsRingtone(context, song.location)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_ringtone_ok),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_ringtone_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onQueueSongChangeCover = navHelper::navigateToTagEditor,
            )
            PlayerActionsSheetModal(
                visible = fullPlayerOverlay == CollectionPlaybackOverlay.Actions,
                onDismiss = { fullPlayerOverlay = CollectionPlaybackOverlay.None },
                song = playingSong,
                playbackParams = playbackParams,
                updatePlaybackParams = preferenceProvider::updatePlaybackParams,
                volumeBoosterPercent = volumeBoosterPercent,
                onVolumeBoosterPercentChange = preferenceProvider::updateVolumeBoosterPercent,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = preferenceProvider::updateKeepScreenOn,
                onSettingsClicked = {
                    fullPlayerOverlay = CollectionPlaybackOverlay.None
                    navHelper.navigateToSettings()
                },
                onOpenAlbum = { navHelper.navigateToAlbumByName(playingSong.album) },
                onPlayerActionEditTags = { song ->
                    if (!AudioFileActions.tryOpenAudioTagEditor(
                            context,
                            song.location,
                        )
                    ) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_action_no_editor_app),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onPlayerActionHideSong = homeViewModel::onSongBlacklist,
                onPlayerActionDeleteSong = homeViewModel::deleteSongFromDevice,
                onPlayerActionRingtone = { song ->
                    if (AudioFileActions.setAsRingtone(context, song.location)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_ringtone_ok),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_ringtone_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onPlayerActionChangeCover = navHelper::navigateToTagEditor,
            )
        }
    }
}
