package com.generic.audioplayes.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private val themePageBg = Brush.verticalGradient(
    0f to Color(0xFF070A18),
    0.5f to Color(0xFF101428),
    1f to Color(0xFF151030),
)

@AndroidEntryPoint
class ThemeFragment : Fragment() {

    private val viewModel: ThemeViewModel by viewModels()

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
                val pickLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent(),
                ) { uri ->
                    uri?.let {
                        val cur = viewModel.themePreference.value
                        viewModel.updateTheme(
                            cur.copy(
                                graphicWallpaperCustomUri = it.toString(),
                                graphicWallpaperPreset = 0,
                            ),
                        )
                    }
                }
                val themeTopBarBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1028),
                            Color(0xFF12102A),
                        ),
                    )
                }
                AudioPlayerTheme(themePreference) {
                    Column(Modifier.fillMaxSize()) {
                        TopBarWithBackArrow(
                            onBackArrowPressed = { findNavController().navigateUp() },
                            title = stringResource(R.string.drawer_graphic_theme),
                            actions = {},
                            onBackgroundColor = Color.White,
                            centerTitle = false,
                            showBottomDivider = false,
                            backgroundBrush = themeTopBarBrush,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(themePageBg),
                        ) {
                            ThemeScreen(
                                viewModel = viewModel,
                                onPickCustomWallpaper = { pickLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
