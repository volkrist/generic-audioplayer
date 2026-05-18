package com.generic.audioplayes.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.collection.CollectionType
import com.generic.audioplayes.components.ScreenEmptyState
import com.generic.audioplayes.components.ScreenErrorState
import com.generic.audioplayes.components.ScreenLoadingState
import com.generic.audioplayes.components.Snackbar
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.Artist
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.home.HomeViewModel
import com.generic.audioplayes.ui.scaffoldContentPaddingWithSystemBars
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private lateinit var navController: NavController

    private val viewModel: SearchViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference) {
                    val query by viewModel.query.collectAsStateWithLifecycle()
                    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
                    val snackbarHostState = remember { SnackbarHostState() }

                    val message by viewModel.message.collectAsStateWithLifecycle()
                    LaunchedEffect(key1 = message) {
                        if (message.isEmpty()) return@LaunchedEffect
                        snackbarHostState.showSnackbar(message = message)
                    }

                    Scaffold(
                        topBar = {
                            SearchBar(
                                query = query,
                                onQueryChange = viewModel::updateQuery,
                                onBackArrowPressed = navController::popBackStack,
                                onClearRequest = viewModel::clearQueryText,
                            )
                        },
                        content = { paddingValues ->
                            val contentPadding = scaffoldContentPaddingWithSystemBars(paddingValues)
                            val errorMessage = searchResult.errorMsg
                            Box(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                when {
                                    searchResult.isLoading -> {
                                        ScreenLoadingState(paddingValues = contentPadding)
                                    }
                                    errorMessage != null -> {
                                        ScreenErrorState(
                                            message = errorMessage,
                                            paddingValues = contentPadding,
                                        )
                                    }
                                    query.isBlank() -> {
                                        ScreenEmptyState(
                                            message = stringResource(R.string.search_type_hint),
                                            paddingValues = contentPadding,
                                        )
                                    }
                                    else -> {
                                        ResultContent(
                                            contentPadding = contentPadding,
                                            searchResult = searchResult,
                                            onSongClicked = this@SearchFragment::handleSongClick,
                                            onAlbumClicked = this@SearchFragment::handleAlbumClick,
                                            onArtistClicked = this@SearchFragment::handleArtistClick,
                                            onFolderClicked = this@SearchFragment::handleFolderClick,
                                            onPlaylistClicked = this@SearchFragment::handlePlaylistClick,
                                            onDictaphoneRecordingClicked = this@SearchFragment::handleSongClick,
                                        )
                                    }
                                }
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = {
                                    Snackbar(it)
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    private fun handleSongClick(song: Song) {
        viewModel.playSongFromSearchResults(song)
    }

    private fun handleAlbumClick(album: Album) {
        if (navController.currentDestination?.id != R.id.searchFragment) return
        navController.navigate(
            SearchFragmentDirections.actionSearchFragmentToCollectionFragment(
                CollectionType(CollectionType.AlbumType, album.name),
            ),
        )
    }

    private fun handleArtistClick(artist: Artist) {
        if (navController.currentDestination?.id != R.id.searchFragment) return
        navController.navigate(
            SearchFragmentDirections.actionSearchFragmentToCollectionFragment(
                CollectionType(CollectionType.ArtistType, artist.name),
            ),
        )
    }

    private fun handleFolderClick(folder: FolderSearchResult) {
        homeViewModel.navigateToFolderInExplorer(folder.absolutePath)
        navController.popBackStack()
    }

    private fun handlePlaylistClick(playlist: Playlist) {
        if (navController.currentDestination?.id != R.id.searchFragment) return
        navController.navigate(
            SearchFragmentDirections.actionSearchFragmentToPlaylistEditorFragment(playlist.playlistId),
        )
    }
}
