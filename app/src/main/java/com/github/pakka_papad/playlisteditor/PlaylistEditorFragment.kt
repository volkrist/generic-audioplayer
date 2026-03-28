package com.github.pakka_papad.playlisteditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.home.HomeViewModel
import com.github.pakka_papad.ui.theme.ZenTheme
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
    lateinit var preferenceProvider: ZenPreferenceProvider

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
                ZenTheme(themePreference, systemUiController) {
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
                    )
                }
            }
        }
    }
}
