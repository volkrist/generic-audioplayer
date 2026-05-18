package com.generic.audioplayes.playlisteditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddSongsToPlaylistFragment : Fragment() {

    private val viewModel: AddSongsToPlaylistViewModel by viewModels()

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference) {
                    AddSongsToPlaylistScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onAdded = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
