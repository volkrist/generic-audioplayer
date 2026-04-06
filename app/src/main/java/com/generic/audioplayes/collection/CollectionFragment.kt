package com.generic.audioplayes.collection

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.generic.audioplayes.Constants
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.components.Snackbar
import com.generic.audioplayes.components.SortOptionChooser
import com.generic.audioplayes.components.SortOptions
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.home.HomeNavHelper
import com.generic.audioplayes.home.HomeViewModel
import com.generic.audioplayes.nowplaying.HomeLibrarySongActionsBottomSheet
import com.generic.audioplayes.nowplaying.PlayerHelper
import com.generic.audioplayes.player.AudioPlayerBroadcastReceiver
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.generic.audioplayes.util.AudioFileActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CollectionFragment : Fragment() {

    private val viewModel: CollectionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var navController: NavController

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private val args: CollectionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = findNavController()
        if (args.collectionType == null) {
            navController.popBackStack()
        }
        viewModel.loadCollection(args.collectionType)
        val sortOptions = listOf(
            SortOptions.Default,
            SortOptions.TitleASC,
            SortOptions.TitleDSC,
            SortOptions.YearASC,
            SortOptions.YearDSC,
            SortOptions.DurationASC,
            SortOptions.DurationDSC,
        )
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference) {
                    val playerHelper = remember(exoPlayer) { PlayerHelper(exoPlayer) }
                    val navHelper = remember(navController) {
                        HomeNavHelper(navController, lifecycle)
                    }
                    val pendingPreviousIntent = remember {
                        PendingIntent.getBroadcast(
                            requireContext(),
                            AudioPlayerBroadcastReceiver.PREVIOUS_ACTION_REQUEST_CODE,
                            Intent(Constants.PACKAGE_NAME).putExtra(
                                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_PREVIOUS,
                            ),
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                    }
                    val pendingNextIntent = remember {
                        PendingIntent.getBroadcast(
                            requireContext(),
                            AudioPlayerBroadcastReceiver.NEXT_ACTION_REQUEST_CODE,
                            Intent(Constants.PACKAGE_NAME).putExtra(
                                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_NEXT,
                            ),
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                    }
                    CollectionPlaybackShell(
                        homeViewModel = homeViewModel,
                        preferenceProvider = preferenceProvider,
                        playerHelper = playerHelper,
                        navHelper = navHelper,
                        pendingPreviousIntent = pendingPreviousIntent,
                        pendingNextIntent = pendingNextIntent,
                    ) { scaffoldBottomPadding ->
                    val context = LocalContext.current
                    val collectionUi by viewModel.collectionUi.collectAsStateWithLifecycle()
                    val songsListState = rememberLazyListState()
                    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
                    var trackSheetSong by remember { mutableStateOf<Song?>(null) }
                    val isPlaylistCollection =
                        args.collectionType?.type == CollectionType.PlaylistType

                    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            homeViewModel.onDeleteConfirmedByUser()
                        } else {
                            homeViewModel.onDeleteConfirmationCancelled()
                        }
                    }
                    LaunchedEffect(Unit) {
                        homeViewModel.deleteConfirmationSender.collect { pendingIntent ->
                            deleteIntentSenderLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build(),
                            )
                        }
                    }
                    val topBarContainerAlpha by remember {
                        derivedStateOf {
                            if (songsListState.firstVisibleItemIndex == 0
                                && songsListState.firstVisibleItemScrollOffset <= 10) 0f else 1f
                        }
                    }
                    var showSortOptions by remember { mutableStateOf(false) }
                    val chosenSortOrder by viewModel.chosenSortOrder.collectAsStateWithLifecycle()
                    val snackbarHostState = remember { SnackbarHostState() }

                    val message by viewModel.message.collectAsStateWithLifecycle()
                    LaunchedEffect(key1 = message){
                        if (message.isEmpty()) return@LaunchedEffect
                        snackbarHostState.showSnackbar(message)
                    }

                    Scaffold(
                        modifier = Modifier.padding(bottom = scaffoldBottomPadding),
                        topBar = {
                            CollectionTopBar(
                                topBarTitle = collectionUi?.topBarTitle ?: "",
                                alpha = topBarContainerAlpha,
                                onBackArrowPressed = navController::popBackStack,
                                actions = listOf(
                                    CollectionActions.AddToQueue {
                                        collectionUi?.songs?.let { viewModel.addToQueue(it) }
                                    },
                                    CollectionActions.AddToPlaylist {
                                        addAllSongsToPlaylistClicked(collectionUi?.songs)
                                    },
                                    CollectionActions.Sort {
                                        showSortOptions = true
                                    }
                                )
                            )
                        },
                        content = { paddingValues ->
                            val padding by remember {
                                derivedStateOf {
                                    PaddingValues(
                                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                                        bottom = paddingValues.calculateBottomPadding(),
                                    )
                                }
                            }
                            LazyColumn(
                                contentPadding = padding,
                                state = songsListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    CollectionImage(
                                        imageUri = collectionUi?.topBarBackgroundImageUri,
                                        title = collectionUi?.topBarTitle,
                                    )
                                }
                                if (collectionUi == null) {
                                    item {
                                        CircularProgressIndicator()
                                    }
                                } else if (collectionUi?.error != null) {
                                    item {
                                        FullScreenSadMessage(
                                            message = collectionUi?.error,
                                        )
                                    }
                                } else if (collectionUi?.songs?.isEmpty() == true) {
                                    item {
                                        FullScreenSadMessage(
                                            message = stringResource(R.string.no_songs_found),
                                        )
                                    }
                                } else {
                                    collectionContent(
                                        songs = collectionUi?.songs ?: emptyList(),
                                        onSongClicked = {
                                            viewModel.setQueue(collectionUi?.songs,it)
                                        },
                                        onSongFavouriteClicked = viewModel::changeFavouriteValue,
                                        currentSong = currentSong,
                                        onPlayAllClicked = {
                                            viewModel.setQueue(collectionUi?.songs,0)
                                        },
                                        onShuffleClicked = {
                                            viewModel.shufflePlay(collectionUi?.songs)
                                        },
                                        onTrackOverflowClick = { trackSheetSong = it },
                                    )
                                }
                            }
                            trackSheetSong?.let { sheetSong ->
                                HomeLibrarySongActionsBottomSheet(
                                    song = sheetSong,
                                    visible = true,
                                    onDismiss = { trackSheetSong = null },
                                    onPlayNext = { homeViewModel.playLibrarySongNext(sheetSong) },
                                    onAddToQueue = {
                                        viewModel.addToQueue(sheetSong)
                                    },
                                    onAddToPlaylist = {
                                        addToPlaylistClicked(sheetSong)
                                    },
                                    onOpenAlbum = {
                                        if (sheetSong.album.isNotBlank() && sheetSong.album != "Unknown") {
                                            navController.navigate(
                                                CollectionFragmentDirections
                                                    .actionCollectionFragmentToCollectionFragment(
                                                        CollectionType(
                                                            CollectionType.AlbumType,
                                                            sheetSong.album,
                                                        ),
                                                    ),
                                            )
                                        }
                                    },
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
                                    onPlayerActionChangeCover = navHelper::navigateToTagEditor,
                                    onRemoveFromPlaylist = if (isPlaylistCollection) {
                                        {
                                            viewModel.removeFromPlaylist(sheetSong)
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            if (showSortOptions){
                                SortOptionChooser(
                                    options = sortOptions,
                                    selectedOption = chosenSortOrder,
                                    onOptionSelect = { option ->
                                        viewModel.updateSortOrder(option)
                                        showSortOptions = false
                                    },
                                    onChooserDismiss = {
                                        showSortOptions = false
                                    }
                                )
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = {
                                    Snackbar(it)
                                }
                            )
                        }
                    )
                    }
                }
            }
        }
    }

    private fun addAllSongsToPlaylistClicked(songs: List<Song>?){
        lifecycleScope.launch {
            if (songs == null) return@launch
            if (navController.currentDestination?.id != R.id.collectionFragment) return@launch
            val songLocations = songs.map { it.location }
            navController.navigate(
                CollectionFragmentDirections
                    .actionCollectionFragmentToSelectPlaylistFragment(songLocations.toTypedArray())
            )
        }
    }

    private fun addToPlaylistClicked(song: Song){
        addAllSongsToPlaylistClicked(listOf(song))
    }
}