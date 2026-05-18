package com.generic.audioplayes.dictaphone

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DictaphoneFragment : Fragment() {

    private val viewModel: DictaphoneViewModel by viewModels()

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onPause() {
        super.onPause()
        viewModel.stopPlayback()
    }

    @OptIn(ExperimentalPermissionsApi::class)
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
                    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(HomeLibraryTokens.libraryShellGradient)
                    ) {
                        TopBarWithBackArrow(
                            onBackArrowPressed = { findNavController().navigateUp() },
                            title = stringResource(R.string.drawer_dictaphone),
                            actions = {},
                            backgroundColor = Color.Transparent,
                        )
                        DictaphoneScreen(
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(1f)
                                .navigationBarsPadding(),
                            microphoneGranted = recordAudioPermission.status.isGranted,
                            onRequestMicrophonePermission = {
                                recordAudioPermission.launchPermissionRequest()
                            },
                        )
                    }
                }
            }
        }
    }
}
