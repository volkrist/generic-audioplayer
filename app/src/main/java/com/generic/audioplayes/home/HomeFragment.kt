package com.generic.audioplayes.home

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.Constants
import com.generic.audioplayes.R
import com.generic.audioplayes.Screens
import com.generic.audioplayes.components.BottomSheet
import com.generic.audioplayes.components.Snackbar
import com.generic.audioplayes.components.getSortOptions
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.nowplaying.HomeLibrarySongActionsBottomSheet
import com.generic.audioplayes.nowplaying.NowPlayingScreen
import com.generic.audioplayes.nowplaying.PlayerActionsSheetModal
import com.generic.audioplayes.nowplaying.PlayerHelper
import com.generic.audioplayes.nowplaying.QueueBottomSheetModal
import com.generic.audioplayes.player.AudioPlayerBroadcastReceiver
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.LibraryShellBackdrop
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.generic.audioplayes.util.AudioFileActions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Single overlay for full-player sheets so queue and track actions can never both be active. */
private enum class FullPlayerOverlay {
    None,
    Queue,
    Actions,
}

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var navController: NavController

    private val viewModel: HomeViewModel by activityViewModels()

    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider
    @Inject lateinit var songExtractor: SongExtractor

    @OptIn(
        ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
        ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = findNavController()
        val pendingPreviousIntent = PendingIntent.getBroadcast(
            context, AudioPlayerBroadcastReceiver.PREVIOUS_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_PREVIOUS
            ),
            PendingIntent.FLAG_IMMUTABLE
        )
        val pendingNextIntent = PendingIntent.getBroadcast(
            context, AudioPlayerBroadcastReceiver.NEXT_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_NEXT
            ),
            PendingIntent.FLAG_IMMUTABLE
        )

        val navHelper = HomeNavHelper(navController, lifecycle)
        val playerHelper = PlayerHelper(exoPlayer)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val systemUiController = rememberSystemUiController()
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference, systemUiController) {
                    LaunchedEffect(Unit) {
                        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                    }
                    val context = LocalContext.current
                    val selectedTabs by preferenceProvider.selectedTabs.collectAsStateWithLifecycle()
                    var currentScreen by rememberSaveable { mutableStateOf(Screens.Songs) }
                    val switchToFoldersTab by viewModel.switchToFoldersTab.collectAsStateWithLifecycle()
                    LaunchedEffect(switchToFoldersTab) {
                        if (switchToFoldersTab) {
                            currentScreen = Screens.Folders
                            viewModel.consumeSwitchToFoldersTab()
                        }
                    }
                    val scope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    val sortOrder by preferenceProvider.sortOrder.collectAsStateWithLifecycle()

                    val songs by viewModel.songs.collectAsStateWithLifecycle()
                    val allSongsListState = rememberLazyListState()

                    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
                    val songPlaying by viewModel.currentSongPlaying.collectAsStateWithLifecycle()

                    val albums by viewModel.albums.collectAsStateWithLifecycle()
                    val allAlbumsGridState = rememberLazyGridState()

                    val personsWithSongCount by viewModel.personsWithSongCount.collectAsStateWithLifecycle()
                    val selectedPerson by viewModel.selectedPerson.collectAsStateWithLifecycle()
                    val allPersonsListState = rememberLazyListState()

                    val playlistsWithSongCount by viewModel.playlistsWithSongCount.collectAsStateWithLifecycle()
                    val smartPlaylistCounts by viewModel.smartPlaylistCounts.collectAsStateWithLifecycle()
                    val allPlaylistsListState = rememberLazyListState()

                    val genresWithSongCount by viewModel.genresWithSongCount.collectAsStateWithLifecycle()
                    val allGenresListState = rememberLazyListState()

                    val files by viewModel.filesInCurrentDestination.collectAsStateWithLifecycle()
                    val folderTrackCount = remember(songExtractor) {
                        { path: String -> songExtractor.countAudioTracksUnderFolderPath(path) }
                    }

                    val dataRetrieved by remember {
                        derivedStateOf {
                            songs != null && albums != null && personsWithSongCount != null
                        }
                    }

                    val swipeableState = rememberSwipeableState(initialValue = 0)
                    val homeCanvas = HomeLibraryTokens.canvasBackground(MaterialTheme.colorScheme)
                    var showSongsSortSheet by remember { mutableStateOf(false) }
                    var showFoldersSortSheet by remember { mutableStateOf(false) }
                    var showSongsSelect by remember { mutableStateOf(false) }
                    var addToPlaylistSongLocations by remember { mutableStateOf<List<String>?>(null) }
                    var folderLibraryTrackSheetSong by remember { mutableStateOf<Song?>(null) }
                    var fullPlayerOverlay by remember { mutableStateOf(FullPlayerOverlay.None) }
                    val songsSortOptions = remember {
                        Screens.Songs.getSortOptions()
                    }
                    val foldersSortOptions = remember {
                        Screens.Folders.getSortOptions()
                    }

                    LaunchedEffect(currentScreen) {
                        if (currentScreen != Screens.Songs) {
                            showSongsSortSheet = false
                            showSongsSelect = false
                        }
                        if (currentScreen != Screens.Folders) {
                            showFoldersSortSheet = false
                        }
                    }

                    val librarySectionSubtitle = when (currentScreen) {
                        Screens.Songs -> songs?.let { "${it.size} песен" } ?: ""
                        Screens.Albums -> albums?.let { "${it.size} альбомов" } ?: ""
                        Screens.Artists -> personsWithSongCount?.let { "${it.size} артистов" } ?: ""
                        Screens.Genres -> "${genresWithSongCount.size} жанров"
                        Screens.Playlists -> "${playlistsWithSongCount.size} плейлистов"
                        Screens.Folders -> {
                            val n = files.directories.size
                            if (n > 0) "$n пап." else "Папки"
                        }
                    }

                    LaunchedEffect(currentSong) {
                        if (currentSong == null) {
                            fullPlayerOverlay = FullPlayerOverlay.None
                        }
                        if (currentSong != null) return@LaunchedEffect
                        if (swipeableState.currentValue != 1) return@LaunchedEffect
                        swipeableState.animateTo(0)
                    }

                    val snackbarHostState = remember { SnackbarHostState() }

                    val message by viewModel.message.collectAsStateWithLifecycle()
                    LaunchedEffect(key1 = message){
                        if (message.isEmpty()) return@LaunchedEffect
                        snackbarHostState.showSnackbar(message)
                    }

                    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            viewModel.onDeleteConfirmedByUser()
                        } else {
                            viewModel.onDeleteConfirmationCancelled()
                        }
                    }
                    LaunchedEffect(Unit) {
                        viewModel.deleteConfirmationSender.collect { pendingIntent ->
                            deleteIntentSenderLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build(),
                            )
                        }
                    }

                    val readStoragePermissionState =
                        rememberPermissionState(
                            permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                        )

                    LaunchedEffect(key1 = readStoragePermissionState) {
                        if (readStoragePermissionState.status.isGranted) return@LaunchedEffect
                        val snackbarResult = snackbarHostState.showSnackbar(
                            context.getString(R.string.grant_access_to_read_storage),
                            context.getString(R.string.settings),
                            true,
                            SnackbarDuration.Indefinite
                        )
                        if (snackbarResult != SnackbarResult.ActionPerformed) return@LaunchedEffect
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).apply {
                            startActivity(this)
                        }
                    }

                    /** 1f = mini player strip fully visible; 0f = full player expanded. Matches [BottomSheet] peek alpha logic. */
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
                    val windowInsets = WindowInsets.systemBars.asPaddingValues()

                    val queue = viewModel.queue
                    val playbackParams by preferenceProvider.playbackParams.collectAsStateWithLifecycle()
                    val keepScreenOn by preferenceProvider.keepScreenOn.collectAsStateWithLifecycle()
                    val volumeBoosterPercent by preferenceProvider.volumeBoosterPercent.collectAsStateWithLifecycle()
                    val widgetStyle by preferenceProvider.widgetStyle.collectAsStateWithLifecycle()
                    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                    val isExplorerAtRoot by viewModel.isExplorerAtRoot.collectAsStateWithLifecycle()

                    BackHandler(
                        enabled = drawerState.currentValue == DrawerValue.Open ||
                            (currentScreen == Screens.Folders && !isExplorerAtRoot) ||
                            swipeableState.currentValue == 1,
                        onBack = {
                            if (drawerState.currentValue == DrawerValue.Open) {
                                scope.launch { drawerState.close() }
                            } else if (swipeableState.currentValue == 1) {
                                when (fullPlayerOverlay) {
                                    FullPlayerOverlay.Actions,
                                    FullPlayerOverlay.Queue,
                                    -> fullPlayerOverlay = FullPlayerOverlay.None
                                    FullPlayerOverlay.None -> scope.launch { swipeableState.animateTo(0) }
                                }
                            } else {
                                viewModel.moveToParent()
                            }
                        }
                    )
                    BackHandler(enabled = addToPlaylistSongLocations != null) {
                        addToPlaylistSongLocations = null
                    }

                    val songScreenSongClicked = remember {
                        { index: Int -> viewModel.setQueue(songs, index) }
                    }
                    val songScreenPlayAllClicked = remember { { viewModel.setQueue(songs) } }
                    val songScreenShuffleClicked = remember { { viewModel.shufflePlay(songs) } }
                    val miniPlayerPlayPauseClicked = remember { {
                        if (swipeableState.currentValue == 0) { viewModel.onMiniPlayerPlayPause() }
                    } }
                    val expandQueueBottomSheet: () -> Unit = remember {
                        { fullPlayerOverlay = FullPlayerOverlay.Queue }
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
                    val updateScreen = remember<(Screens) -> Unit> { {
                        if (currentScreen == it) {
                            scope.launch {
                                when (it) {
                                    Screens.Songs -> allSongsListState.scrollToItem(0)
                                    Screens.Albums -> allAlbumsGridState.scrollToItem(0)
                                    Screens.Artists -> allPersonsListState.scrollToItem(0)
                                    Screens.Playlists -> allPlaylistsListState.scrollToItem(0)
                                    Screens.Genres -> allGenresListState.scrollToItem(0)
                                    else -> {}
                                }
                            }
                        } else {
                            currentScreen = it
                        }
                    } }

                    /** Inset above system bar / mini player peek (bottom tab bar removed). */
                    val homeScreenBottomPadding by remember(currentSong) {
                        derivedStateOf {
                            if (currentSong == null) {
                                12.dp
                            } else {
                                12.dp + HomeLibraryTokens.miniPlayerPeekHeight
                            }
                        }
                    }

                    HomeNavigationDrawer(
                        drawerState = drawerState,
                        onItemClick = { item ->
                            scope.launch {
                                drawerState.close()
                                when (item) {
                                    DrawerMenuDestination.Library -> { }
                                    DrawerMenuDestination.Settings -> navHelper.navigateToSettings()
                                    DrawerMenuDestination.SleepTimer -> navHelper.navigateToSleepTimer()
                                    DrawerMenuDestination.VolumeBooster -> navHelper.navigateToVolumeBooster()
                                    DrawerMenuDestination.Equalizer -> navHelper.navigateToEqualizer()
                                    DrawerMenuDestination.Dictaphone -> navHelper.navigateToDictaphone()
                                    DrawerMenuDestination.GraphicTheme -> navHelper.navigateToTheme()
                                    DrawerMenuDestination.Widgets -> navHelper.navigateToWidgets()
                                }
                            }
                        },
                    ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LibraryShellBackdrop(
                            themePreference = themePreference,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Scaffold(
                            modifier = Modifier
                                .padding(bottom = homeScreenBottomPadding),
                            containerColor = homeCanvas,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            topBar = {
                                HomeTopBar(
                                    onMenuClicked = { scope.launch { drawerState.open() } },
                                    onThemeIconClicked = navHelper::navigateToTheme,
                                    onSettingsClicked = navHelper::navigateToSettings,
                                    onSearchClicked = navHelper::navigateToSearch,
                                    currentScreen = currentScreen,
                                    onSortOptionChosen = viewModel::saveSortOption,
                                    currentSortOrder = sortOrder,
                                    selectedTabs = selectedTabs,
                                    onScreenChange = updateScreen,
                                    sectionSubtitle = librarySectionSubtitle,
                                    onLayoutIconClicked = {
                                        if (currentScreen == Screens.Songs) {
                                            showSongsSelect = true
                                        }
                                    },
                                    onSongsSortSheetRequest = { showSongsSortSheet = true },
                                    onFoldersSortSheetRequest = { showFoldersSortSheet = true },
                                )
                            },
                            content = {
                                Box(
                                    modifier = Modifier
                                        .padding(
                                            top = it.calculateTopPadding(),
                                            start = windowInsets.calculateStartPadding(
                                                LayoutDirection.Ltr
                                            ),
                                            end = windowInsets.calculateEndPadding(LayoutDirection.Ltr)
                                        )
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (!dataRetrieved) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = currentScreen,
                                            transitionSpec = {
                                                (
                                                    fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                                                        slideInVertically { h -> h / 24 }
                                                    ) with (
                                                    fadeOut(tween(220)) +
                                                        slideOutVertically { h -> -h / 24 }
                                                    )
                                            },
                                            label = "homeTabContent",
                                        ) { targetScreen ->
                                            when (targetScreen) {
                                                Screens.Songs -> {
                                                    AllSongs(
                                                        songs = songs,
                                                        onSongClicked = songScreenSongClicked,
                                                        listState = allSongsListState,
                                                        onFavouriteClicked = viewModel::changeFavouriteValue,
                                                        currentSong = currentSong,
                                                        onAddToQueueClicked = viewModel::addToQueue,
                                                        onPlayAllClicked = songScreenPlayAllClicked,
                                                        onShuffleClicked = songScreenShuffleClicked,
                                                        onAddToPlaylistsClicked = navHelper::navigateToChoosePlaylist,
                                                        onPlayLibrarySongNext = viewModel::playLibrarySongNext,
                                                        onOpenAlbum = { song ->
                                                            navHelper.navigateToAlbumByName(song.album)
                                                        },
                                                        onPlayerActionEditTags = navHelper::navigateToTagEditor,
                                                        onPlayerActionHideSong = viewModel::onSongBlacklist,
                                                        onPlayerActionDeleteSong = viewModel::deleteSongFromDevice,
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
                                                Screens.Albums -> {
                                                    Albums(
                                                        albums = albums,
                                                        gridState = allAlbumsGridState,
                                                        onAlbumClicked = navHelper::navigateToViewDetails
                                                    )
                                                }
                                                Screens.Artists -> {
                                                    Persons(
                                                        personsWithSongCount = personsWithSongCount,
                                                        onPersonClicked = navHelper::navigateToViewDetails,
                                                        listState = allPersonsListState,
                                                        selectedPerson = selectedPerson,
                                                        onPersonSelect = viewModel::onPersonSelect
                                                    )
                                                }
                                                Screens.Playlists -> {
                                                    Playlists(
                                                        allSongs = songs,
                                                        playlistsWithSongCount = playlistsWithSongCount,
                                                        onPlaylistClicked = navHelper::navigateToViewDetails,
                                                        listState = allPlaylistsListState,
                                                        onPlaylistCreate = viewModel::onPlaylistCreate,
                                                        onDeletePlaylistClicked = viewModel::deletePlaylist,
                                                        onOpenSettingsForPlaylistTools = navHelper::navigateToSettings,
                                                        onSmartPlaylist = { action ->
                                                            when (action) {
                                                                HomeSmartPlaylistAction.Favourites ->
                                                                    navHelper.navigateToViewDetails()
                                                                HomeSmartPlaylistAction.RecentlyAdded ->
                                                                    navHelper.navigateToRecentlyAddedCollection()
                                                                HomeSmartPlaylistAction.RecentlyPlayed ->
                                                                    navHelper.navigateToRecentlyPlayedCollection()
                                                                HomeSmartPlaylistAction.TopTracks ->
                                                                    navHelper.navigateToTopTracksCollection()
                                                            }
                                                        },
                                                    )
                                                }
                                                Screens.Genres -> {
                                                    Genres(
                                                        genresWithSongCount = genresWithSongCount,
                                                        listState = allGenresListState,
                                                        onGenreClicked = navHelper::navigateToViewDetails
                                                    )
                                                }
                                                Screens.Folders -> {
                                                    val ctx = LocalContext.current
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        Files(
                                                            contents = files,
                                                            onDirectoryClicked = viewModel::onFileClicked,
                                                            onSongClicked = viewModel::onFileClicked,
                                                            currentSong = currentSong,
                                                            onAddToPlaylistClicked = navHelper::navigateToChoosePlaylist,
                                                            onAddToQueueClicked = viewModel::addToQueue,
                                                            onFolderPlay = viewModel::playAllInFolder,
                                                            onFolderPlayNext = viewModel::playFolderNext,
                                                            onFolderAddToQueue = viewModel::addFolderToQueue,
                                                            onFolderAddToPlaylist = { dir ->
                                                                scope.launch {
                                                                    val songs = viewModel.getSongsInFolderRecursive(dir)
                                                                    if (songs.isNotEmpty()) {
                                                                        navHelper.navigateToChoosePlaylist(songs)
                                                                    }
                                                                }
                                                            },
                                                            onFolderHide = viewModel::onFolderBlacklist,
                                                            onFolderDelete = viewModel::deleteFolderFromDevice,
                                                            onFolderRename = viewModel::renameFolder,
                                                            onFolderSaveNote = viewModel::saveFolderNote,
                                                            folderTrackCount = folderTrackCount,
                                                            onTrackOverflowClick = { mini ->
                                                                songExtractor.resolveSong(mini.location)?.let { resolved ->
                                                                    folderLibraryTrackSheetSong = resolved
                                                                }
                                                            },
                                                        )
                                                        folderLibraryTrackSheetSong?.let { sheetSong ->
                                                            HomeLibrarySongActionsBottomSheet(
                                                                song = sheetSong,
                                                                visible = true,
                                                                onDismiss = { folderLibraryTrackSheetSong = null },
                                                                onPlayNext = { viewModel.playLibrarySongNext(sheetSong) },
                                                                onAddToQueue = { viewModel.addToQueue(sheetSong) },
                                                                onAddToPlaylist = {
                                                                    navHelper.navigateToChoosePlaylist(sheetSong)
                                                                },
                                                                onOpenAlbum = {
                                                                    navHelper.navigateToAlbumByName(sheetSong.album)
                                                                },
                                                                onPlayerActionEditTags = navHelper::navigateToTagEditor,
                                                                onPlayerActionHideSong = viewModel::onSongBlacklist,
                                                                onPlayerActionDeleteSong = viewModel::deleteSongFromDevice,
                                                                onPlayerActionRingtone = { song ->
                                                                    if (AudioFileActions.setAsRingtone(ctx, song.location)) {
                                                                        Toast.makeText(
                                                                            ctx,
                                                                            ctx.getString(R.string.player_ringtone_ok),
                                                                            Toast.LENGTH_SHORT,
                                                                        ).show()
                                                                    } else {
                                                                        Toast.makeText(
                                                                            ctx,
                                                                            ctx.getString(R.string.player_ringtone_failed),
                                                                            Toast.LENGTH_LONG,
                                                                        ).show()
                                                                    }
                                                                },
                                                                onPlayerActionChangeCover = navHelper::navigateToTagEditor,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    snackbar = { Snackbar(it) }
                                )
                            }
                        )
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
                                            start = windowInsets.calculateStartPadding(LayoutDirection.Ltr),
                                            end = windowInsets.calculateEndPadding(LayoutDirection.Ltr),
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
                                                fullPlayerOverlay = FullPlayerOverlay.Actions
                                            },
                                            onThemeClicked = navHelper::navigateToTheme,
                                            onCollapseFullPlayer = collapseFullPlayer,
                                            onPausePlayPressed = viewModel::onMiniPlayerPlayPause,
                                            onPreviousPressed = pendingPreviousIntent::send,
                                            onNextPressed = pendingNextIntent::send,
                                            songPlaying = songPlaying,
                                            playerHelper = playerHelper,
                                            currentSongPlaying = songPlaying,
                                            onFavouriteClicked = viewModel::changeFavouriteValue,
                                            onQueueClicked = expandQueueBottomSheet,
                                            repeatMode = repeatMode,
                                            toggleRepeatMode = viewModel::toggleRepeatMode,
                                            playbackParams = playbackParams,
                                            updatePlaybackParams = preferenceProvider::updatePlaybackParams,
                                            onSleepTimerClicked = navHelper::navigateToSleepTimer,
                                            onSaveQueueClicked = { navHelper.navigateToChoosePlaylist(queue) },
                                            onShuffleClicked = { viewModel.shufflePlay(queue.toList()) },
                                            onEqualizerClicked = navHelper::navigateToEqualizer,
                                            onVolumeBoosterClicked = navHelper::navigateToVolumeBooster,
                                            onOpenAlbum = { navHelper.navigateToAlbumByName(playingSong.album) },
                                            onOpenArtist = { navHelper.navigateToArtistByName(playingSong.artist) },
                                            onOpenFolder = {
                                                java.io.File(playingSong.location).parentFile?.let { dir ->
                                                    viewModel.navigateToFolderInExplorer(dir.absolutePath)
                                                }
                                            },
                                            onAddCurrentSongToPlaylist = {
                                                addToPlaylistSongLocations = listOf(playingSong.location)
                                            },
                                            keepScreenOn = keepScreenOn,
                                            onKeepScreenOnChange = preferenceProvider::updateKeepScreenOn,
                                            volumeBoosterPercent = volumeBoosterPercent,
                                            onVolumeBoosterPercentChange = preferenceProvider::updateVolumeBoosterPercent,
                                            onSettingsClicked = navHelper::navigateToSettings,
                                            onPlayerActionEditTags = navHelper::navigateToTagEditor,
                                            onPlayerActionHideSong = { song ->
                                                viewModel.onSongBlacklist(song)
                                            },
                                            onPlayerActionDeleteSong = { song ->
                                                viewModel.deleteSongFromDevice(song)
                                            },
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
                            },
                            swipeableState = swipeableState,
                        )
                        SongsSortBottomSheet(
                            visible = showSongsSortSheet,
                            options = songsSortOptions,
                            selectedOrdinal = sortOrder[Screens.Songs.ordinal]
                                ?: songsSortOptions.first().ordinal,
                            onDismiss = { showSongsSortSheet = false },
                            onConfirm = { ordinal ->
                                viewModel.saveSortOption(Screens.Songs.ordinal, ordinal)
                            },
                        )
                        FoldersSortBottomSheet(
                            visible = showFoldersSortSheet,
                            options = foldersSortOptions,
                            selectedOrdinal = sortOrder[Screens.Folders.ordinal]
                                ?: foldersSortOptions.first().ordinal,
                            onDismiss = { showFoldersSortSheet = false },
                            onConfirm = { ordinal ->
                                viewModel.saveSortOption(Screens.Folders.ordinal, ordinal)
                            },
                        )
                        if (showSongsSelect) {
                            SongsSelectOverlay(
                                themePreference = themePreference,
                                songs = songs ?: emptyList(),
                                onDismiss = { showSongsSelect = false },
                                onPlaySelected = { list ->
                                    if (list.isNotEmpty()) {
                                        viewModel.setQueue(list, 0)
                                    }
                                },
                                onAddToPlaylist = { list ->
                                    if (list.isNotEmpty()) {
                                        navHelper.navigateToChoosePlaylist(list)
                                    }
                                },
                                onDeleteSelected = { list ->
                                    list.forEach { viewModel.deleteSongFromDevice(it) }
                                },
                            )
                        }
                        addToPlaylistSongLocations?.let { locations ->
                            AddToPlaylistFromPlayerSheet(
                                playlists = playlistsWithSongCount,
                                favouritesCount = smartPlaylistCounts.favourites,
                                onDismiss = { addToPlaylistSongLocations = null },
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addSongsToPlaylistFromPlayer(locations, playlistId)
                                },
                                onCreatePlaylist = { name ->
                                    viewModel.createPlaylistAndAddSongsFromPlayer(name, locations)
                                    addToPlaylistSongLocations = null
                                },
                                onFavouritesClick = {
                                    viewModel.addSongsToFavouritesFromPlayer(locations)
                                },
                            )
                        }
                        currentSong?.let { playingSong ->
                            QueueBottomSheetModal(
                                visible = fullPlayerOverlay == FullPlayerOverlay.Queue,
                                onDismiss = { fullPlayerOverlay = FullPlayerOverlay.None },
                                queue = queue,
                                onFavouriteClicked = viewModel::changeFavouriteValue,
                                currentSong = playingSong,
                                playerHelper = playerHelper,
                                onDrag = viewModel::onSongDrag,
                                onQueueSongPlayNext = viewModel::moveQueueSongToPlayNext,
                                onQueueSongAddToPlaylist = navHelper::navigateToChoosePlaylist,
                                onQueueSongRemoveFromQueue = viewModel::removeSongFromQueue,
                                onQueueSongOpenAlbum = { s ->
                                    navHelper.navigateToAlbumByName(s.album)
                                },
                                onQueueSongEditTags = navHelper::navigateToTagEditor,
                                onQueueSongHide = viewModel::onSongBlacklist,
                                onQueueSongDelete = viewModel::deleteSongFromDevice,
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
                                visible = fullPlayerOverlay == FullPlayerOverlay.Actions,
                                onDismiss = { fullPlayerOverlay = FullPlayerOverlay.None },
                                song = playingSong,
                                playbackParams = playbackParams,
                                updatePlaybackParams = preferenceProvider::updatePlaybackParams,
                                volumeBoosterPercent = volumeBoosterPercent,
                                onVolumeBoosterPercentChange = preferenceProvider::updateVolumeBoosterPercent,
                                keepScreenOn = keepScreenOn,
                                onKeepScreenOnChange = preferenceProvider::updateKeepScreenOn,
                                onSettingsClicked = {
                                    fullPlayerOverlay = FullPlayerOverlay.None
                                    navHelper.navigateToSettings()
                                },
                                onOpenAlbum = { navHelper.navigateToAlbumByName(playingSong.album) },
                                onPlayerActionEditTags = navHelper::navigateToTagEditor,
                                onPlayerActionHideSong = { song ->
                                    viewModel.onSongBlacklist(song)
                                },
                                onPlayerActionDeleteSong = { song ->
                                    viewModel.deleteSongFromDevice(song)
                                },
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
                }
            }
        }
    }
}