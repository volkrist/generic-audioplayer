package com.generic.audioplayes.playlisteditor

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.collection.CollectionType
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.home.HomeViewModel
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.generic.audioplayes.util.AudioFileActions
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistEditorFragment : Fragment() {

    private val viewModel: PlaylistEditorViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val text = viewModel.getExportM3uText() ?: return@launch
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(StandardCharsets.UTF_8))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val text = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: return@launch
                viewModel.importM3uContent(text)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val systemUiController = rememberSystemUiController()
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference, systemUiController) {
                    val context = LocalContext.current
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
                    PlaylistEditorScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onAddFromLibrary = {
                            navController.navigate(
                                PlaylistEditorFragmentDirections
                                    .actionPlaylistEditorFragmentToAddSongsToPlaylistFragment(
                                        viewModel.playlistId,
                                    ),
                            )
                        },
                        onExportM3u = { exportLauncher.launch("playlist.m3u") },
                        onImportM3u = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        onFavouriteClicked = homeViewModel::changeFavouriteValue,
                        onPlayLibrarySongNext = homeViewModel::playLibrarySongNext,
                        onAddToQueue = homeViewModel::addToQueue,
                        onAddToPlaylist = { song ->
                            navController.navigate(
                                PlaylistEditorFragmentDirections
                                    .actionPlaylistEditorFragmentToSelectPlaylistFragment(
                                        arrayOf(song.location),
                                    ),
                            )
                        },
                        onOpenAlbum = { song ->
                            if (song.album.isNotBlank() && song.album != "Unknown") {
                                navController.navigate(
                                    PlaylistEditorFragmentDirections
                                        .actionPlaylistEditorFragmentToCollectionFragment(
                                            CollectionType(
                                                CollectionType.AlbumType,
                                                song.album,
                                            ),
                                        ),
                                )
                            }
                        },
                        onPlayerActionEditTags = { song ->
                            navController.navigate(
                                PlaylistEditorFragmentDirections
                                    .actionPlaylistEditorFragmentToTagEditorFragment(song.location),
                            )
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
                        onPlayerActionChangeCover = { song ->
                            navController.navigate(
                                PlaylistEditorFragmentDirections
                                    .actionPlaylistEditorFragmentToTagEditorFragment(song.location),
                            )
                        },
                        onRemoveFromPlaylist = { song ->
                            viewModel.removeSong(song.location)
                        },
                    )
                }
            }
        }
    }
}
