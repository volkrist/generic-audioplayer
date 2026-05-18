package com.generic.audioplayes.volume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VolumeBoosterFragment : Fragment() {

    private val viewModel: VolumeBoosterViewModel by viewModels()

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                AudioPlayerTheme(themePreference) {
                    // Same library‑shell gradient as Home / CollectionFragment / TagEditor, so
                    // the Volume Booster screen blends with the rest of the app instead of
                    // showing a stock grey surface.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(HomeLibraryTokens.libraryShellGradient)
                    ) {
                        TopBarWithBackArrow(
                            onBackArrowPressed = { findNavController().navigateUp() },
                            title = stringResource(R.string.drawer_volume_booster),
                            actions = {},
                            backgroundColor = Color.Transparent,
                        )
                        VolumeBoosterScreen(
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(top = 0.dp),
                        )
                    }
                }
            }
        }
    }
}
